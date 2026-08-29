package com.petterp.floatingx.core.host

import com.petterp.floatingx.core.layout.FxGravity

/**
 * 一次布局提交：内容左上角坐标 + 当前锚点的 gravity + 布局方向。
 * Layer 容器只用 x/y；Window 容器可把 gravity 映射到 LayoutParams.gravity（spec §2.3）。
 */
public data class FxLayoutSpec(val x: Float, val y: Float, val gravity: FxGravity, val ltr: Boolean)
