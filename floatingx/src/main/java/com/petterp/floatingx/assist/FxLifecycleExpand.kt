package com.petterp.floatingx.assist

import android.app.Activity
import android.os.Bundle

/**
 * Fx-lifecycle扩展
 *
 * PS: 只有显示悬浮窗的Activity的才会被回调相应生命周期
 * */
open class FxLifecycleExpand {
    internal var onActivityCreated: ((Activity, Bundle?) -> Unit)? = null
    internal var onActivityStarted: ((Activity) -> Unit)? = null
    internal var onActivityResumed: ((Activity) -> Unit)? = null
    internal var onActivityPaused: ((Activity) -> Unit)? = null
    internal var onActivityStopped: ((Activity) -> Unit)? = null
    internal var onActivitySaveInstanceState: ((Activity, Bundle) -> Unit?)? = null
    internal var onActivityDestroyed: ((Activity) -> Unit)? = null

    open fun onCreated(obj: (Activity, Bundle?) -> Unit) {
        this.onActivityCreated = obj
    }

    open fun onStarted(obj: (Activity) -> Unit) {
        this.onActivityStarted = obj
    }

    open fun onResumes(obj: (Activity) -> Unit) {
        this.onActivityResumed = obj
    }

    open fun onPaused(obj: (Activity) -> Unit) {
        this.onActivityPaused = obj
    }

    open fun onStopped(obj: (Activity) -> Unit) {
        this.onActivityStopped = obj
    }

    open fun onSaveInstanceState(obj: (Activity, Bundle?) -> Unit) {
        this.onActivitySaveInstanceState = obj
    }

    open fun onDestroyed(obj: (Activity) -> Unit) {
        this.onActivityDestroyed = obj
    }
}
