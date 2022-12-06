package com.petterp.floatingx.assist

import android.view.Gravity
import com.petterp.floatingx.util.FX_GRAVITY_BOTTOM
import com.petterp.floatingx.util.FX_GRAVITY_CENTER
import com.petterp.floatingx.util.FX_GRAVITY_TOP

/** 对于位置而言,至少需要两个方向才能确定位置,如果单纯一个方向,那么另一个方向将无法确定,即x或y其中一个将无法计算 */
enum class FxGravity(val value: Int, val scope: Int) {
    DEFAULT(Gravity.START or Gravity.TOP, FX_GRAVITY_TOP),

    LEFT_OR_TOP(Gravity.START or Gravity.TOP, FX_GRAVITY_TOP),
    LEFT_OR_CENTER(Gravity.START or Gravity.CENTER, FX_GRAVITY_CENTER),
    LEFT_OR_BOTTOM(Gravity.START or Gravity.BOTTOM, FX_GRAVITY_BOTTOM),

    RIGHT_OR_TOP(Gravity.END or Gravity.TOP, FX_GRAVITY_TOP),
    RIGHT_OR_CENTER(Gravity.END or Gravity.CENTER, FX_GRAVITY_CENTER),
    RIGHT_OR_BOTTOM(Gravity.END or Gravity.BOTTOM, FX_GRAVITY_BOTTOM),

    CENTER(Gravity.CENTER, FX_GRAVITY_CENTER),
    TOP_OR_CENTER(Gravity.CENTER_HORIZONTAL or Gravity.TOP, FX_GRAVITY_TOP),
    BOTTOM_OR_CENTER(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, FX_GRAVITY_BOTTOM);

    /** 是否为默认位置 */
    fun isDefault() = this == DEFAULT
}
