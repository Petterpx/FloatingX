package com.petterp.floatingx.ext

import android.app.Activity
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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

internal fun <T : View> T.delayView(obj: (T) -> Unit) {
    viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.removeOnPreDrawListener(this)
            }
            obj(this@delayView)
            return true
        }
    })
}

class UiExt private constructor() {
    companion object {
        var statsBarHeight: Int = 0

        var navigationBarHeight: Int = 0

        fun isViewCovered(view: View): Boolean {
            var currentView = view
            val currentViewRect = Rect()
            val partVisible = currentView.getGlobalVisibleRect(currentViewRect)
            val totalHeightVisible: Boolean =
                currentViewRect.bottom - currentViewRect.top >= view.measuredHeight
            val totalWidthVisible: Boolean =
                currentViewRect.right - currentViewRect.left >= view.measuredWidth
            val totalViewVisible = partVisible && totalHeightVisible && totalWidthVisible
            // if any part of the view is clipped by any of its parents,return true
            if (!totalViewVisible) return true
            while (currentView.parent is ViewGroup) {
                val currentParent = currentView.parent as ViewGroup
                // if the parent of view is not visible,return true
                if (currentParent.visibility != View.VISIBLE) return true
                val start = indexOfViewInParent(currentView, currentParent)
                for (i in start + 1 until currentParent.childCount) {
                    val viewRect = Rect()
                    view.getGlobalVisibleRect(viewRect)
                    val otherView = currentParent.getChildAt(i)
                    val otherViewRect = Rect()
                    otherView.getGlobalVisibleRect(otherViewRect)
                    // if view intersects its older brother(covered),return true
                    if (Rect.intersects(viewRect, otherViewRect)) return true
                }
                currentView = currentParent
            }
            return false
        }

        private fun indexOfViewInParent(view: View, parent: ViewGroup): Int {
            var index = 0
            while (index < parent.childCount) {
                if (parent.getChildAt(index) === view) break
                index++
            }
            return index
        }
    }
}
