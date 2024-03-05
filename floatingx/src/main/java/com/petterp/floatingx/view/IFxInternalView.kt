package com.petterp.floatingx.view

import android.view.View
import android.widget.FrameLayout

/**
 * Fx内部视图控制接口
 * @author petterp
 */
internal interface IFxInternalView {

    val childView: View?

    val containerView: FrameLayout

    val viewHolder: FxViewHolder?

    fun moveLocation(x: Float, y: Float, useAnimation: Boolean = true)

    fun moveLocationByVector(x: Float, y: Float, useAnimation: Boolean = true)

    fun moveToEdge()

    fun updateView()
}
