package com.petterp.floatingx.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import com.petterp.floatingx.assist.helper.FxBasisHelper

/**
 * FxDefault View
 * @author petterp
 */
@SuppressLint("ViewConstructor")
class FxDefaultContainerView(helper: FxBasisHelper, context: Context, attrs: AttributeSet? = null) :
    FxBasicContainerView(helper, context, attrs) {

    private var downTouchX = 0f
    private var downTouchY = 0f

    override fun initView() {
        super.initView()
        isClickable = true
        initLayoutParams()
        installChildView()
        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun initLayoutParams() {
        val lp = helper.layoutParams ?: LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
        )
        lp.apply { gravity = Gravity.START or Gravity.TOP }
        layoutParams = lp
    }

    override fun currentX(): Float = x

    override fun currentY(): Float = y

    override fun updateXY(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    override fun parentSize(): Pair<Int, Int>? {
        val parentGroup = (parent as? ViewGroup) ?: return null
        return parentGroup.width to parentGroup.height
    }

    override fun onTouchDown(event: MotionEvent) {
        downTouchX = event.x
        downTouchY = event.y
    }

    // 计算方式的不同，故与system逻辑有区别
    override fun onTouchMove(event: MotionEvent) {
        val x = x.minus(downTouchX).plus(event.x)
        val y = y.minus(downTouchY).plus(event.y)
        safeUpdatingXY(x, y)
    }

    override fun onTouchCancel(event: MotionEvent) {
        downTouchX = 0f
        downTouchY = 0f
    }
}
