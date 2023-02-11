package com.petterp.floatingx.impl.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.impl.control.FxAppControlImpl
import com.petterp.floatingx.listener.IFxProxyTagActivityLifecycle
import com.petterp.floatingx.util.FxLog
import com.petterp.floatingx.util.decorView
import com.petterp.floatingx.util.lazyLoad

/**
 * App-lifecycle 的代理实现，用于处理Fx自身逻辑
 *
 * @author petterp
 */
class FxProxyLifecycleCallBackImpl : Application.ActivityLifecycleCallbacks {

    private var helper: AppHelper? = null
    private var appControl: FxAppControlImpl? = null

    private val fxLog: FxLog?
        get() = helper?.fxLog

    private val enableFx: Boolean
        get() = helper?.enableFx ?: false

    private val appLifecycleCallBack: IFxProxyTagActivityLifecycle?
        get() = helper?.fxLifecycleExpand

    private val insertCls by lazyLoad {
        mutableMapOf<Class<*>, Boolean>()
    }

    private val Activity.name: String
        get() = javaClass.name.split(".").last()
    private val Activity.isParent: Boolean
        get() = appControl?.getManagerView()?.parent === decorView

    private val Activity.isActivityInValid: Boolean
        get() {
            val cls = this::class.java
            return insertCls[cls] ?: isInsertActivity(cls)
        }

    /** 初始化helper与app控制器 */
    fun init(helper: AppHelper, control: FxAppControlImpl) {
        this.helper = helper
        this.appControl = control
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (!enableFx) return
        appLifecycleCallBack?.let {
            if (activity.isActivityInValid) it.onCreated(activity, savedInstanceState)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (!enableFx) return
        helper?.fxLifecycleExpand?.onStarted(activity)
    }

    /** 最开始想到在onActivityPostStarted后插入, 但是最后发现在Android9及以下,此方法不会被调用,故选择了onResume */
    override fun onActivityResumed(activity: Activity) {
        if (!enableFx) return
        val activityName = activity.name
        fxLog?.d("fxApp->insert, insert [$activityName] Start ---------->")
        val isActivityInValid = activity.isActivityInValid
        if (isActivityInValid) appLifecycleCallBack?.onResumes(activity)
        else {
            fxLog?.d("fxApp->insert, insert [$activityName] Fail ,This activity is not in the list of allowed inserts.")
            return
        }
        val isParent = activity.isParent
        if (isParent) {
            fxLog?.d("fxApp->insert, insert [$activityName] Fail ,The current Activity has been inserted.")
            return
        }
        appControl?.let {
            it.attach(activity)
            fxLog?.d("fxApp->insert, insert [$activityName] Success--------------->")
        } ?: fxLog?.d("fxApp->insert, insert [$activityName] Fail ,appControl = null.")
    }

    override fun onActivityPaused(activity: Activity) {
        if (!enableFx) return
        appLifecycleCallBack?.let {
            if (activity.isActivityInValid) it.onPaused(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (!enableFx) return
        appLifecycleCallBack?.let {
            if (activity.isActivityInValid) it.onStopped(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (!enableFx) return
        appLifecycleCallBack?.let {
            if (activity.isActivityInValid) it.onDestroyed(activity)
        }
        val isParent = activity.isParent
        fxLog?.d("fxApp->detach? isContainActivity-${activity.isActivityInValid}--enableFx-$enableFx---isParent-$isParent")
        if (isParent) appControl?.detach(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        if (!enableFx) return
        appLifecycleCallBack?.let {
            if (activity.isActivityInValid) it.onSaveInstanceState(activity, outState)
        }
    }

    private fun isInsertActivity(cls: Class<*>): Boolean =
        helper?.let {
            // 条件 允许全局安装&&不在黑名单 || 禁止全局安装&&在白名单
            val isInsert = (it.isAllInstall && !it.blackFilterList.contains(cls)) ||
                (!it.isAllInstall && it.whiteInsertList.contains(cls))
            insertCls[cls] = isInsert
            return isInsert
        } ?: false
}
