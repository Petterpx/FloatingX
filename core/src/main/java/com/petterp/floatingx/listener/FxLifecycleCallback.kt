package com.petterp.floatingx.listener

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.petterp.floatingx.config.FxHelper
import com.petterp.floatingx.ext.FxDebug
import com.petterp.floatingx.ext.rootView

/**
 * @Author petterp
 * @Date 2021/5/20-4:07 下午
 * @Email ShiyihuiCloud@163.com
 * @Function App-lifecycle
 */
class FxLifecycleCallback(
    private val helper: FxHelper
) :
    Application.ActivityLifecycleCallbacks {
    internal var control: IFxControl? = null
    var topActivity: Activity? = null
    private fun isActivityInValid(activity: Activity) =
        helper.blackList.contains(activity::class.java)

    private fun Activity.isParent() = control?.getView()?.parent === rootView
    private fun Activity.name() = javaClass.name.split(".").last()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        FxDebug.d("AppLifecycle-onActivityCreated")
    }

    override fun onActivityStarted(activity: Activity) {
        val isActivityInValid = isActivityInValid(activity)
        val isParent = activity.isParent()
        FxDebug.d(
            "AppLifecycle--[${activity.name()}]-onActivityStarted-isContainAct-$isActivityInValid--isEnable-${helper.isEnable}---isAttach-$isParent"
        )
        if (helper.isEnable && isActivityInValid && !isParent)
            control?.attach(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        FxDebug.d("AppLifecycle-onActivityResumed")
        topActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        FxDebug.d("AppLifecycle-onActivityStopped")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (topActivity == activity) topActivity = null
        val isParent = activity.isParent()
        FxDebug.d(
            "AppLifecycle--[${activity.name()}]-onActivityDestroyed--isEnable-${helper.isEnable}---isAttach-$isParent"
        )
        if (helper.isEnable && isParent)
            control?.detach(activity)
    }
}
