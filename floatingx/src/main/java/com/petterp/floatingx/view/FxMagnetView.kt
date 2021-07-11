package com.petterp.floatingx.view

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.assist.Direction
import com.petterp.floatingx.assist.FxHelper
import com.petterp.floatingx.config.SystemConfig
import com.petterp.floatingx.ext.FxDebug
import com.petterp.floatingx.ext.navigationBarHeight
import com.petterp.floatingx.ext.topActivity
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
    private var mScreenWidth = 0f
    private var mScreenHeight = 0f

    @Volatile
    private var isMoveLoading = false

    private var isNearestLeft = true
    private var mPortraitY = 0f
    private var touchDownX = 0f
    private var touchDownId: Int = 0

    @Volatile
    private var isClickEnable: Boolean = true
    internal var childView: View? = null

    init {
        mMoveAnimator = MoveAnimator()
        isClickable = true
        if (helper.layoutId != 0) {
            childView = inflate(context, helper.layoutId, this)
            helper.layoutParams?.let {
                childView?.layoutParams = helper.layoutParams
            }
            FxDebug.d("view-->init, source-[layout]")
        }
        val hasConfig = helper.iFxConfigStorage?.hasConfig() ?: false
        layoutParams = defaultLayoutParams(hasConfig)
        x = if (hasConfig) helper.iFxConfigStorage!!.getX() else helper.x
        y = if (hasConfig) helper.iFxConfigStorage!!.getY() else initDefaultY()
        FxDebug.d("view->x&&y   hasConfig-($hasConfig),x-($x),y-($y)")
        setBackgroundColor(Color.TRANSPARENT)
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

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (helper.enableAbsoluteFix && updateSize())
            moveToEdge()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_MOVE -> updateViewPosition(event)
            MotionEvent.ACTION_UP -> {
                FxDebug.d("view---onTouchEvent--up")
                actionTouchCancel()
                clickManagerView()
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

    private fun clickManagerView() {
        if (isClickEnable && isOnClickEvent()) {
            isClickEnable = false
            FxDebug.d("view -> click")
            helper.clickListener?.invoke(this)
            postDelayed({ isClickEnable = true }, helper.clickTime)
        }
    }

    private fun actionTouchCancel() {
        helper.iFxScrollListener?.up()
        clearPortraitY()
        touchDownId = 0
        moveToEdge()
    }

    private fun initDefaultY(): Float {
        var defaultY = helper.y
        // 向下 ->
        if (helper.y < 0) {
            defaultY -= SystemConfig.navigationBarHeight
        } else if (helper.y > 0) {
            defaultY += SystemConfig.statsBarHeight
        } else {
            if (helper.gravity == Direction.RIGHT_OR_TOP || helper.gravity == Direction.LEFT_OR_TOP)
                defaultY += SystemConfig.statsBarHeight
            else if (helper.gravity == Direction.LEFT_OR_BOTTOM || helper.gravity == Direction.RIGHT_OR_BOTTOM)
                defaultY -= SystemConfig.navigationBarHeight
        }
        return defaultY
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
            var desY = mOriginalY + event.rawY - mOriginalRawY
            // 如果允许边界外滚动，则Y轴只需要考虑状态栏与导航栏,即可超出的范围为提供的边界与marginEdge
            if (helper.enableScrollOutsideScreen) {
                if (desY < SystemConfig.statsBarHeight) {
                    desY = SystemConfig.statsBarHeight.toFloat()
                }
                val statusY = mScreenHeight - SystemConfig.navigationBarHeight
                if (desY > statusY) {
                    desY = statusY
                }
            } else {
                val moveX = helper.borderMargin.l + helper.edgeOffset
                val moveMaxX = mScreenWidth - helper.borderMargin.r - helper.edgeOffset
                val moveY = SystemConfig.statsBarHeight + helper.borderMargin.t + helper.edgeOffset
                val moveMaxY =
                    mScreenHeight - SystemConfig.navigationBarHeight - helper.edgeOffset - helper.borderMargin.b
                if (desX < moveX) desX = moveX
                if (desX > moveMaxX) desX = moveMaxX
                if (desY < moveY) desY = moveY
                if (desY > moveMaxY) desY = moveMaxY
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

    private fun updateSize(): Boolean {
        (parent as ViewGroup).apply {
            val newScreenWidth = (width - this@FxMagnetView.width).toFloat()
            val newScreenHeight = (height - this@FxMagnetView.height).toFloat()
            FxDebug.d("view->size oldW-($mScreenWidth),oldH-($mScreenHeight),newW-($newScreenWidth),newH-($newScreenHeight)")
            if (mScreenHeight != newScreenHeight || mScreenWidth != newScreenWidth) {
                mScreenWidth = newScreenWidth
                mScreenHeight = newScreenHeight
                return true
            }
            return false
        }
    }

    @JvmOverloads
    fun moveToEdge(isLeft: Boolean = isNearestLeft(), isLandscape: Boolean = false) {
        if (isMoveLoading) return
        isMoveLoading = true
        var moveX = x
        var moveY = y
        if (helper.enableEdgeAdsorption) {
            moveX =
                if (isLeft) helper.edgeOffset + helper.borderMargin.l else mScreenWidth - helper.edgeOffset - helper.borderMargin.r
            // 对于重建之后的位置保存
            if (isLandscape && mPortraitY != 0f) {
                moveY = mPortraitY
                clearPortraitY()
            } else {
                topActivity?.let {
                    SystemConfig.navigationBarHeight = it.navigationBarHeight
                }
            }
            // 拿到y轴目前应该在的距离
            moveY =
                (helper.borderMargin.t + helper.edgeOffset + SystemConfig.statsBarHeight).coerceAtLeast(
                    moveY
                )
                    .coerceAtMost((mScreenHeight - helper.borderMargin.b - helper.edgeOffset - SystemConfig.navigationBarHeight))
            if (moveY == y && x == moveX) {
                isMoveLoading = false
                return
            }
            mMoveAnimator?.start(moveX, moveY)
            FxDebug.d("view-->moveToEdge---x-($x)，y-($y) ->  moveX-($moveX),moveY-($moveY)")
        }
        saveConfig(moveX, moveY)
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
        FxDebug.d("view--lifecycle-> onConfigurationChanged--")
        if (parent != null) {
            val newNavigationBarHeight = topActivity?.navigationBarHeight ?: 0
            val isLandscape =
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || SystemConfig.navigationBarHeight != newNavigationBarHeight
            markPortraitY(isLandscape)
            isMoveLoading = false
            (parent as ViewGroup).post {
                if (updateSize())
                    moveToEdge(isLandscape = isLandscape)
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
        internal val HANDLER = Handler(Looper.getMainLooper())
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
            } else {
                isMoveLoading = false
            }
        }

        fun stop() {
            isMoveLoading = false
            HANDLER.removeCallbacks(this)
        }
    }

    private fun defaultLayoutParams(hasConfig: Boolean) = LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
    ).apply {
        if (!hasConfig)
            gravity = helper.gravity.value
    }

    private fun saveConfig(moveX: Float, moveY: Float) {
        helper.iFxConfigStorage?.apply {
            setX(moveX)
            setY(moveY)
            setVersionCode(getVersionCode() + 1)
            FxDebug.d("view-->saveDirection---x-($x)，y-($y)")
        }
    }
}
