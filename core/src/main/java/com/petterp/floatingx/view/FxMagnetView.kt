package com.petterp.floatingx.view

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.config.FxHelper
import com.petterp.floatingx.ext.FxDebug
import kotlin.math.abs

/**
 * @Author petterp
 * @Date 2021/5/19-7:30 下午
 * @Email ShiyihuiCloud@163.com
 * @Function 基础悬浮窗View 源自 ->
 * https://github.com/shenzhen2017/EasyFloat/blob/main/easyfloat/src/main/java/com/zj/easyfloat/floatingview/FloatingMagnetView.java
 */
@SuppressLint("ViewConstructor")
class FxMagnetView @JvmOverloads constructor(
    private val helper: FxHelper,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(helper.context, attrs, defStyleAttr, defStyleRes) {

    private var mOriginalRawX = 0f
    private var mOriginalRawY = 0f
    private var mOriginalX = 0f
    private var mOriginalY = 0f

    // 最后触摸的时间
    private var mLastTouchDownTime: Long = 0
    private var mMoveAnimator: MoveAnimator? = null
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mStatusBarHeight = 0

    private var isNearestLeft = true
    private var mPortraitY = 0f
    private var touchDownX = 0f
    private var touchDownId: Int = 0
    internal var childView: View? = null

    init {
        mMoveAnimator = MoveAnimator()
        mStatusBarHeight = 0
        isClickable = true
        if (helper.layoutId != 0) {
            childView = inflate(context, helper.layoutId, this)
            helper.layoutParams?.let {
                childView?.layoutParams = helper.layoutParams
            }
        }
        layoutParams = defaultLayoutParams()
        x = helper.x
        y = helper.y
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        var intercepted = false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                FxDebug.d("view---onInterceptTouchEvent--down")
                intercepted = false
                touchDownX = ev.x
                // 初始化按下后的信息
                initTouchDown(ev)
                helper.iFxScrollListener?.down()
            }
            MotionEvent.ACTION_MOVE ->
                // 判断是否要拦截事件
                intercepted =
                    abs(touchDownX - ev.x) >= ViewConfiguration.get(
                        context
                    ).scaledTouchSlop
            MotionEvent.ACTION_UP -> intercepted = false
        }
        return intercepted
    }

    private fun initTouchDown(ev: MotionEvent) {
        changeOriginalTouchParams(ev)
        updateSize()
        touchDownId = ev.getPointerId(0)
        mMoveAnimator?.stop()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_MOVE -> updateViewPosition(event)
            MotionEvent.ACTION_UP -> {
                FxDebug.d("view---onTouchEvent--up")
                actionTouchCancel()
                if (isOnClickEvent()) {
                    helper.clickListener?.invoke(this)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                FxDebug.d("view---onTouchEvent--POINTER_UP")
                if (event.findPointerIndex(touchDownId) == 0) {
                    moveToEdge()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                FxDebug.d("view---onTouchEvent--CANCEL")
                actionTouchCancel()
            }
        }
        return true
    }

    private fun actionTouchCancel() {
        helper.iFxScrollListener?.up()
        clearPortraitY()
        touchDownId = 0
        moveToEdge()
    }

    private fun isOnClickEvent(): Boolean {
        return System.currentTimeMillis() - mLastTouchDownTime < TOUCH_TIME_THRESHOLD
    }

    private fun updateViewPosition(event: MotionEvent) {
        event.findPointerIndex(touchDownId).takeIf {
            it == 0
        }?.let {
            // 按下时的坐标+当前手指距离屏幕的坐标-最开始距离屏幕的坐标
            var desX = mOriginalX + event.rawX - mOriginalRawX
            if (desX < 0f) desX = 0f
            if (desX > mScreenWidth) desX = mScreenWidth.toFloat()
            // 限制不可超出屏幕高度
            var desY = mOriginalY + event.rawY - mOriginalRawY
            if (desY < mStatusBarHeight) {
                desY = mStatusBarHeight.toFloat()
            }
            if (desY > mScreenHeight) {
                desY = mScreenHeight.toFloat()
            }
            x = desX
            y = desY
            helper.iFxScrollListener?.dragIng(x, y)
            FxDebug.v("view---scrollListener--drag--x($x)-y($y)")
        }
    }

    private fun changeOriginalTouchParams(event: MotionEvent) {
        mOriginalX = x
        mOriginalY = y
        mOriginalRawX = event.rawX
        mOriginalRawY = event.rawY
        mLastTouchDownTime = System.currentTimeMillis()
    }

    private fun updateSize() {
        (parent as ViewGroup).apply {
            mScreenWidth = width - this@FxMagnetView.width - helper.lScrollEdge - helper.rScrollEdge
            mScreenHeight = height - this@FxMagnetView.height
        }
    }

    @JvmOverloads
    fun moveToEdge(isLeft: Boolean = isNearestLeft(), isLandscape: Boolean = false) {
        if (!helper.isEdgeEnable) return
        mMoveAnimator?.stop()
        // x坐标
        val moveX =
            if (isLeft) helper.marginEdge + helper.lScrollEdge else mScreenWidth - helper.marginEdge - helper.rScrollEdge
        var y = y
        // 对于重建之后的位置保存
        if (!isLandscape && mPortraitY != 0f) {
            y = mPortraitY
            clearPortraitY()
        }
        // 拿到y轴目前应该在的距离
        val moveY =
            helper.tScrollEdge.toFloat().coerceAtLeast(y)
                .coerceAtMost((mScreenHeight - helper.bScrollEdge).toFloat())
        FxDebug.d("moveToEdge-----x-($x)，y-($y) ->  moveX-($moveX),moveY-($moveY)")
        if (moveY == y && x == moveX) return
        mMoveAnimator?.start(moveX, moveY)
    }

    private fun fixDirection() {
        FxDebug.d("fixDirection-----defaultEdge-(${helper.marginEdge}),x-($x),y-($y),screenWidth-($mScreenWidth)")
        // 如果开启自动吸附&&当前位置不符合边缘
        if (helper.isEdgeEnable && (abs(x) != helper.marginEdge || abs(x) != abs(mScreenWidth - helper.marginEdge)))
            moveToEdge()
    }

    private fun clearPortraitY() {
        mPortraitY = 0f
    }

    private fun isNearestLeft(): Boolean {
        val middle = mScreenWidth / 2
        isNearestLeft = x < middle
        return isNearestLeft
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        FxDebug.d("view---onConfigurationChanged--")
        if (parent != null) {
            val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
            markPortraitY(isLandscape)
            (parent as ViewGroup).post {
                updateSize()
                moveToEdge(isNearestLeft, isLandscape)
            }
        }
    }

    private fun markPortraitY(isLandscape: Boolean) {
        if (isLandscape) {
            mPortraitY = y
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
//        fixDirection()
        helper.iFxViewLifecycle?.attach()
        FxDebug.d("view-lifecycle-> onAttachedToWindow")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        helper.iFxViewLifecycle?.detached()
        FxDebug.d("view-lifecycle-> onDetachedFromWindow")
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        helper.iFxViewLifecycle?.windowsVisibility(visibility)
        FxDebug.d("view-lifecycle-> onWindowVisibilityChanged")
    }

    companion object {
        private const val TOUCH_TIME_THRESHOLD = 150
        private val HANDLER = Handler(Looper.getMainLooper())
    }

    private inner class MoveAnimator : Runnable {
        private var destinationX = 0f
        private var destinationY = 0f
        private var startingTime: Long = 0
        fun start(x: Float, y: Float) {
            destinationX = x
            destinationY = y
            startingTime = System.currentTimeMillis()
            HANDLER.post(this)
        }

        override fun run() {
            if (rootView == null || rootView.parent == null) {
                return
            }
            val progress = 1f.coerceAtMost((System.currentTimeMillis() - startingTime) / 400f)
            x += (destinationX - x) * progress
            y += (destinationY - y) * progress
            if (progress < 1) {
                HANDLER.post(this)
            }
        }

        fun stop() {
            HANDLER.removeCallbacks(this)
        }
    }

    private fun defaultLayoutParams() = LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = helper.gravity.value
    }
}
