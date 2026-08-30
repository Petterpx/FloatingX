package com.petterp.floatingx.core

import android.view.View
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxConfigScope
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.container.FxViewHolder
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxPoint

/**
 * 用户面向的控制接口（spec §2.7）。所有方法必须在主线程调用。
 * 未 attach 时 show/hide 记录意图、moveTo/moveBy 排队，host ready 后按序回放。
 */
public interface FxControl {
    public val tag: String
    public val state: FxState
    public val isShowing: Boolean

    /** 内容左上角的屏幕坐标，三种 host 语义一致（#200） */
    public val position: FxPoint
    public val anchor: FxAnchor
    public val config: FxConfig
    public val host: FxHost
    public val contentView: View?
    public val holder: FxViewHolder?

    public fun show()
    public fun hide()

    /** 销毁；之后任何调用抛 IllegalStateException */
    public fun cancel()

    public fun moveTo(x: Float, y: Float)
    public fun moveTo(x: Float, y: Float, animate: Boolean)
    public fun moveBy(dx: Float, dy: Float)
    public fun moveBy(dx: Float, dy: Float, animate: Boolean)

    /** 整体替换配置；Kotlin 用 `update { }` 扩展 */
    public fun update(config: FxConfig)

    /** 内容 view 在 install 时即创建，所以 show 之前也可用（#152/#89） */
    public fun updateContent(block: (FxViewHolder) -> Unit)
    public fun setContent(content: FxContent)

    public fun addListener(listener: FxListener)
    public fun removeListener(listener: FxListener)
    public fun addFeature(feature: FxFeature)
    public fun removeFeature(feature: FxFeature)
}

/** Kotlin DSL：在现有配置基础上局部修改 */
public inline fun FxControl.update(block: FxConfigScope.() -> Unit) {
    update(FxConfigScope(config).apply(block).build())
}
