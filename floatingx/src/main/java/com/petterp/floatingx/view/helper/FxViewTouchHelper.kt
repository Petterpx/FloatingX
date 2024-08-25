package com.petterp.floatingx.view.helper

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.petterp.floatingx.assist.FxDisplayMode
import com.petterp.floatingx.util.INVALID_TOUCH_ID
import com.petterp.floatingx.util.TOUCH_CLICK_LONG_TIME
import com.petterp.floatingx.util.TOUCH_TIME_THRESHOLD
import com.petterp.floatingx.util.pointerId
import com.petterp.floatingx.view.FxBasicContainerView
import kotlin.math.abs

/**
 * 手势事件辅助类，处理各种手势类事件的分发
 * @author petterp
 */
class FxViewTouchHelper : FxViewBasicHelper() {
    private var initX = 0f
    private var initY = 0f
    private var scaledTouchSlop = 0F
    private var isClickEvent = false
    private var isEnableClick = true
    private var mLastTouchDownTime = 0L
    private var touchDownId = INVALID_TOUCH_ID

    @SuppressLint("ClickableViewAccessibility")
    override fun initConfig(parentView: FxBasicContainerView) {
        super.initConfig(parentView)
        scaledTouchSlop = ViewConfiguration.get(parentView.context).scaledTouchSlop.toFloat()
        resetConfig()
    }

    // 不通过setTouchListener()方法设置的监听器主要是为外部留口，如果外部需要更强的灵活性，则可以自行实现
    fun touchEvent(event: MotionEvent, basicView: FxBasicContainerView): Boolean {
        if (config.displayMode != FxDisplayMode.DisplayOnly) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> initTouchDown(event)
                MotionEvent.ACTION_MOVE -> touchToMove(event)
                MotionEvent.ACTION_POINTER_DOWN -> touchToPointerDown(event)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_POINTER_UP -> touchCancel(event)
            }
        }
        return config.iFxTouchListener?.onTouch(event, basicView) ?: false
    }

    fun interceptTouchEvent(event: MotionEvent): Boolean {
        // 仅展示时，不拦截事件
        if (config.displayMode == FxDisplayMode.DisplayOnly) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (hasMainPointerId()) return false
                initTouchDown(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isCurrentPointerId(event)) return false
                return config.displayMode.canMove && canInterceptEvent(event)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isCurrentPointerId(event)) return false
                resetConfig()
                config.fxLog.d("fxView -> interceptEventCancel")
            }
        }
        return false
    }

    private fun canInterceptEvent(event: MotionEvent) =
        abs(event.rawX - initX) >= scaledTouchSlop || abs(event.rawY - initY) >= scaledTouchSlop

    private fun initTouchDown(event: MotionEvent) {
        if (hasMainPointerId()) return
        initClickConfig(event)
        touchDownId = event.pointerId
        basicView?.onTouchDown(event)
        config.iFxTouchListener?.onDown()
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
        // 不支持move时return掉
        if (!config.displayMode.canMove) return
        basicView?.onTouchMove(event)
        val x = basicView?.currentX() ?: -1f
        val y = basicView?.currentY() ?: -1f
        config.iFxTouchListener?.onDragIng(event, x, y)
        config.fxLog.v("fxView -> touchMove,x:$x,y:$y")
    }

    private fun touchCancel(event: MotionEvent) {
        if (config.enableEdgeAdsorption && config.displayMode.canMove) basicView?.moveToEdge()
        basicView?.onTouchCancel(event)
        config.iFxTouchListener?.onUp()
        performClickAction()
        config.fxLog.d("fxView -> mainTouchUp")
    }

    private fun performClickAction() {
        if (isClickEvent && config.hasClickStatus) {
            val diffTime = System.currentTimeMillis() - mLastTouchDownTime
            if (diffTime < TOUCH_TIME_THRESHOLD && isEnableClick) {
                if (config.clickTime > 0) {
                    isEnableClick = false
                    basicView?.postDelayed({ isEnableClick = true }, config.clickTime)
                }
                config.iFxClickListener?.onClick(basicView)
            } else if (diffTime >= TOUCH_CLICK_LONG_TIME) {
                val isHandle = config.iFxLongClickListener?.onLongClick(basicView) ?: false
                if (isHandle) basicView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
        resetConfig()
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

    private fun resetConfig() {
        initX = 0f
        initY = 0f
        isClickEvent = false
        mLastTouchDownTime = 0L
        touchDownId = INVALID_TOUCH_ID
    }

    private fun hasMainPointerId() = touchDownId != INVALID_TOUCH_ID
}
