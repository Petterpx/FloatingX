package com.petterp.floatingx.core.layout

public enum class FxHorizontal { START, CENTER, END }

public enum class FxVertical { TOP, CENTER, BOTTOM }

/** 锚点所依附的边/角。START/END 为逻辑方向，RTL 时由 ltr 参数解析 */
public enum class FxGravity(public val horizontal: FxHorizontal, public val vertical: FxVertical) {
    TOP_START(FxHorizontal.START, FxVertical.TOP),
    TOP_CENTER(FxHorizontal.CENTER, FxVertical.TOP),
    TOP_END(FxHorizontal.END, FxVertical.TOP),
    CENTER_START(FxHorizontal.START, FxVertical.CENTER),
    CENTER(FxHorizontal.CENTER, FxVertical.CENTER),
    CENTER_END(FxHorizontal.END, FxVertical.CENTER),
    BOTTOM_START(FxHorizontal.START, FxVertical.BOTTOM),
    BOTTOM_CENTER(FxHorizontal.CENTER, FxVertical.BOTTOM),
    BOTTOM_END(FxHorizontal.END, FxVertical.BOTTOM);

    public companion object {
        @JvmStatic
        public fun of(horizontal: FxHorizontal, vertical: FxVertical): FxGravity =
            entries.first { it.horizontal == horizontal && it.vertical == vertical }
    }
}
