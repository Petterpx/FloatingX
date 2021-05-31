package com.petterp.floatingx.ext

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible

/**
 * @Author petterp
 * @Date 2021/5/20-5:17 下午
 * @Email ShiyihuiCloud@163.com
 * @Function
 */

internal inline fun <reified T : Any> lazyLoad(
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.NONE,
    crossinline obj: () -> T
): Lazy<T> =
    lazy(mode) {
        obj()
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

internal val Activity.rootView: FrameLayout?
    get() = try {
        window.decorView.findViewById(android.R.id.content)
    } catch (e: Exception) {
        e.printStackTrace()
        FxDebug.e("rootView -> Null")
        null
    }

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
