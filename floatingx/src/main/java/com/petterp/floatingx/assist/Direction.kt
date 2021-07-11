package com.petterp.floatingx.assist

import android.view.Gravity

/**
 * @Author petterp
 * @Date 2021/5/25-3:27 下午
 * @Email ShiyihuiCloud@163.com
 * @Function 悬浮窗的位置方向
 */
enum class Direction(val value: Int) {
    LEFT_OR_TOP(Gravity.START or Gravity.TOP),
    LEFT_OR_CENTER(Gravity.START or Gravity.CENTER),
    LEFT_OR_BOTTOM(Gravity.START or Gravity.BOTTOM),
    RIGHT_OR_TOP(Gravity.END or Gravity.TOP),
    RIGHT_OR_CENTER(Gravity.END or Gravity.CENTER),
    RIGHT_OR_BOTTOM(Gravity.END or Gravity.BOTTOM)
}
