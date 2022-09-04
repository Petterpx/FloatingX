package com.petterp.floatingx.util

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.impl.lifecycle.FxLifecycleCallbackImpl

/** App级当前设置了tag的栈顶Activity */
internal val topActivity: Activity?
    get() = FxLifecycleCallbackImpl.getTopActivity()

internal val Activity.decorView: FrameLayout?
    get() = try {
        window.decorView as FrameLayout
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

internal val Activity.contentView: FrameLayout?
    get() = try {
        window.decorView.findViewById(android.R.id.content)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

/** 判断视图是否被覆盖 */
internal val View.isViewCovered: Boolean
    get() {
        var currentView = this
        val currentViewRect = Rect()
        val partVisible = currentView.getGlobalVisibleRect(currentViewRect)
        val totalHeightVisible: Boolean =
            currentViewRect.bottom - currentViewRect.top >= this.measuredHeight
        val totalWidthVisible: Boolean =
            currentViewRect.right - currentViewRect.left >= this.measuredWidth
        val totalViewVisible = partVisible && totalHeightVisible && totalWidthVisible
        // if any part of the view is clipped by any of its parents,return true
        if (!totalViewVisible) return true
        while (currentView.parent is ViewGroup) {
            val currentParent = currentView.parent as ViewGroup
            // if the parent of view is not visible,return true
            if (currentParent.visibility != View.VISIBLE) return true
            val start = currentView.indexOfViewInParent(currentParent)
            for (i in start + 1 until currentParent.childCount) {
                val viewRect = Rect()
                this.getGlobalVisibleRect(viewRect)
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

/** Fx的一些UI扩展 */
@JvmSynthetic
internal fun ViewGroup.updateParams(left: Int, top: Int, end: Int, bottom: Int) {
    val parent = (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
        leftMargin = left
        topMargin = top
        marginEnd = end
        bottomMargin = bottom
    }
    layoutParams = parent
}

@JvmSynthetic
internal fun View.indexOfViewInParent(parent: ViewGroup): Int {
    var index = 0
    while (index < parent.childCount) {
        if (parent.getChildAt(index) === this) break
        index++
    }
    return index
}
