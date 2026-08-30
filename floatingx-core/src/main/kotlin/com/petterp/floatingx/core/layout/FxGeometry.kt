package com.petterp.floatingx.core.layout

/*
 * 纯 Kotlin 几何类型。刻意不用 android.graphics.*，
 * 使 FxLayoutResolver / FxAdsorbResolver 可以在纯 JVM 里测试。
 */

public data class FxPoint(val x: Float, val y: Float) {
    public companion object {
        @JvmField public val ZERO: FxPoint = FxPoint(0f, 0f)
    }
}

public data class FxSize(val width: Float, val height: Float) {
    /** 宽高都大于 0 才可用于定位；内容尚未测量时为 false */
    public val isValid: Boolean get() = width > 0f && height > 0f

    public companion object {
        @JvmField public val EMPTY: FxSize = FxSize(0f, 0f)
    }
}

public data class FxRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    public val width: Float get() = right - left
    public val height: Float get() = bottom - top
    public val centerX: Float get() = (left + right) / 2f
    public val centerY: Float get() = (top + bottom) / 2f

    public fun inset(l: Float, t: Float, r: Float, b: Float): FxRect = FxRect(left + l, top + t, right - r, bottom - b)
    public fun inset(insets: FxInsets): FxRect = inset(insets.left, insets.top, insets.right, insets.bottom)
}

/** safe area（状态栏 / 导航栏 / 刘海）四边 */
public data class FxInsets @JvmOverloads constructor(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f) {
    public companion object {
        @JvmField public val NONE: FxInsets = FxInsets()
    }
}

/** 用户配置的四边留白 */
public data class FxMargin @JvmOverloads constructor(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f) {
    public companion object {
        @JvmField public val NONE: FxMargin = FxMargin()

        @JvmStatic public fun all(value: Float): FxMargin = FxMargin(value, value, value, value)
    }
}

/** 允许内容超出可用区的哪些边（#235） */
public data class FxOverflow @JvmOverloads constructor(val top: Boolean = false, val bottom: Boolean = false, val left: Boolean = false, val right: Boolean = false) {
    public companion object {
        @JvmField public val NONE: FxOverflow = FxOverflow()

        @JvmField public val ALL: FxOverflow = FxOverflow(top = true, bottom = true, left = true, right = true)
    }
}

/** host 提供的父区域：rect 为容器整体，insets 为其中的 safe area */
public data class FxBounds @JvmOverloads constructor(val rect: FxRect, val insets: FxInsets = FxInsets.NONE)
