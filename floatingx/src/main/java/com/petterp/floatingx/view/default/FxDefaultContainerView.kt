package com.petterp.floatingx.view.default

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.assist.FxDisplayMode
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.util.DEFAULT_MOVE_ANIMATOR_DURATION
import com.petterp.floatingx.util.FX_GRAVITY_BOTTOM
import com.petterp.floatingx.util.FX_GRAVITY_TOP
import com.petterp.floatingx.util.INVALID_LAYOUT_ID
import com.petterp.floatingx.util.INVALID_TOUCH_ID
import com.petterp.floatingx.util.pointerId
import com.petterp.floatingx.util.withIn
import com.petterp.floatingx.view.FxViewHolder
import com.petterp.floatingx.view.IFxInternalView

/** 基础悬浮窗View */
@SuppressLint("ViewConstructor")
class FxDefaultContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), View.OnLayoutChangeListener, IFxInternalView {

    private lateinit var helper: FxBasisHelper
    private val clickHelper = FxClickHelper()
    private val locationHelper = FxLocationHelper()
    private val configHelper = FxViewConfigHelper()
    private var _viewHolder: FxViewHolder? = null

    private var _childFxView: View? = null
    override val childView: View?
        get() = _childFxView
    override val containerView: FrameLayout
        get() = this
    override val viewHolder: FxViewHolder?
        get() {
            if (_viewHolder == null) _viewHolder = FxViewHolder(this)
            return _viewHolder
        }

    @JvmSynthetic
    internal fun init(config: FxBasisHelper): FxDefaultContainerView {
        this.helper = config
        initView()
        return this
    }

    private fun initView() {
        _childFxView = inflateLayoutView() ?: inflateLayoutId()
        clickHelper.initConfig(helper)
        locationHelper.initConfig(helper)
        configHelper.initConfig(context, helper)
        checkNotNull(_childFxView) { "initFxView -> Error,check your layoutId or layoutView." }
        initLocation()
        updateDisplayMode()
        // 注意这句代码非常重要,可以避免某些情况下View被隐藏掉
        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun inflateLayoutView(): View? {
        val view = helper.layoutView ?: return null
        helper.fxLog?.d("fxView-->init, way:[layoutView]")
        val lp = view.layoutParams ?: LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        addView(view, lp)
        return view
    }

    private fun inflateLayoutId(): View? {
        if (helper.layoutId == INVALID_LAYOUT_ID) return null
        helper.fxLog?.d("fxView-->init, way:[layoutId]")
        val view = LayoutInflater.from(context).inflate(helper.layoutId, this, false)
        addView(view)
        return view
    }

    private fun initLocation() {
        val configImpl = helper.iFxConfigStorage
        val hasConfig = configImpl?.hasConfig() ?: false
        val lp = helper.layoutParams ?: LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
        )
        // 不存在历史坐标时,设置gravity,默认左上角
        if (!hasConfig) lp.gravity = helper.gravity.value
        layoutParams = lp

        // 获得浮窗的位置
        // 存在历史位置 || 根据配置去获取
        val (initX, initY) = if (hasConfig) {
            configImpl!!.getX() to configImpl.getY()
        } else {
            initDefaultXY()
        }
        if (initX != -1F) x = initX
        if (initY != -1F) y = initY
        helper.fxLog?.d("fxView->initLocation,isHasConfig-($hasConfig),defaultX-($initX),defaultY-($initY)")
    }

