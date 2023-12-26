package com.petterp.floatingx.view.basic

import android.animation.ValueAnimator
import com.petterp.floatingx.util.DEFAULT_MOVE_ANIMATOR_DURATION

/**
 * FxView基础辅助类
 * @author petterp
 */
class FxViewAnimationHelper : FxBasicViewHelper() {
    private var valueAnimator: ValueAnimator? = null
    private var animateListener: IFxViewAnimate? = null
    private var startX: Float = 0f
    private var startY: Float = 0f
    private var endX: Float = 0f
    private var endY: Float = 0f

    fun setListener(iFxViewAnimate: IFxViewAnimate) {
        this.animateListener = iFxViewAnimate
    }

    fun start(endX: Float, endY: Float) {
        val startX = basicView?.currentX() ?: 0f
        val startY = basicView?.currentY() ?: 0f
        if (startX == endX && startY == endY) return
        this.startX = startX
        this.startY = startY
        this.endX = endX
        this.endY = endY
        checkOrInitAnimator()
        if (valueAnimator?.isRunning == true) valueAnimator?.cancel()
        valueAnimator?.start()
    }

    private fun checkOrInitAnimator() {
        if (valueAnimator == null) {
            valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DEFAULT_MOVE_ANIMATOR_DURATION
                addUpdateListener {
                    val fraction = it.animatedValue as Float
                    val x = calculationNumber(startX, endX, fraction)
                    val y = calculationNumber(startY, endY, fraction)
                    basicView?.updateXY(x, y)
                }
            }
        }
    }

    private fun calculationNumber(start: Float, end: Float, fraction: Float): Float {
        val currentX = if (start == end) {
            start
        } else {
            start + (end - start) * fraction
        }
        return currentX
    }
}

typealias IFxViewAnimate = (x: Float, y: Float) -> Unit
