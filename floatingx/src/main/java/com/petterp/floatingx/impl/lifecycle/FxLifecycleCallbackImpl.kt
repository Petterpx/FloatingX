package com.petterp.floatingx.impl.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.impl.control.FxAppControlImpl
import com.petterp.floatingx.listener.IFxProxyTagActivityLifecycle
import com.petterp.floatingx.util.FxLog
import com.petterp.floatingx.util.decorView
import java.lang.ref.WeakReference

/**
 * App-lifecycle-CallBack
 */
class FxLifecycleCallbackImpl :
    Application.ActivityLifecycleCallbacks {

    private val fxLog: FxLog?
        get() = helper?.fxLog
    private val helper: AppHelper?
        get() = FloatingX.helper
    private val enableFx: Boolean
        get() = helper?.enableFx ?: false
    private val appControl: FxAppControlImpl?
        get() = FloatingX.fxControl
    private val appLifecycleCallBack: IFxProxyTagActivityLifecycle?
        get() = helper?.fxLifecycleExpand

    private val Activity.name: String
        get() = javaClass.name.split(".").last()
    private val Activity.isParent: Boolean
        get() = appControl?.getManagerView()?.parent === decorView

    private val insertCls by lazy {
        mutableMapOf<Class<*>, Boolean>()
    }
    private val Activity.isActivityInValid: Boolean
        get() {
            val cls = this::class.java
            return insertCls[cls] ?: isInsertActivity(cls)
        }

    private fun isInsertActivity(cls: Class<*>): Boolean =
        helper?.let {
            val isInsert =
                (it.enableAllBlackClass && !(it.filterList?.contains(cls) ?: false)) ||
                    it.blackList?.contains(cls) ?: false
            insertCls[cls] = isInsert
            return isInsert
        } ?: false

    private fun initTopActivity(activity: Activity) {
        if (topActivity?.get() != activity)
            topActivity = WeakReference(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (!enableFx) return
        fxLog?.d("AppLifecycle--[${activity.name}]-onActivityCreated")
        appLifecycleCallBack?.let {
            if (activity.isActivityInValid) it.onCreated(activity, savedInstanceState)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (!enableFx) return
        helper?.fxLog?.d("AppLifecycle--[${activity.name}]-onActivityStarted")
        helper?.fxLifecycleExpand?.onStarted(activity)
    }

    /**
     * 最开始想到在onActivityPostStarted后插入,
     * 但是最后发现在Android9及以下,此方法不会被调用,故选择了onResume
     * */
    override fun onActivityResumed(activity: Activity) {
        initTopActivity(activity)
        if (!enableFx) return
        val activityName = activity.name
        fxLog?.d("AppLifecycle--[$activityName]-onActivityResumed")
        fxLog?.d("fxApp->insert, insert [$activityName] Start ---------->")
        val isActivityInValid = activity.isActivityInValid
        if (isActivityInValid) appLifecycleCallBack?.onResumes(activity)
        else {
            fxLog?.d("fxApp->insert, insert [$activityName] Fail ,This activity is not in the list of allowed inserts.")
            fxLog?.d("fxApp->insert, insert [$activityName] End ----------->")
            return
        }
        val isParent = activity.isParent
        if (isParent) {
            fxLog?.d("fxApp->insert, insert [$activityName] Fail ,The current Activity has been inserted.")
            fxLog?.d("fxApp->insert, insert [$activityName] End ----------->")
            return
        }
        appControl?.let {
            it.attach(activity)
            fxLog?.d("fxApp->insert, insert [$activityName] Success--------------->")
        } ?: fxLog?.d("fxApp->insert, insert [$activityName] Fail ,appControl = null.")
        fxLog?.d("fxApp->insert, insert [$activityName] End ----------->")
    }

    override fun onActivityPaused(activity: Activity) {
        if (!enableFx) return
        fxLog?.d("AppLifecycle--[${activity.name}]-onActivityPaused")
        appLifecycleCallBack?.let {
            if (activity.isActivityInValid) it.onPaused(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (!enableFx) return
        fxLog?.d("AppLifecycle--[${activity.name}]-onActivityStopped")
        appLifecycleCallBack?.let {
            if (activity.isActivityInValid) it.onStopped(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (!enableFx) return
        fxLog?.d("AppLifecycle--[${activity.name}]-onActivityDestroyed")
        appLifecycleCallBack?.let {
            if (activity.isActivityInValid) it.onDestroyed(activity)
        }
        val isParent = activity.isParent
        fxLog?.d("fxApp->detach? isContainActivity-${activity.isActivityInValid}--enableFx-$enableFx---isParent-$isParent")
        if (isParent) appControl?.detach(activity)
        if (topActivity?.get() === activity) {
            topActivity?.clear()
            topActivity = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        if (!enableFx) return
        fxLog?.d("AppLifecycle--[${activity.name}]-onActivityDestroyed")
        appLifecycleCallBack?.let {
            if (activity.isActivityInValid) it.onSaveInstanceState(activity, outState)
        }
    }

    companion object {
        internal var topActivity: WeakReference<Activity>? = null
    }
}
