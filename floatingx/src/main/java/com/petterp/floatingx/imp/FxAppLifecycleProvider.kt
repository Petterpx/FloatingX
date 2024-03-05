package com.petterp.floatingx.imp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Fx基础Provider提供者
 * @author petterp
 */
class FxAppLifecycleProvider : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        updateTopActivity(activity)
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
        updateTopActivity(activity)
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    companion object {
        private var _currentActivity: WeakReference<Activity>? = null

        @JvmSynthetic
        fun getTopActivity(): Activity? = _currentActivity?.get()

        @JvmSynthetic
        internal fun updateTopActivity(activity: Activity?) {
            if (activity == null || _currentActivity?.get() === activity) return
            _currentActivity = WeakReference(activity)
        }
    }
}
