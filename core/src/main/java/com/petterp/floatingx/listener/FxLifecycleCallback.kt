package com.petterp.floatingx.listener

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.petterp.floatingx.config.FxHelper
import com.petterp.floatingx.ext.FxDebug
import com.petterp.floatingx.ext.fxParentView

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
    internal var topActivity: Activity? = null

    private val Activity.isParent: Boolean
        get() = control?.getView()?.parent === fxParentView
    private val Activity.name: String
        get() = javaClass.name.split(".").last()
    private val Activity.isActivityInValid: Boolean
        get() = helper.blackList.contains(this::class.java)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityCreated")
//        if (helper.enableFx && helper.enableAttachDialogF && activity is FragmentActivity) {
//            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
//                FragmentLifecycleCallBacksImpl(),
//                true
//            )
//        }
    }

    override fun onActivityStarted(activity: Activity) {
        topActivity = activity
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityStarted")
    }

    override fun onActivityResumed(activity: Activity) {
        val isActivityInValid = activity.isActivityInValid
        val isParent = activity.isParent
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityResumed")
        FxDebug.d("view->isAttach? isContainActivity-$isActivityInValid--enableFx-${helper.enableFx}---isParent-$isParent")
        if (helper.enableFx && isActivityInValid && !isParent)
            control?.attach(activity)
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityStopped")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityDestroyed")
    }

    override fun onActivityPreDestroyed(activity: Activity) {
        if (topActivity == activity) topActivity = null
        val isParent = activity.isParent
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityPreDestroyed")
        FxDebug.d("view->isDetach? isContainActivity-${activity.isActivityInValid}--enableFx-${helper.enableFx}---isParent-$isParent")
        if (helper.enableFx && isParent)
            control?.detach(activity)
    }
}
