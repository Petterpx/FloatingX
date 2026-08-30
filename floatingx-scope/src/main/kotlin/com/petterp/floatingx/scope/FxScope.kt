package com.petterp.floatingx.scope

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfigScope
import com.petterp.floatingx.core.config.FxInstallScope

/** DSL：`FloatingX.create { viewGroupHost(parent); layout(...) }`，创建 host 并设置到 scope 上 */
@JvmSynthetic
public fun FxInstallScope.viewGroupHost(viewGroup: ViewGroup): ViewGroupHost =
    ViewGroupHost(viewGroup).also { host = it }

/**
 * 局部浮窗：挂在本 ViewGroup 内，不进注册表。
 * tag 只用于日志与位置持久化的存储键（空则不持久化，见 FloatingX.create）。
 * 不再需要时调用 control.cancel()。
 */
@JvmSynthetic
public fun ViewGroup.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl =
    FloatingX.create(tag) {
        host = ViewGroupHost(this@fxScope)
        block()
    }

/**
 * 局部浮窗：挂在 Activity 的 android.R.id.content 上。
 * API 29+ 在该 Activity destroy 时自动 cancel；API < 29 只能靠 window 卸下时 lost，
 * 建议调用方在 onDestroy 里 cancel（不 cancel 也不会泄漏：host 只被 Activity 自己的 view 树引用）。
 */
@JvmSynthetic
public fun Activity.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl {
    val control = findViewById<ViewGroup>(android.R.id.content).fxScope(tag, block)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        registerActivityLifecycleCallbacks(CancelOnDestroy(control))
    }
    return control
}

/** Activity 级生命周期回调（API 29+），destroy 时 cancel 并自我注销 */
private class CancelOnDestroy(private val control: FxControl) : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) activity.unregisterActivityLifecycleCallbacks(this)
        if (control.state != FxState.CANCELLED) control.cancel()
    }
}
