package com.petterp.floatingx.config

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

/**
 * @Author petterp
 * @Date 2021/7/11-5:53 下午
 * @Email ShiyihuiCloud@163.com
 * @Function 系统配置的保存
 */
class SystemConfig private constructor() {
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
