package com.petterp.floatingx.core.gesture

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes

/** 拖动触发方式 */
public enum class FxDrag { IMMEDIATE, AFTER_LONG_PRESS, DISABLED }

/** 与内容里可滚动子 view 的冲突策略：AUTO = 落点下有可滚动子 view 时不抢；PARENT = 超过 slop 就抢；CHILD = 永不抢 */
public enum class FxChildPriority { AUTO, PARENT, CHILD }

/** 允许起拖的区域，x/y 为相对内容 view 左上角的坐标 */
public fun interface FxRegion {
    public fun contains(x: Float, y: Float, content: View): Boolean

    public companion object {
        /** 只有按在某个子 view 上才能拖（#165） */
        @JvmStatic
        public fun child(@IdRes id: Int): FxRegion = FxRegion { x, y, content ->
            val child = content.findViewById<View>(id) ?: return@FxRegion false
            if (child === content) return@FxRegion true
            val group = content as? ViewGroup ?: return@FxRegion false
            val rect = Rect(0, 0, child.width, child.height)
            group.offsetDescendantRectToMyCoords(child, rect)
            rect.contains(x.toInt(), y.toInt())
        }

        @JvmStatic
        public fun rect(left: Float, top: Float, right: Float, bottom: Float): FxRegion =
            FxRegion { x, y, _ -> x >= left && x <= right && y >= top && y <= bottom }
    }
}

/**
 * 可组合的手势配置（spec §2.4），替代 2.x 的 FxDisplayMode 枚举。
 * @param longPressTimeout 0 表示使用系统 ViewConfiguration.getLongPressTimeout()
 */
public data class FxGesture @JvmOverloads constructor(
    val click: Boolean = true,
    val longPress: Boolean = true,
    val drag: FxDrag = FxDrag.IMMEDIATE,
    val dragRegion: FxRegion? = null,
    val childPriority: FxChildPriority = FxChildPriority.AUTO,
    val touchable: Boolean = true,
    val longPressTimeout: Long = 0L,
) {
    public companion object {
        @JvmField public val Normal: FxGesture = FxGesture()

        @JvmField public val ClickOnly: FxGesture = FxGesture(drag = FxDrag.DISABLED)

        /** 完全透传，内容只展示（#243/#108） */
        @JvmField public val DisplayOnly: FxGesture = FxGesture(click = false, longPress = false, drag = FxDrag.DISABLED, touchable = false)

        /** 长按后才可拖动（#222） */
        @JvmField public val LongPressToDrag: FxGesture = FxGesture(drag = FxDrag.AFTER_LONG_PRESS)
    }
}
