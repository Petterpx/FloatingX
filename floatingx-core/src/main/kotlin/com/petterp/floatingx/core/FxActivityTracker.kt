package com.petterp.floatingx.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 前台 Activity 跟踪（spec §2.7）。不再用 ContentProvider 自动初始化：
 * 需要它的 host（app/system）在构造时调用 init(application)。
 * onActivityDestroyed 一定清引用，避免 2.x 的 topActivity 指向已销毁 Activity。
 */
public object FxActivityTracker {

    public interface Observer {
        public fun onActivityResumed(activity: Activity) {}

        /** API 29+ 才会回调；低版本 host 自行在 onActivityResumed 后 post */
        public fun onActivityPostResumed(activity: Activity) {}
        public fun onActivityPaused(activity: Activity) {}
        public fun onActivityDestroyed(activity: Activity) {}
    }

    private var app: Application? = null
    private var top: WeakReference<Activity>? = null
    private val observers = CopyOnWriteArrayList<Observer>()

    public val topActivity: Activity?
        get() = top?.get()

    @JvmStatic
    public fun init(application: Application) {
        if (app === application) return
        app?.unregisterActivityLifecycleCallbacks(callbacks)
        app = application
        application.registerActivityLifecycleCallbacks(callbacks)
    }

    @JvmStatic
    public fun addObserver(observer: Observer) {
        observers.addIfAbsent(observer)
    }

    @JvmStatic
    public fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) {
            top = WeakReference(activity)
            observers.forEach { it.onActivityResumed(activity) }
        }

        override fun onActivityPostResumed(activity: Activity) {
            observers.forEach { it.onActivityPostResumed(activity) }
        }

        override fun onActivityPaused(activity: Activity) {
            observers.forEach { it.onActivityPaused(activity) }
        }

        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) {
            if (top?.get() === activity) top = null
            observers.forEach { it.onActivityDestroyed(activity) }
        }
    }
}
