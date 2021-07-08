package com.petterp.floatingx.impl

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.petterp.floatingx.config.FxHelper
import com.petterp.floatingx.ext.FxDebug
import com.petterp.floatingx.ext.fxParentView
import com.petterp.floatingx.listener.IFxControl
import java.lang.ref.WeakReference

/**
 * @Author petterp
 * @Date 2021/5/20-4:07 下午
 * @Email ShiyihuiCloud@163.com
 * @Function App-lifecycle
 */
class FxLifecycleCallbackImpl(
    private val helper: FxHelper
) :
    Application.ActivityLifecycleCallbacks {
    internal var control: IFxControl? = null
    internal var topActivity: WeakReference<Activity>? = null

    private val Activity.isParent: Boolean
        get() = control?.getView()?.parent === fxParentView
    private val Activity.name: String
        get() = javaClass.name.split(".").last()
    private val Activity.isActivityInValid: Boolean
        get() = helper.blackList.contains(this::class.java)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityCreated")
        helper.fxLifecycleExpand?.onActivityCreated?.let {
            if (activity.isActivityInValid) it.invoke(activity, savedInstanceState)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        initActivity(activity)
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityStarted")
        helper.fxLifecycleExpand?.onActivityStarted?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        initActivity(activity)
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityResumed")
        val isActivityInValid = activity.isActivityInValid
        val isParent = activity.isParent
        FxDebug.d("view->isAttach? isContainActivity-$isActivityInValid--enableFx-${helper.enableFx}---isParent-$isParent")
        if (helper.enableFx && isActivityInValid && !isParent)
            control?.attach(activity)
        helper.fxLifecycleExpand?.onActivityResumed?.let {
            if (isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityPaused")
        helper.fxLifecycleExpand?.onActivityPaused?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityStopped")
        helper.fxLifecycleExpand?.onActivityStopped?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        helper.fxLifecycleExpand?.onActivityDestroyed?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
        val isParent = activity.isParent
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityDestroyed")
        FxDebug.d("view->isDetach? isContainActivity-${activity.isActivityInValid}--enableFx-${helper.enableFx}---isParent-$isParent")
        if (helper.enableFx && isParent)
            control?.detach(activity)
        if (topActivity?.get() === activity) {
            topActivity?.clear()
            topActivity = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        helper.fxLifecycleExpand?.onActivitySaveInstanceState?.let {
            if (activity.isActivityInValid) it.invoke(activity, outState)
        }
    }

    private fun initActivity(activity: Activity) {
        if (topActivity?.get() != activity)
            topActivity = WeakReference(activity)
    }
}
