package com.petterp.floatingx.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 前台 Activity 跟踪（spec §2.7）。core 自身不做任何自动初始化（不带 ContentProvider）：
 * - 进程启动即初始化由 app 模块的 `FxAppInitProvider` 负责（它在清单里声明，onCreate 时调 [init]）；
 * - 各 host（app/system）在 `bind()` 里仍会再调一次 [init] 兜底，[init] 幂等，重复调用无副作用。
 *
 * 注意 [init] 只能注册后续回调，无法补种「注册之前就已经 resume」的 Activity：
 * 那种情况下 [topActivity] 为 null，直到下一次 Activity resume 才有值。
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
