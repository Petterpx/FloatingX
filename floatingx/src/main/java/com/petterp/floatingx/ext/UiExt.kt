package com.petterp.floatingx.ext

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible

/**
 * @Author petterp
 * @Date 2021/6/2-7:12 下午
 * @Email ShiyihuiCloud@163.com
 * @Function Fx的一些UI扩展
 */

internal fun ViewGroup.updateParams(left: Int, top: Int, end: Int, bottom: Int) {
    val parent = (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
        leftMargin = left
        topMargin = top
        marginEnd = end
        bottomMargin = bottom
    }
    layoutParams = parent
}

internal fun View.show() {
    if (isVisible) return
    else isVisible = true
}

internal fun View.hide() {
    if (!isVisible) return
    else isVisible = false
}
