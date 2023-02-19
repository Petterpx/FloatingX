package com.petterp.floatingx.impl.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.impl.control.FxAppControlImpl
import java.lang.ref.WeakReference

/** App-lifecycle-CallBack */
class FxLifecycleCallbackImpl : Application.ActivityLifecycleCallbacks {

    private val fxList: Collection<FxAppControlImpl>
        get() = FloatingX.getFxList().values

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (isFxsNotAllow()) return
        for (fx in fxList) {
            fx.onActivityCreated(activity, savedInstanceState)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (isFxsNotAllow()) return
        for (fx in fxList) {
            fx.onActivityStarted(activity)
        }
    }

    /** 最开始想到在onActivityPostStarted后插入, 但是最后发现在Android9及以下,此方法不会被调用,故选择了onResume */
    override fun onActivityResumed(activity: Activity) {
        updateTopActivity(activity)
        if (isFxsNotAllow()) return
        for (fx in fxList) {
            fx.onActivityResumed(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (isFxsNotAllow()) return
        for (fx in fxList) {
            fx.onActivityPaused(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (isFxsNotAllow()) return
        for (fx in fxList) {
            fx.onActivityStopped(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (isFxsNotAllow()) return
        for (fx in fxList) {
            fx.onActivityDestroyed(activity)
        }
        if (topActivity?.get() === activity) {
            topActivity?.clear()
            topActivity = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        if (isFxsNotAllow()) return
        for (fx in fxList) {
            fx.onActivitySaveInstanceState(activity, outState)
        }
    }

    private fun isFxsNotAllow(): Boolean {
        return fxList.isEmpty()
    }

    private fun updateTopActivity(activity: Activity) {
        if (topActivity?.get() != activity) topActivity = WeakReference(activity)
    }

    companion object {
        internal var topActivity: WeakReference<Activity>? = null

        @JvmSynthetic
        fun getTopActivity(): Activity? = topActivity?.get()

        @JvmSynthetic
        internal fun updateTopActivity(activity: Activity?) {
            if (activity == null) return
            topActivity = WeakReference(activity)
        }

        @JvmSynthetic
        internal fun releaseTopActivity() {
            topActivity?.clear()
            topActivity = null
        }
    }
}
