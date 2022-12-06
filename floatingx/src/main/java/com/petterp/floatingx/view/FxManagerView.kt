package com.petterp.floatingx.view

import android.annotation.SuppressLint
import android.content.Context
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
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.util.FX_GRAVITY_BOTTOM
import com.petterp.floatingx.util.FX_GRAVITY_TOP
import com.petterp.floatingx.util.coerceInFx
import com.petterp.floatingx.util.topActivity

/** 基础悬浮窗View */
@SuppressLint("ViewConstructor")
class FxManagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private lateinit var helper: BasisHelper
    private var mLastTouchDownTime = 0L
    private var mParentWidth = 0f
    private var mParentHeight = 0f

    private var isNearestLeft = true
    private var mPortraitY = 0f
    private var downTouchX = 0f
    private var downTouchY = 0f
    private var touchDownId = 0

    private var minHBoundary = 0f
    private var maxHBoundary = 0f
    private var minWBoundary = 0f
    private var maxWBoundary = 0f

    private var isClickEnable = true
    private var isMoveLoading = false
    private var scaledTouchSlop = 0

    private var _childFxView: View? = null
    val childFxView: View? get() = _childFxView
    private var mMoveAnimator: MoveAnimator = MoveAnimator()

    fun init(config: BasisHelper): FxManagerView {
        this.helper = config
        initView()
        return this
    }

    private fun initView() {
        _childFxView = inflateLayoutView() ?: inflateLayoutId()
        checkNotNull(_childFxView) { "initFxView -> Error,check your layoutId or layoutView." }
        initLocation()
        isClickable = true
        scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        helper.iFxViewLifecycle?.initView(this)
        // 注意这句代码非常重要,可以避免某些情况下View被隐藏掉
        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun inflateLayoutView(): View? {
        val view = helper.layoutView?.get() ?: return null
        helper.fxLog?.d("fxView-->init, way:[layoutView]")
        val lp = layoutParams ?: LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        addViewInLayout(view, -1, lp, true)
        return view
    }

    private fun inflateLayoutId(): View? {
        if (helper.layoutId == 0) return null
        helper.fxLog?.d("fxView-->init, way:[layoutId]")
        return inflate(context, helper.layoutId, this)
    }

    private fun initLocation() {
        // 初始化lp
        val configImpl = helper.iFxConfigStorage
        val hasConfig = configImpl?.hasConfig() ?: false
        val lp = helper.layoutParams ?: LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        // 不存在历史坐标时,设置gravity,默认左上角
        if (!hasConfig) lp.gravity = helper.gravity.value
        layoutParams = lp

        // 获得浮窗的位置
        // 存在历史位置 || 根据配置去获取
        val (initX, initY) = if (hasConfig) configImpl!!.getX() to configImpl.getY()
        else initDefaultXY()
        if (initX != -1F) x = initX
        if (initY != -1F) y = initY
        helper.fxLog?.d("fxView->initLocation,isHasConfig-($hasConfig),defaultX-($initX),defaultY-($initY)")
    }

    private fun initDefaultXY(): Pair<Float, Float> {
        // 非辅助定位&&非默认位置,此时x,y不可信
        if (!helper.enableAssistLocation && !helper.gravity.isDefault()) {
            helper.fxLog?.e(
                "fxView--默认坐标可能初始化异常,如果显示位置异常,请检查您的gravity是否为默认配置，当前gravity:${helper.gravity}。\n" +
                    "如果您要配置gravity,建议您启用辅助定位setEnableAssistDirection(),此方法将更便于定位。"
            )
        }
        return helper.defaultX to checkDefaultY(helper.defaultY)
    }

    private fun checkDefaultY(y: Float): Float {
        // 单独处理状态栏和底部导航栏
        var defaultY = y
        when (helper.gravity.scope) {
            FX_GRAVITY_TOP -> defaultY += helper.statsBarHeight
            FX_GRAVITY_BOTTOM -> defaultY -= helper.navigationBarHeight
            else -> {}
        }
        return defaultY
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (updateWidgetSize() && helper.enableAbsoluteFix) moveToEdge()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        var intercepted = false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initTouchDown(ev)
                helper.fxLog?.d("fxView---onInterceptTouchEvent-[down],interceptedTouch-$intercepted")
            }
            MotionEvent.ACTION_MOVE -> {
                intercepted = kotlin.math.abs(downTouchX - ev.x) >= scaledTouchSlop
                helper.fxLog?.v("fxView---onInterceptTouchEvent-[move], interceptedTouch-$intercepted")
            }
        }
        return intercepted
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        helper.iFxScrollListener?.eventIng(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (touchDownId == INVALID_TOUCH_ID) {
                    val eventX = event.getX(event.actionIndex)
                    val eventY = event.getY(event.actionIndex)
                    if (eventX >= 0 && eventX <= width && eventY >= 0 && eventY <= height) {
                        initTouchDown(event)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchDownId == INVALID_TOUCH_ID || !helper.enableTouch) {
                    return super.onTouchEvent(event)
                }
                val pointIdx = event.findPointerIndex(touchDownId)
                if (pointIdx != INVALID_TOUCH_IDX) {
                    updateLocation(event, pointIdx)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == touchDownId) {
                    actionTouchCancel()
                    helper.fxLog?.d("fxView---onTouchEvent--ACTION_POINTER_UP---clearTouchId->")
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                helper.fxLog?.d("fxView---onTouchEvent--End")
                actionTouchCancel()
                clickManagerView()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        helper.iFxViewLifecycle?.attach()
        helper.fxLog?.d("fxView-lifecycle-> onAttachedToWindow")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        helper.iFxViewLifecycle?.detached()
        helper.fxLog?.d("fxView-lifecycle-> onDetachedFromWindow")
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        helper.iFxViewLifecycle?.windowsVisibility(visibility)
        helper.fxLog?.d("fxView-lifecycle-> onWindowVisibilityChanged")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        helper.fxLog?.d("fxView--lifecycle-> onConfigurationChanged--")
        val parentGroup = (parent as? ViewGroup) ?: return
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        var isNavigationCHanged = false
        // 对于全局的处理
        if (helper is AppHelper) {
            val navigationBarHeight = helper.navigationBarHeight
            (helper as AppHelper).updateNavigationBar(topActivity)
            isNavigationCHanged = navigationBarHeight != helper.navigationBarHeight
        }
        if (isLandscape || isNavigationCHanged) {
            mPortraitY = y
        }
        isMoveLoading = false

        // 如果视图大小改变,则更新位置
        parentGroup.post {
            if (updateWidgetSize()) {
                moveToEdge(isLandscape = isLandscape)
            }
        }
    }

    private fun clickManagerView() {
        if (helper.enableClickListener && isClickEnable && helper.iFxClickListener != null && isOnClickEvent()) {
            isClickEnable = false
            helper.iFxClickListener!!.onClick(this)
            postDelayed({ isClickEnable = true }, helper.clickTime)
            helper.fxLog?.d("fxView -> click")
        }
    }

    private fun actionTouchCancel() {
        helper.iFxScrollListener?.up()
        mPortraitY = 0f
        touchDownId = INVALID_TOUCH_ID
        moveToEdge()
    }

    private fun isOnClickEvent(): Boolean {
        return System.currentTimeMillis() - mLastTouchDownTime < TOUCH_TIME_THRESHOLD
    }

    private fun updateBoundary(isDownTouchInit: Boolean) {
        // 开启边缘回弹时,浮窗允许移动到边界外
        if (helper.enableEdgeRebound) {
            val edgeOffset = if (isDownTouchInit) 0f else helper.edgeOffset
            val marginTop = if (isDownTouchInit) 0f else helper.borderMargin.t + edgeOffset
            val marginBto = if (isDownTouchInit) 0f else helper.borderMargin.b + edgeOffset
            val marginLef = if (isDownTouchInit) 0f else helper.borderMargin.l + edgeOffset
            val marginRig = if (isDownTouchInit) 0f else helper.borderMargin.r + edgeOffset
            minWBoundary = marginLef
            maxWBoundary = mParentWidth - marginRig
            minHBoundary = helper.statsBarHeight.toFloat() + marginTop
            maxHBoundary = mParentHeight - helper.navigationBarHeight - marginBto
        } else {
            minWBoundary = helper.borderMargin.l
            maxWBoundary = mParentWidth - helper.borderMargin.r
            minHBoundary = helper.statsBarHeight + helper.borderMargin.t
            maxHBoundary = mParentHeight - helper.navigationBarHeight - helper.borderMargin.b
        }
    }

    private fun initTouchDown(ev: MotionEvent) {
        updateWidgetSize()
        updateBoundary(true)
        touchDownId = ev.getPointerId(ev.actionIndex)
        downTouchX = ev.getX(ev.actionIndex)
        downTouchY = ev.getY(ev.actionIndex)
        // init width and height boundary
        mMoveAnimator.stop()
        helper.iFxScrollListener?.down()
        if (helper.enableClickListener) mLastTouchDownTime = System.currentTimeMillis()
        helper.fxLog?.d("fxView---newTouchDown:$touchDownId")
    }

    private fun updateLocation(event: MotionEvent, pointIndex: Int) {
        val disX = x.plus(event.getX(pointIndex)).minus(downTouchX)
            .coerceInFx(minWBoundary, maxWBoundary)
        val disY = y.plus(event.getY(pointIndex)).minus(downTouchY)
            .coerceInFx(minHBoundary, maxHBoundary)
        x = disX
        y = disY
        helper.iFxScrollListener?.dragIng(event, disX, disY)
        helper.fxLog?.v("fxView---scrollListener--drag-event--x($disX)-y($disY)")
    }

    private fun updateWidgetSize(): Boolean {
        // 如果此时浮窗被父布局移除,parent将为null,此时就别更新位置了,没意义
        val parentGroup = (parent as? ViewGroup) ?: return false
        // 这里先减掉自身大小可以避免后期再重复减掉
        val parentWidth = (parentGroup.width - this@FxManagerView.width).toFloat()
        val parentHeight = (parentGroup.height - this@FxManagerView.height).toFloat()
        if (mParentHeight != parentHeight || mParentWidth != parentWidth) {
            helper.fxLog?.d("fxView->updateContainerSize: oldW-($mParentWidth),oldH-($mParentHeight),newW-($parentWidth),newH-($parentHeight)")
            mParentWidth = parentWidth
            mParentHeight = parentHeight
            return true
        }
        return false
    }

    private fun isNearestLeft(): Boolean {
        val middle = mParentWidth / 2
        isNearestLeft = x < middle
        return isNearestLeft
    }

    @JvmSynthetic
    internal fun moveToEdge(isLeft: Boolean = isNearestLeft(), isLandscape: Boolean = false) {
        if (isMoveLoading) return
        // 允许边缘吸附
        if (helper.enableEdgeAdsorption) {
            updateBoundary(false)
            autoMove(isLeft, isLandscape)
            return
        }
        // 允许边缘回弹
        if (helper.enableEdgeRebound) {
            updateBoundary(false)
            val currentX = x.coerceInFx(minWBoundary, maxWBoundary)
            val currentY = y.coerceInFx(minHBoundary, maxHBoundary)
            if (currentX != x || currentY != y) {
                isMoveLoading = true
                moveLocation(currentX, currentY)
            }
        }
    }

    private fun autoMove(isLeft: Boolean, isLandscape: Boolean) {
        isMoveLoading = true
        var moveY = y
        val moveX = if (isLeft) minWBoundary else maxWBoundary
        // 对于重建之后的位置保存
        if (isLandscape && mPortraitY != 0f) {
            moveY = mPortraitY
            mPortraitY = 0f
        }
        moveY = moveY.coerceInFx(minHBoundary, maxHBoundary)
        moveLocation(moveX, moveY)
    }

    @JvmSynthetic
    internal fun updateLocation(x: Float, y: Float) {
        (layoutParams as LayoutParams).gravity = FxGravity.DEFAULT.value
        this.x = x
        this.y = y
        helper.fxLog?.d("fxView-updateManagerView-> RestoreLocation  x->$x,y->$y")
    }

    private fun moveLocation(moveX: Float, moveY: Float) {
        if (moveX == x && moveY == y) {
            isMoveLoading = false
            return
        }
        mMoveAnimator.start(moveX, moveY)
        helper.fxLog?.d("fxView-->moveToEdge---x-($x)，y-($y) ->  moveX-($moveX),moveY-($moveY)")
        if (helper.enableSaveDirection) {
            saveConfig(moveX, moveY)
        }
    }

    private fun saveConfig(moveX: Float, moveY: Float) {
        if (helper.iFxConfigStorage == null) {
            helper.fxLog?.e("fxView-->saveDirection---iFxConfigStorageImpl does not exist, save failed!")
            return
        }
        helper.iFxConfigStorage?.update(moveX, moveY)
        helper.fxLog?.d("fxView-->saveDirection---x-($moveX)，y-($moveY)")
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
            if (childFxView == null || childFxView?.parent == null) return
            val progress =
                MAX_PROGRESS.coerceAtMost((System.currentTimeMillis() - startingTime) / DEFAULT_MOVE_ANIMATOR_DURATION)
            x += (destinationX - x) * progress
            y += (destinationY - y) * progress
            if (progress < MAX_PROGRESS) {
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

    companion object {
        private const val INVALID_TOUCH_ID = -1
        private const val INVALID_TOUCH_IDX = -1
        private const val TOUCH_TIME_THRESHOLD = 150L
        private const val MAX_PROGRESS = 1f
        private const val DEFAULT_MOVE_ANIMATOR_DURATION = 400f
        private val HANDLER = Handler(Looper.getMainLooper())
    }
}
