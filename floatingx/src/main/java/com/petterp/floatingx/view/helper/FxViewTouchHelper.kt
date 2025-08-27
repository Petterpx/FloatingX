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
    private var isLongPressActivated = false
    private var isDownOnClickableChild = false

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
                // 长按移动模式：只有在长按激活后才拦截移动事件，除非不在可点击子视图上
                return if (config.displayMode.canLongPressMove) {
                    (isLongPressActivated || !isDownOnClickableChild) && canInterceptEvent(event)
                } else {
                    config.displayMode.canMove && canInterceptEvent(event)
                }
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
        // Check if touch is on a clickable child view for LongPressMove mode
        isDownOnClickableChild = if (config.displayMode.canLongPressMove) {
            checkTouchOnClickableChild(event)
        } else {
            false
        }
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
        // Check if movement is allowed
        val canMoveNow = if (config.displayMode.canLongPressMove) {
            // For long press move mode, check if long press is activated and enough time has passed
            // But allow immediate movement if not touching a clickable child
            val timeSinceDown = System.currentTimeMillis() - mLastTouchDownTime
            if (timeSinceDown >= TOUCH_CLICK_LONG_TIME && !isLongPressActivated) {
                isLongPressActivated = true
                // Trigger haptic feedback for long press
                basicView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                config.fxLog.d("fxView -> long press activated for movement")
            }
            // Allow movement if long press is activated OR if not touching a clickable child
            isLongPressActivated || !isDownOnClickableChild
        } else {
            config.displayMode.canMove
        }
        
        // 不支持move时return掉
        if (!canMoveNow) return
        basicView?.onTouchMove(event)
        val x = basicView?.x ?: 0f
        val y = basicView?.y ?: 0f
        config.iFxTouchListener?.onDragIng(event, x, y)
        config.fxLog.v("fxView -> touchMove,x:$x,y:$y")
    }

    private fun touchCancel(event: MotionEvent) {
        val canMoveForEdge = config.displayMode.canMove || (config.displayMode.canLongPressMove && (isLongPressActivated || !isDownOnClickableChild))
        if (config.enableEdgeAdsorption && canMoveForEdge) basicView?.moveToEdge()
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
            } else if (diffTime >= TOUCH_CLICK_LONG_TIME && !config.displayMode.canLongPressMove) {
                // Only trigger long click listener if not in long press move mode (to avoid conflicts)
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
        isLongPressActivated = false
        isDownOnClickableChild = false
    }

    private fun hasMainPointerId() = touchDownId != INVALID_TOUCH_ID

    /**
     * Check if the touch event is on a clickable child view
     * This helps determine whether to apply long press behavior or allow immediate interaction
     */
    private fun checkTouchOnClickableChild(event: MotionEvent): Boolean {
        val container = basicView ?: return false
        val childView = container.childView ?: return false
        
        // Convert touch coordinates to child view coordinates
        val touchX = event.rawX
        val touchY = event.rawY
        
        // Find the target view at the touch position
        val targetView = findViewAtPosition(childView, touchX, touchY)
        
        // Check if the target view or any of its parents are clickable
        return isViewClickable(targetView)
    }
    
    /**
     * Recursively find the view at the specified global coordinates
     */
    private fun findViewAtPosition(view: android.view.View, x: Float, y: Float): android.view.View? {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height
        
        // Check if touch is within this view's bounds
        if (x < left || x > right || y < top || y > bottom) {
            return null
        }
        
        // If this is a ViewGroup, check its children first
        if (view is android.view.ViewGroup) {
            for (i in view.childCount - 1 downTo 0) {
                val child = view.getChildAt(i)
                val targetView = findViewAtPosition(child, x, y)
                if (targetView != null) {
                    return targetView
                }
            }
        }
        
        // Return this view if no child was found at the position
        return view
    }
    
    /**
     * Check if a view is clickable (has click listeners or is inherently clickable)
     */
    private fun isViewClickable(view: android.view.View?): Boolean {
        if (view == null) return false
        
        // Check if view is clickable or has click listeners
        return view.isClickable || view.hasOnClickListeners()
    }
}
