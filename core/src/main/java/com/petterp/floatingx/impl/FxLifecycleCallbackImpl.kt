package com.petterp.floatingx.impl

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.petterp.floatingx.config.FxHelper
import com.petterp.floatingx.ext.FxDebug
import com.petterp.floatingx.ext.fxParentView
import com.petterp.floatingx.listener.IFxControl

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
    internal var topActivity: Activity? = null

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
        topActivity = activity
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityStarted")
        helper.fxLifecycleExpand?.onActivityStarted?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        val isActivityInValid = activity.isActivityInValid
        val isParent = activity.isParent
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityResumed")
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

    override fun onActivityPostDestroyed(activity: Activity) {
        if (topActivity === activity) topActivity = null
        val isParent = activity.isParent
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityPostDestroyed")
        FxDebug.d("view->isDetach? isContainActivity-${activity.isActivityInValid}--enableFx-${helper.enableFx}---isParent-$isParent")
        if (helper.enableFx && isParent)
            control?.detach(activity)
        helper.fxLifecycleExpand?.onActivityPostDestroyed?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        FxDebug.d("AppLifecycle--[${activity.name}]-onActivityDestroyed")
        helper.fxLifecycleExpand?.onActivityDestroyed?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPostPaused(activity: Activity) {
        helper.fxLifecycleExpand?.onActivityPostPaused?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPostStopped(activity: Activity) {
        helper.fxLifecycleExpand?.onActivityPostStopped?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPreDestroyed(activity: Activity) {
        helper.fxLifecycleExpand?.onActivityPreDestroyed?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        helper.fxLifecycleExpand?.onActivityPreCreated?.let {
            if (activity.isActivityInValid) it.invoke(activity, savedInstanceState)
        }
    }

    override fun onActivityPrePaused(activity: Activity) {
        super.onActivityPrePaused(activity)
        helper.fxLifecycleExpand?.onActivityPrePaused?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPreResumed(activity: Activity) {
        super.onActivityPreResumed(activity)
        helper.fxLifecycleExpand?.onActivityPreResumed?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPostSaveInstanceState(activity: Activity, outState: Bundle) {
        helper.fxLifecycleExpand?.onActivityPostSaveInstanceState?.let {
            if (activity.isActivityInValid) it.invoke(activity, outState)
        }
    }

    override fun onActivityPreSaveInstanceState(activity: Activity, outState: Bundle) {
        super.onActivityPreSaveInstanceState(activity, outState)
        helper.fxLifecycleExpand?.onActivityPreSaveInstanceState?.let {
            if (activity.isActivityInValid) it.invoke(activity, outState)
        }
    }

    override fun onActivityPreStopped(activity: Activity) {
        super.onActivityPreStopped(activity)
        helper.fxLifecycleExpand?.onActivityPreStopped?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPostResumed(activity: Activity) {
        helper.fxLifecycleExpand?.onActivityPostResumed?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPostStarted(activity: Activity) {
        helper.fxLifecycleExpand?.onActivityPostStarted?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPreStarted(activity: Activity) {
        helper.fxLifecycleExpand?.onActivityPreStarted?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        helper.fxLifecycleExpand?.onActivityPostCreated?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        helper.fxLifecycleExpand?.onActivitySaveInstanceState?.let {
            if (activity.isActivityInValid) it.invoke(activity, outState)
        }
    }
}
