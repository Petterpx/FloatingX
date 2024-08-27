package com.petterp.floatingx.imp.system

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.listener.IFxProxyTagActivityLifecycle
import com.petterp.floatingx.util.FxLog

/**
 * App-lifecycle 的代理实现，用于处理Fx-System逻辑
 *
 * @author petterp
 */
class FxSystemLifecycleImp(
    private val helper: FxAppHelper,
    private val provider: FxSystemPlatformProvider?
) : Application.ActivityLifecycleCallbacks {
    private var isNeedAskPermission = true
    private val insertCls = mutableMapOf<Class<*>, Boolean>()

    private val fxLog: FxLog
        get() = helper.fxLog

    private val enableFx: Boolean
        get() = helper.enableFx

    private val appLifecycleCallBack: IFxProxyTagActivityLifecycle?
        get() = helper.fxLifecycleExpand

    private val Activity.name: String
        get() = javaClass.name.split(".").last()

    private val Activity.isActivityInValid: Boolean
        get() = helper.isCanInstall(this)

    init {
        isNeedAskPermission = helper.enableFx
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (!enableFx) return
        if (appLifecycleCallBack != null && activity.isActivityInValid) {
            appLifecycleCallBack?.onCreated(activity, savedInstanceState)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (!enableFx) return
        helper.fxLifecycleExpand?.onStarted(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        if (!enableFx) return
        val activityName = activity.name
        fxLog.v("fxApp->insert, insert [$activityName] Start ---------->")
        val isActivityInValid = activity.isActivityInValid
        if (isActivityInValid) {
            provider?.safeShowOrHide(true)
            checkAskPermissionAndShow(activity)
            appLifecycleCallBack?.onResumes(activity)
        } else {
            provider?.safeShowOrHide(false)
            fxLog.v("fxApp->insert, insert [$activityName] Fail ,This activity is not in the list of allowed inserts.")
            return
        }
    }

    private fun checkAskPermissionAndShow(activity: Activity) {
        if (isNeedAskPermission) {
            isNeedAskPermission = false
            provider?.internalAskAutoPermission(activity)
            return
        }
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

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

}
