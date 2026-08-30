package com.petterp.floatingx.compose

import androidx.lifecycle.Lifecycle
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope

/**
 * 把浮窗生命周期映射到 FxComposeOwner（spec §6）：
 * attach → STARTED，show → RESUMED，hide → STARTED，detach → CREATED，只有 cancel → DESTROYED。
 * 内容不是 FxComposeContent 时什么都不做。
 */
public class ComposeOwnerFeature : FxFeature {

    private var scope: FxFeatureScope? = null

    /** 当前绑定的 compose 内容；detach 不清空（owner 跨 attach 周期存活），只有 cancel / 换内容才清 */
    private var bound: FxComposeContent? = null

    override fun onAttach(scope: FxFeatureScope) {
        this.scope = scope
        (scope.config.content as? FxComposeContent)?.let { bind(it, scope) }
    }

    private fun bind(content: FxComposeContent, scope: FxFeatureScope) {
        bound = content
        // 系统窗口的根 view 就是容器：Compose 的窗口级 Recomposer 从根 view 找 owner
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
        bound?.owner?.destroy()
        bound?.release()
        bound = null
    }

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        if (old.content === new.content) return
        (old.content as? FxComposeContent)?.let { it.owner.destroy(); it.release() }
        bound = null
        val s = scope ?: return
        (new.content as? FxComposeContent)?.let { bind(it, s) }
    }
}
