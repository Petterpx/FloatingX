package com.petterp.floatingx.view.basic

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.Keep
import com.petterp.floatingx.util.INVALID_TOUCH_ID
import com.petterp.floatingx.util.TOUCH_TIME_THRESHOLD
import com.petterp.floatingx.util.pointerId
import kotlin.math.abs

/**
 * 手势事件辅助类，处理点击事件
 * @author petterp
 */
class FxViewTouchHelper : FxBasicViewHelper() {
    private var initX = 0f
    private var initY = 0f
    private var scaledTouchSlop = 0F
    private var isClickEvent = false
    private var clickEnable = true
    private var mLastTouchDownTime = 0L
    private var touchDownId = INVALID_TOUCH_ID

    @SuppressLint("ClickableViewAccessibility")
    override fun initConfig(parentView: FxBasicParentView) {
        super.initConfig(parentView)
        reset()
        scaledTouchSlop = ViewConfiguration.get(parentView.context).scaledTouchSlop.toFloat()
        parentView.setOnTouchListener { _, event ->
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

    fun interceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (hasMainPointerId()) return false
                initTouchDown(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isCurrentPointerId(event)) return false
                return abs(event.x - initX) >= scaledTouchSlop ||
                    abs(event.y - initY) >= scaledTouchSlop
            }
        }
        return false
    }

    @Keep
    fun touchCancel(view: View) {
        if (isClickEffective()) {
            config.iFxClickListener?.onClick(view)
            if (config.clickTime > 0) {
                clickEnable = false
                view.postDelayed({ clickEnable = true }, config.clickTime)
            } else {
                clickEnable = true
            }
            config.fxLog.d("fxView -> click")
        }
        reset()
    }

    private fun initTouchDown(event: MotionEvent) {
        if (hasMainPointerId()) return
        initClickConfig(event)
        touchDownId = event.pointerId
        basicView?.onTouchDown(event)
        config.fxLog.d("fxView -> initDownTouch,mainTouchId:$touchDownId")
    }

    private fun initClickConfig(event: MotionEvent) {
        this.initX = event.rawX
        this.initY = event.rawY
        if (!config.enableClickListener || config.iFxClickListener == null) return
        isClickEvent = true
        mLastTouchDownTime = System.currentTimeMillis()
    }

    private fun touchToPointerDown(event: MotionEvent) {
        if (hasMainPointerId()) {
            config.fxLog.d("fxView -> touchToPointerDown: currentId:${event.pointerId}, mainTouchId:$touchDownId exist,return")
            return
        }
        // Before the event starts, check first
        if (basicView?.preCheckPointerDownTouch(event) != true) {
            config.fxLog.d("fxView -> touchToPointerDown: current touch location error,return")
            return
        }
        initTouchDown(event)
    }

    private fun touchToMove(event: MotionEvent) {
        if (!isCurrentPointerId(event)) return
        checkClickState(event)
        basicView?.onTouchMove(event)
        config.fxLog.v("fxView -> touchMove,rawX:${event.rawX},rawY:${event.rawY}")
    }

    private fun touchCancel(event: MotionEvent) {
        if (!isCurrentPointerId(event)) return
        performClick()
        reset()
        basicView?.moveToEdge()
        basicView?.onTouchCancel(event)
        config.fxLog.d("fxView -> mainTouchUp")
    }

    private fun performClick() {
        if (isClickEffective()) {
            clickEnable = false
            config.iFxClickListener?.onClick(basicView)
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

    private fun hasMainPointerId() = touchDownId != INVALID_TOUCH_ID

    private fun isClickEffective(): Boolean {
        // 当前是点击事件&&点击事件目前可启用&&回调存在&&点击时间小于阈值
        return isClickEvent && clickEnable && config.enableClickListener &&
            config.iFxClickListener != null &&
            System.currentTimeMillis() - mLastTouchDownTime < TOUCH_TIME_THRESHOLD
    }
}
