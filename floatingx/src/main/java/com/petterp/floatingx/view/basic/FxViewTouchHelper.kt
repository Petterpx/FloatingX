package com.petterp.floatingx.view.basic

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.Keep
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.util.INVALID_TOUCH_ID
import com.petterp.floatingx.util.TOUCH_TIME_THRESHOLD
import com.petterp.floatingx.util.pointerId
import kotlin.math.abs

/**
 * 手势事件辅助类，处理点击事件
 * @author petterp
 */
class FxViewTouchHelper {
    private var initX = 0f
    private var initY = 0f
    private var scaledTouchSlop = 0F
    private var isClickEvent = false
    private var clickEnable = true
    private var mLastTouchDownTime = 0L
    private var touchDownId = INVALID_TOUCH_ID
    private lateinit var helper: FxBasisHelper
    private var basicView: FxBasicParentView? = null

    @SuppressLint("ClickableViewAccessibility")
    fun initConfig(view: FxBasicParentView) {
        reset()
        this.basicView = view
        this.helper = view.helper
        scaledTouchSlop = ViewConfiguration.get(view.context).scaledTouchSlop.toFloat()
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> initTouchDown(event)
                MotionEvent.ACTION_MOVE -> touchToMove(event)
                MotionEvent.ACTION_POINTER_DOWN -> touchToPointerDown(event)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_POINTER_UP -> touchCancel(event)
            }
            false
        }
    }

    fun hasMainPointerId() = touchDownId != INVALID_TOUCH_ID

    @Keep
    fun touchCancel(view: View) {
        if (isClickEffective()) {
            helper.iFxClickListener?.onClick(view)
            if (helper.clickTime > 0) {
                clickEnable = false
                view.postDelayed({ clickEnable = true }, helper.clickTime)
            } else {
                clickEnable = true
            }
            helper.fxLog?.d("fxView -> click")
        }
        reset()
    }

    private fun initTouchDown(event: MotionEvent) {
        if (hasMainPointerId()) return
        initClickConfig(event)
        touchDownId = event.pointerId
        basicView?.onTouchDown(event)
        helper.fxLog?.d("fxView->initTouchDown---id:$touchDownId->")
    }

    private fun initClickConfig(event: MotionEvent) {
        if (!helper.enableClickListener || helper.iFxClickListener == null) return
        isClickEvent = true
        this.initX = event.rawX
        this.initY = event.rawY
        mLastTouchDownTime = System.currentTimeMillis()
    }

    private fun touchToPointerDown(event: MotionEvent) {
        if (hasMainPointerId()) return
        initTouchDown(event)
    }

    private fun touchToMove(event: MotionEvent) {
        if (!isCurrentPointerId(event)) return
        checkClickState(event)
        basicView?.onTouchMove(event)
        helper.fxLog?.d("fxView---onTouchEvent---onTouchMove->")
    }

    private fun touchCancel(event: MotionEvent) {
        if (!isCurrentPointerId(event)) return
        performClick()
        reset()
        basicView?.onTouchCancel(event)
        helper.fxLog?.d("fxView---onTouchEvent---MainTouchCancel->")
    }

    private fun performClick() {
        if (isClickEffective()) {
            clickEnable = false
            helper.iFxClickListener?.onClick(basicView)
            basicView?.postDelayed({
                clickEnable = true
            }, 1000)
        }
    }

    private fun checkClickState(event: MotionEvent) {
        if (!isClickEvent) return
        isClickEvent = abs(event.rawX - initX) < scaledTouchSlop &&
            abs(event.rawY - initY) < scaledTouchSlop
    }

    private fun isCurrentPointerId(ev: MotionEvent): Boolean {
        if (touchDownId == INVALID_TOUCH_ID) return false
        return ev.pointerId == touchDownId
    }

    private fun reset() {
        initX = 0f
        initY = 0f
        isClickEvent = false
        mLastTouchDownTime = 0L
        touchDownId = INVALID_TOUCH_ID
    }

    private fun isClickEffective(): Boolean {
        // 当前是点击事件&&点击事件目前可启用&&回调存在&&点击时间小于阈值
        return isClickEvent && clickEnable && helper.enableClickListener &&
            helper.iFxClickListener != null &&
            System.currentTimeMillis() - mLastTouchDownTime < TOUCH_TIME_THRESHOLD
    }
}
