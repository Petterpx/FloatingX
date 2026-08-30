package com.petterp.floatingx.compose

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * 浮窗自己的 Lifecycle / ViewModelStore / SavedStateRegistry owner（spec §6）。
 * 归 control 所有：容器 detach 只降到 CREATED，只有 control.cancel() 才 destroy——
 * 所以 ViewModel 与 rememberSaveable 的状态能跨页面、跨 host 存活（修 #210/#239）。
 * 所有方法必须在主线程调用（LifecycleRegistry 的要求）。
 */
public class FxComposeOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    public val isDestroyed: Boolean get() = registry.currentState == Lifecycle.State.DESTROYED

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    /** 在 CREATED / STARTED / RESUMED 之间移动；已 destroy 则忽略。要销毁请用 destroy() */
    public fun moveTo(state: Lifecycle.State) {
        if (isDestroyed) return
        require(state != Lifecycle.State.DESTROYED && state != Lifecycle.State.INITIALIZED) { "请用 destroy()；不支持 $state" }
        registry.currentState = state
    }

    /** 只在 control.cancel() 时调用：DESTROYED + 清空 ViewModel。幂等 */
    public fun destroy() {
        if (isDestroyed) return
        registry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    /** 把三个 owner 装到 view 上，Compose 向上查找 ViewTree owner 时命中 */
    public fun attachTo(view: View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    public companion object {
        /**
         * attachTo 的反操作：把 view 上的三个 ViewTree owner 摘掉。
         * 浮窗换成非 compose 内容时用——容器 view 会留给新内容，
         * 免得后来者捡到一个已经 destroy 的 owner（那比没有 owner 更糟：拿到它的 Recomposer 会直接被取消）。
         */
        @JvmStatic
        public fun detachFrom(view: View) {
            view.setViewTreeLifecycleOwner(null)
            view.setViewTreeViewModelStoreOwner(null)
            view.setViewTreeSavedStateRegistryOwner(null)
        }
    }
}
