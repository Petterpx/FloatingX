package com.petterp.floatingx.compose

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope

/**
 * 把浮窗生命周期映射到 FxComposeOwner（spec §6）：
 * attach → STARTED，show → RESUMED，hide → STARTED，detach → CREATED，只有 cancel → DESTROYED。
 * 内容不是 FxComposeContent 时什么都不做。
 *
 * 由 `compose {}` 自动注册。手动 `addFeature(ComposeOwnerFeature(content))` 时把内容传进来，
 * 「host 一直没 ready（从未 attach）就 cancel」这种情况才有办法销毁 owner。
 */
public class ComposeOwnerFeature @JvmOverloads constructor(content: FxComposeContent? = null) : FxFeature {

    private var scope: FxFeatureScope? = null

    /** 当前绑定的 compose 内容；detach 不清空（owner 跨 attach 周期存活），只有 cancel / 换内容才清 */
    private var bound: FxComposeContent? = null

    /**
     * 最近一次见到的 compose 内容。onAttach 从未来过（host 始终没 ready）时 bound 一直是空，
     * cancel 只能靠它拿到 owner 去 destroy。
     */
    private var lastContent: FxComposeContent? = content

    /** 兜底装到宿主根 view 上的 owner，见 attachRootFallback；只在 attach 期间持有 */
    private var rootFallbackView: View? = null
    private var rootFallbackOwner: FxComposeOwner? = null

    override fun onAttach(scope: FxFeatureScope) {
        this.scope = scope
        val next = scope.config.content as? FxComposeContent
        // 对账：detach 期间换过内容的话 core 不会回调 onConfigChanged（featuresAttached=false），
        // bound 还停在旧内容上，先把它销毁再绑新的
        val stale = bound
        if (stale != null && stale !== next) {
            destroy(stale)
            if (next == null) clearContainerOwner(scope, stale.owner)
        }
        lastContent = next
        if (next != null) bind(next, scope)
    }

    private fun bind(content: FxComposeContent, scope: FxFeatureScope) {
        bound = content
        // 系统窗口的根 view 就是容器：Compose 的窗口级 Recomposer 从根 view 找 owner
        content.owner.attachTo(scope.container.view)
        attachRootFallback(content.owner, scope.container.view)
        scope.container.contentView?.let { content.bind(scope.control, it) }
        content.owner.moveTo(if (scope.control.isShowing) Lifecycle.State.RESUMED else Lifecycle.State.STARTED)
    }

    override fun onShow() {
        bound?.owner?.moveTo(Lifecycle.State.RESUMED)
    }

    override fun onHide() {
        bound?.owner?.moveTo(Lifecycle.State.STARTED)
    }

    override fun onDetach() {
        bound?.owner?.moveTo(Lifecycle.State.CREATED)
        // 兜底 owner 只在挂载期间有意义：留在宿主根 view 上，浮窗一 stop 就会把整个窗口的 Recomposer 拖住
        releaseRootFallback()
        scope = null
    }

    override fun onCancel() {
        (bound ?: lastContent)?.let { destroy(it) }
        lastContent = null
        releaseRootFallback()
    }

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        if (old.content === new.content) return
        val next = new.content as? FxComposeContent
        val stale = bound ?: (old.content as? FxComposeContent)
        if (stale != null && stale !== next) {
            destroy(stale)
            // 换成非 compose 内容：容器上的 ViewTree owner 也要摘掉，别让新内容捡到一个已销毁的 owner
            if (next == null) clearContainerOwner(scope, stale.owner)
        }
        lastContent = next
        val s = scope ?: return
        if (next != null) bind(next, s)
    }

    /** compose {} 复用已有实例时同步内容，见 FxComposeExt.compose */
    internal fun declare(content: FxComposeContent) {
        lastContent = content
    }

    private fun destroy(content: FxComposeContent) {
        content.owner.destroy()
        content.release()
        if (bound === content) bound = null
        if (rootFallbackOwner === content.owner) releaseRootFallback()
    }

    /** 只摘自己装上去的那个 owner，宿主自带的不动 */
    private fun clearContainerOwner(scope: FxFeatureScope?, owner: FxComposeOwner) {
        val view = scope?.container?.view ?: return
        if (view.findViewTreeLifecycleOwner() === owner) FxComposeOwner.detachFrom(view)
    }

    /**
     * Layer 容器挂在宿主的 decorView 下，而窗口级 Recomposer 是从**根 view** 上找 LifecycleOwner 的：
     * 裸 android.app.Activity 的 decor 上没有 owner（androidx 的 ComponentActivity 才装），
     * 这时补上浮窗自己的，否则 ComposeView 一 attach 就抛 "no ViewTreeLifecycleOwner"。
     * 宿主已经有 owner（正常情况）就不覆盖。
     */
    private fun attachRootFallback(owner: FxComposeOwner, container: View) {
        releaseRootFallback()
        val root = container.rootView
        if (root === container || root.findViewTreeLifecycleOwner() != null) return
        owner.attachTo(root)
        rootFallbackView = root
        rootFallbackOwner = owner
    }

    private fun releaseRootFallback() {
        val root = rootFallbackView ?: return
        val owner = rootFallbackOwner
        rootFallbackView = null
        rootFallbackOwner = null
        if (root.findViewTreeLifecycleOwner() === owner) FxComposeOwner.detachFrom(root)
    }
}
