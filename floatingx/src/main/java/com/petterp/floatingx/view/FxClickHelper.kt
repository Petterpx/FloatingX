package com.petterp.floatingx.view

import androidx.annotation.Keep
import com.petterp.floatingx.assist.helper.BasisHelper
import kotlin.math.abs

/**
 *
 * @author petterp
 */
class FxClickHelper {
    private var initX = 0f
    private var initY = 0f
    private var isClickEvent = false
    private var clickEnable = true
    private var mLastTouchDownTime = 0L
    private lateinit var helper: BasisHelper

    fun initConfig(helper: BasisHelper) {
        reset()
        this.helper = helper
    }

    fun initDown(x: Float, y: Float) {
        if (!helper.enableClickListener || helper.iFxClickListener == null) return
        this.initX = x
        this.initY = y
        isClickEvent = true
        mLastTouchDownTime = System.currentTimeMillis()
    }

    fun checkClickEvent(x: Float, y: Float) {
        if (!isClickEvent) return
        isClickEvent = abs(x - initX) < FxManagerView.TOUCH_CLICK_OFFSET &&
                abs(y - initY) < FxManagerView.TOUCH_CLICK_OFFSET
    }

    @Keep
    fun performClick(view: FxManagerView) {
        if (!isClickEffective()) return
        helper.iFxClickListener?.onClick(view)
        if (helper.clickTime > 0) {
            clickEnable = false
            view.postDelayed({ clickEnable = true }, helper.clickTime)
        }
        helper.fxLog?.d("fxView -> click")
        reset()
    }

    private fun reset() {
        initX = 0f
        initY = 0f
        isClickEvent = false
        mLastTouchDownTime = 0L
    }

    private fun isClickEffective(): Boolean {
        return isClickEvent && clickEnable && helper.enableClickListener &&
                helper.iFxClickListener != null &&
                System.currentTimeMillis() - mLastTouchDownTime < FxManagerView.TOUCH_TIME_THRESHOLD
    }
}
