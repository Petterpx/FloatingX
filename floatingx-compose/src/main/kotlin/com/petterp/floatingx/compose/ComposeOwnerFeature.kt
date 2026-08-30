package com.petterp.floatingx.compose

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope

/**
 * 把浮窗生命周期映射到 FxComposeOwner（spec §6）：
 * attach → STARTED，show → RESUMED，hide → STARTED，detach → CREATED，只有 cancel（或被 removeFeature
 * 摘掉）→ DESTROYED。内容不是 FxComposeContent 时什么都不做。
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

    /**
     * 最近一次绑定过的容器 view。onDetach 之后 scope 就没了，但 onRemove / onCancel 仍要把
     * 装在容器上的 owner 摘掉——不能给活着的容器留一个已 destroy 的 owner。
     * 每次 bind 都会指向当前容器，所以不会攥住换掉的旧容器。
     */
    private var containerView: View? = null

    override fun onAttach(scope: FxFeatureScope) {
        this.scope = scope
        // detach 期间换内容由 onConfigChanged 当场对账（core 无条件广播），这里只管绑当前这份
        val next = scope.config.content as? FxComposeContent
        lastContent = next
        if (next != null) bind(next, scope)
    }

    private fun bind(content: FxComposeContent, scope: FxFeatureScope) {
        bound = content
        containerView = scope.container.view
        // 系统窗口的根 view 就是容器：容器上也挂一份 owner，向上查找 ViewTree owner 的调用方都能命中
        content.owner.attachTo(scope.container.view)
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
        scope = null
    }

    override fun onCancel() {
        destroyBound()
    }

    /** 被 removeFeature / update 摘掉：之后收不到 onCancel 了，在这里把 owner 释放掉 */
    override fun onRemove() {
        destroyBound()
    }

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        if (old.content === new.content) return
        val next = new.content as? FxComposeContent
        val stale = bound ?: (old.content as? FxComposeContent)
        if (stale != null && stale !== next) {
            // 换成非 compose 内容：容器上的 ViewTree owner 也要摘掉，别让新内容捡到一个已销毁的 owner
            if (next == null) clearContainerOwner(stale)
            destroy(stale)
        }
        lastContent = next
        // 未挂载时只记住内容，绑定推迟到下一次 onAttach
        val s = scope ?: return
        if (next != null) bind(next, s)
    }

    /** compose {} 复用已有实例时同步内容，见 FxComposeExt.compose */
    internal fun declare(content: FxComposeContent) {
        lastContent = content
    }

    private fun destroyBound() {
        (bound ?: lastContent)?.let {
            clearContainerOwner(it)
            destroy(it)
        }
        lastContent = null
        containerView = null
    }

    private fun destroy(content: FxComposeContent) {
        content.owner.destroy()
        content.release()
        if (bound === content) bound = null
    }

    /** 只摘自己装上去的那个 owner，宿主自带的不动 */
    private fun clearContainerOwner(content: FxComposeContent) {
        val view = scope?.container?.view ?: containerView ?: return
        if (view.findViewTreeLifecycleOwner() === content.owner) FxComposeOwner.detachFrom(view)
    }
}
