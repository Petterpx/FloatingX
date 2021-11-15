package com.petterp.floatingx.impl.lifecycle

import android.app.Activity
import android.os.Bundle
import com.petterp.floatingx.listener.IFxProxyTagActivityLifecycle

/** IFxProxyTagActivityLifecycle的空实现 */
open class FxTagActivityLifecycleImpl : IFxProxyTagActivityLifecycle {
    override fun onCreated(activity: Activity, bundle: Bundle?) {}

    override fun onStarted(activity: Activity) {}

    override fun onResumes(activity: Activity) {}

    override fun onPaused(activity: Activity) {}

    override fun onStopped(activity: Activity) {}

    override fun onSaveInstanceState(activity: Activity, bundle: Bundle?) {}

    override fun onDestroyed(activity: Activity) {}
}
