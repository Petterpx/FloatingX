package com.petterp.floatingx.assist

import android.view.Gravity

/**
 * 对于位置而言,至少需要两个方向才能确定位置,如果单纯一个方向,那么另一个方向将无法确定,即x或y其中一个将无法计算
 * */
enum class Direction(val value: Int) {
    DEFAULT(Gravity.START or Gravity.TOP),
    LEFT_OR_TOP(Gravity.START or Gravity.TOP),
    LEFT_OR_CENTER(Gravity.START or Gravity.CENTER),
    LEFT_OR_BOTTOM(Gravity.START or Gravity.BOTTOM),
    RIGHT_OR_TOP(Gravity.END or Gravity.TOP),
    RIGHT_OR_CENTER(Gravity.END or Gravity.CENTER),
    RIGHT_OR_BOTTOM(Gravity.END or Gravity.BOTTOM)
}
