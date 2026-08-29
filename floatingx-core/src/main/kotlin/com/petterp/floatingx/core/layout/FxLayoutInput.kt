package com.petterp.floatingx.core.layout

/** 一次定位需要的全部输入 */
public data class FxLayoutInput(
    val bounds: FxBounds,
    val size: FxSize,
    val ltr: Boolean = true,
    val margin: FxMargin = FxMargin.NONE,
    val overflow: FxOverflow = FxOverflow.NONE,
    val safeArea: Boolean = true,
) {
    /** 可用区：bounds 扣掉 insets（safeArea 时）再扣 margin */
    public val area: FxRect = (if (safeArea) bounds.rect.inset(bounds.insets) else bounds.rect)
        .inset(margin.left, margin.top, margin.right, margin.bottom)
}
