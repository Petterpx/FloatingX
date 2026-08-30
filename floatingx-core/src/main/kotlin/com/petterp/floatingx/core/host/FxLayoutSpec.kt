package com.petterp.floatingx.core.host

import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity

/**
 * 一次布局提交：内容左上角坐标 + 当前锚点 + 布局方向。
 * Layer 容器只用 x/y；Window 容器可把 gravity 映射到 LayoutParams.gravity（spec §2.3）。
 * 带完整 anchor 而不只是 gravity：Window 容器还需要 dx/dy 才能还原 LayoutParams 的偏移。
 */
public data class FxLayoutSpec(val x: Float, val y: Float, val anchor: FxAnchor, val ltr: Boolean) {
    public val gravity: FxGravity get() = anchor.gravity
}
