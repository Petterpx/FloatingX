package com.petterp.floatingx.util

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.petterp.floatingx.view.FxMagnetView

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

internal fun FxMagnetView.show(isAnimation: Boolean) {
    isVisible = true
    if (isAnimation && helper.enableAnimation &&
        helper.fxAnimation != null && !helper.fxAnimation!!.fromJobRunning
    ) {
        if (helper.fxAnimation?.fromJobRunning == true) {
            FxDebug.d("view->Animation ,startAnimation Executing, cancel this operation!")
            return
        }
        FxDebug.d("view->Animation ,startAnimation Executing, cancel this operation.")
        helper.fxAnimation?.fromStartAnimator(this)
    }
}
