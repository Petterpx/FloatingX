package com.petterp.floatingx.view

import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes

/**
 * Fx内部视图控制接口,便于开发者便捷的控制浮窗
 * @author petterp
 */
interface IFxInternalHelper {

    val childView: View?

    val containerView: FrameLayout

    val viewHolder: FxViewHolder?

    fun moveLocation(x: Float, y: Float, useAnimation: Boolean = true)

    fun moveLocationByVector(x: Float, y: Float, useAnimation: Boolean = true)

    fun checkPointerDownTouch(view: View, event: MotionEvent): Boolean

    fun checkPointerDownTouch(@IdRes id: Int, event: MotionEvent): Boolean

    fun moveToEdge()

    fun updateView(@LayoutRes layoutId: Int)

    fun updateView(layoutView: View)

    fun invokeClick()
}
