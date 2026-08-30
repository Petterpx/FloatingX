package com.petterp.floatingx.scope

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfigScope
import com.petterp.floatingx.core.config.FxInstallScope

/** DSL：`FloatingX.create { fragmentHost(fragment); layout(...) }` */
@JvmSynthetic
public fun FxInstallScope.fragmentHost(fragment: Fragment): FragmentHost =
    FragmentHost(fragment).also { host = it }

/**
 * 局部浮窗：挂在 Fragment 的根 view 上，view 创建后才显示（#244），fragment destroy 时自动 cancel。
 * 须在 onCreate 或之后调用（需要 fragment.context）。
 */
@JvmSynthetic
public fun Fragment.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl {
    // DESTROYED 的 fragment 上 viewLifecycleOwnerLiveData 再也不会有新值，浮窗永远 ready 不了，早点报错
    check(lifecycle.currentState != Lifecycle.State.DESTROYED) { "Fragment 已 destroy，不能再创建浮窗" }
    val control = FloatingX.create(tag) {
        host = FragmentHost(this@fxScope)
        block()
    }
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            owner.lifecycle.removeObserver(this)
            if (control.state != FxState.CANCELLED) control.cancel()
        }
    })
    return control
}
