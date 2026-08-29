package com.petterp.floatingx.core.layout

/**
 * 位置的唯一真值：依附的边/角 + 从该边向内的偏移。
 * START → x = area.left + dx；END → x = area.right - w - dx；CENTER → x = centerX - w/2 + dx。
 */
public data class FxAnchor(val gravity: FxGravity, val dx: Float = 0f, val dy: Float = 0f) {
    public companion object {
        @JvmField public val DEFAULT: FxAnchor = FxAnchor(FxGravity.TOP_START)
    }
}
