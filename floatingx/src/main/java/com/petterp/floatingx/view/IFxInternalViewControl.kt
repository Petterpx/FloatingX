package com.petterp.floatingx.view

import android.view.View
import android.widget.FrameLayout

/**
 *
 * @author petterp
 */
internal interface IFxInternalViewControl {

    val containerView: FrameLayout

    val childView: View?

    fun moveLocation(x: Float, y: Float, useAnimation: Boolean = true)

    fun moveLocationByVector(x: Float, y: Float, useAnimation: Boolean = true)

    fun moveToEdge()

    fun getX(): Float

    fun getY(): Float

    fun restoreLocation(x: Float, y: Float)
}
