package com.petterp.floatingx.system.container

import android.view.Gravity
import android.view.WindowManager
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxHorizontal
import com.petterp.floatingx.core.layout.FxVertical
import kotlin.math.roundToInt

/**
 * 把 core 的"内容左上角屏幕坐标 + 锚点 gravity"换算成 WindowManager.LayoutParams 的 gravity + 偏移（spec §2.3 / #187）。
 * 让 WindowManager 自己保持锚定边：内容尺寸变化时窗口从锚定边生长，不会先跳到旧坐标再被修正。
 * 屏幕尺寸或内容尺寸未知（0）时退化为 TOP|LEFT + 直接坐标。
 */
internal object WindowLayoutMath {

    fun apply(
        lp: WindowManager.LayoutParams,
        x: Float,
        y: Float,
        gravity: FxGravity,
        ltr: Boolean,
        boundsW: Int,
        boundsH: Int,
        contentW: Int,
        contentH: Int,
    ) {
        if (boundsW <= 0 || boundsH <= 0 || contentW <= 0 || contentH <= 0) {
            lp.gravity = Gravity.TOP or Gravity.LEFT
            lp.x = x.roundToInt()
            lp.y = y.roundToInt()
            return
        }
        val h = when (gravity.horizontal) {
            FxHorizontal.START -> if (ltr) Gravity.LEFT else Gravity.RIGHT
            FxHorizontal.END -> if (ltr) Gravity.RIGHT else Gravity.LEFT
            FxHorizontal.CENTER -> Gravity.CENTER_HORIZONTAL
        }
        val v = when (gravity.vertical) {
            FxVertical.TOP -> Gravity.TOP
            FxVertical.BOTTOM -> Gravity.BOTTOM
            FxVertical.CENTER -> Gravity.CENTER_VERTICAL
        }
        lp.gravity = h or v
        // 四舍五入而不是截断：截断会把亚像素的偏移一路吞掉，连续换算后位置会向左上漂移
        lp.x = when (h) {
            Gravity.LEFT -> x
            Gravity.RIGHT -> boundsW - x - contentW
            else -> x - (boundsW - contentW) / 2f
        }.roundToInt()
        lp.y = when (v) {
            Gravity.TOP -> y
            Gravity.BOTTOM -> boundsH - y - contentH
            else -> y - (boundsH - contentH) / 2f
        }.roundToInt()
    }
}