    private fun initDefaultXY(): Pair<Float, Float> {
        // 非辅助定位&&非默认位置,此时x,y不可信
        if (!helper.enableAssistLocation && !helper.gravity.isDefault()) {
            helper.fxLog?.e(
                "fxView--默认坐标可能初始化异常,如果显示位置异常,请检查您的gravity是否为默认配置，当前gravity:${helper.gravity}。\n" +
                    "如果您要配置gravity,建议您启用辅助定位setEnableAssistDirection(),此方法将更便于定位。",
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

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        var intercepted = false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initTouchDown(ev)
                helper.fxLog?.d("fxView---onInterceptTouchEvent-[down]")
            }

            MotionEvent.ACTION_MOVE -> {
                intercepted = configHelper.checkInterceptedEvent(ev)
                helper.fxLog?.d("fxView---onInterceptTouchEvent-[move], interceptedTouch-$intercepted")
            }
        }
        return intercepted
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        helper.iFxScrollListener?.eventIng(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> initTouchDown(event)
            MotionEvent.ACTION_MOVE -> touchToMove(event)
            MotionEvent.ACTION_POINTER_DOWN -> touchToPointerDown(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_POINTER_UP -> touchToPointerUp(event)
        }
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        helper.iFxViewLifecycle?.attach()
        (parent as? ViewGroup)?.addOnLayoutChangeListener(this)
        helper.fxLog?.d("fxView-lifecycle-> onAttachedToWindow")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        helper.iFxViewLifecycle?.detached()
        (parent as? ViewGroup)?.removeOnLayoutChangeListener(this)
        helper.fxLog?.d("fxView-lifecycle-> onDetachedFromWindow")
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        helper.iFxViewLifecycle?.windowsVisibility(visibility)
        helper.fxLog?.d("fxView-lifecycle-> onWindowVisibilityChanged")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        helper.fxLog?.d("fxView--lifecycle-> onConfigurationChanged--->")
        // use the configuration in Configuration first
        val isScreenChanged = locationHelper.updateConfig(newConfig)
        if (!isScreenChanged) return
        val x = x
        val y = y
        locationHelper.saveLocation(x, y, configHelper)
        helper.fxLog?.d("fxView--lifecycle-> saveLocation:[x:$x,y:$y]")
    }

    override fun setOnClickListener(l: OnClickListener?) {
        helper.iFxClickListener = l
        helper.enableClickListener = true
    }

    private fun initTouchDown(ev: MotionEvent) {
        if (configHelper.hasMainPointerId()) return
        clickHelper.initDown(x, y)
        configHelper.initTouchDown(ev)
        configHelper.updateWidgetSize(this)
        configHelper.updateBoundary(true)
        // init width and height boundary
        helper.iFxScrollListener?.down()
    }

    override fun moveLocation(x: Float, y: Float, useAnimation: Boolean) {
        val newX = configHelper.safeX(x)
        val newY = configHelper.safeY(y)
        if (useAnimation) {
            moveToLocation(newX, newY)
        } else {
            this.x = x
            this.y = y
        }
    }

    override fun moveLocationByVector(x: Float, y: Float, useAnimation: Boolean) {
        val currentX = this.x.plus(x)
        val currentY = this.y.plus(y)
        moveLocation(currentX, currentY, useAnimation)
    }

    override fun updateView() {
        removeView(_childFxView)
        _childFxView = if (helper.layoutView != null) {
            inflateLayoutView()
        } else {
            inflateLayoutId()
        }
    }

    override fun moveToEdge() {
        configHelper.updateBoundary(false)
        configHelper.getAdsorbDirectionLocation(x, y)?.let { (x, y) ->
            moveToLocation(x, y)
            saveLocationToStorage(x, y)
        }
    }

    @JvmSynthetic
    internal fun updateDisplayMode() {
        isClickable = helper.displayMode != FxDisplayMode.DisplayOnly
    }

    private fun moveToLocation(moveX: Float, moveY: Float) {
        if (moveX == x && moveY == y) return
        helper.fxLog?.d("fxView-->moveToEdge---x-($x)，y-($y) ->  moveX-($moveX),moveY-($moveY)")
        animate().x(moveX).y(moveY).setDuration(DEFAULT_MOVE_ANIMATOR_DURATION).start()
    }

    private fun saveLocationToStorage(moveX: Float, moveY: Float) {
        if (!helper.enableSaveDirection) return
        if (helper.iFxConfigStorage == null) {
            helper.fxLog?.e("fxView-->saveDirection---iFxConfigStorageImpl does not exist, save failed!")
            return
        }
        helper.iFxConfigStorage?.update(moveX, moveY)
        helper.fxLog?.d("fxView-->saveDirection---x-($moveX)，y-($moveY)")
    }

    private fun restoreLocation() {
        val (x, y) = locationHelper.getLocation(configHelper)
        this.x = x
        this.y = y
        saveLocationToStorage(x, y)
        helper.fxLog?.d("fxView--lifecycle-> restoreLocation:[x:$x,y:$y]")
    }

    private fun touchToMove(event: MotionEvent) {
        if (configHelper.hasMainPointerId() && helper.displayMode == FxDisplayMode.Normal) {
            updateLocation(event)
        }
    }

    private fun touchToPointerUp(event: MotionEvent) {
        if (configHelper.isCurrentPointerId(event)) {
            touchToCancel()
        } else {
            helper.fxLog?.d("fxView---onTouchEvent--ACTION_POINTER_UP---id:${event.pointerId}->")
        }
    }

    private fun touchToPointerDown(event: MotionEvent) {
        helper.fxLog?.d("fxView---onTouchEvent--touchToPointerDown--id:${event.getPointerId(event.actionIndex)}->")
        if (configHelper.hasMainPointerId()) return
        // Here you can realize the multi-finger cooperative pulling 😆
        if (event.x.withIn(0, width) && event.y.withIn(0, height)) {
            initTouchDown(event)
        }
    }

    private fun touchToCancel() {
        moveToEdge()
        helper.iFxScrollListener?.up()
        configHelper.touchDownId = INVALID_TOUCH_ID
        clickHelper.performClick(this)
        helper.fxLog?.d("fxView---onTouchEvent---MainTouchCancel->")
    }

    private fun refreshLocation(w: Int, h: Int) {
        if (!configHelper.updateWidgetSize(w, h, this)) return
        // 初始化位置时，我们进行一次位置校准，避免浮窗位置异常
        if (locationHelper.isInitLocation()) {
            checkOrFixLocation()
            return
        }
        if (locationHelper.isRestoreLocation()) {
            restoreLocation()
        } else {
            moveToEdge()
        }
    }

    private fun checkOrFixLocation() {
        val disX = configHelper.safeX(x)
        val disY = configHelper.safeY(y)
        moveToLocation(disX, disY)
    }

    private fun updateLocation(event: MotionEvent) {
        val disX = configHelper.safeX(x, event)
        val disY = configHelper.safeY(y, event)
        x = disX
        y = disY
        clickHelper.checkClickEvent(disX, disY)
        helper.iFxScrollListener?.dragIng(event, disX, disY)
        helper.fxLog?.v("fxView---scrollListener--drag-event--x($disX)-y($disY)")
    }

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        if (v == null) return
        refreshLocation(v.width, v.height)
    }
}
