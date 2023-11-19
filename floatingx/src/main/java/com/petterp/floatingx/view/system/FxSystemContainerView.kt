package com.petterp.floatingx.view.system

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.view.IFxInternalView

/** 基础悬浮窗View */
@SuppressLint("ViewConstructor")
class FxSystemContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs), IFxInternalView {

    private lateinit var helper: FxBasisHelper
    override val containerView: FrameLayout
        get() = this
    override val childView: View
        get() = TODO("Not yet implemented")

    override fun moveLocation(x: Float, y: Float, useAnimation: Boolean) {
        TODO("Not yet implemented")
    }

    override fun moveLocationByVector(x: Float, y: Float, useAnimation: Boolean) {
        TODO("Not yet implemented")
    }

    override fun moveToEdge() {
        TODO("Not yet implemented")
    }

    override fun updateView() {
    }
}
