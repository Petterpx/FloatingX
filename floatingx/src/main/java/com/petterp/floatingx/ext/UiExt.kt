package com.petterp.floatingx.ext

import android.app.Activity
import android.graphics.Point
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible

/**
 * @Author petterp
 * @Date 2021/6/2-7:12 下午
 * @Email ShiyihuiCloud@163.com
 * @Function Fx的一些UI扩展
 */

val Activity.navigationBarHeight: Int
    get() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            windowManager.defaultDisplay
        }
        val size = Point()
        val realSize = Point()
        display?.getSize(size)
        display?.getRealSize(realSize)
        val resourceId =
            resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val height = resources.getDimensionPixelSize(resourceId)
        // 超出系统默认的导航栏高度以上，则认为存在虚拟导航
        return if (realSize.y - size.y > height - 10) {
            height
        } else 0
    }

val Activity.statusBarHeight: Int
    get() {
        var height = 0
        val resourceId: Int =
            resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            height = resources
                .getDimensionPixelSize(resourceId)
        }
        return height
    }

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

// internal fun <T : View> T.delayView(obj: (T) -> Unit) {
//    viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
//        override fun onPreDraw(): Boolean {
//            if (viewTreeObserver.isAlive) {
//                viewTreeObserver.removeOnPreDrawListener(this)
//            }
//            obj(this@delayView)
//            return true
//        }
//    })
// }
