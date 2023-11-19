package com.petterp.floatingx.view.default

import androidx.annotation.Keep
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.util.TOUCH_CLICK_OFFSET
import com.petterp.floatingx.util.TOUCH_TIME_THRESHOLD
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
    private lateinit var helper: FxBasisHelper

    private val canClick: Boolean
        get() = helper.enableClickListener && helper.iFxClickListener != null


    fun initConfig(helper: FxBasisHelper) {
        reset()
        this.helper = helper
    }

    fun initDown(x: Float, y: Float) {
        if (!canClick) return
        this.initX = x
        this.initY = y
        isClickEvent = true
        mLastTouchDownTime = System.currentTimeMillis()
    }

    fun checkClickEvent(x: Float, y: Float) {
        if (!isClickEvent) return
        isClickEvent = abs(x - initX) < TOUCH_CLICK_OFFSET &&
            abs(y - initY) < TOUCH_CLICK_OFFSET
    }

    @Keep
    fun performClick(view: FxDefaultContainerView) {
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
            System.currentTimeMillis() - mLastTouchDownTime < TOUCH_TIME_THRESHOLD
    }
}
