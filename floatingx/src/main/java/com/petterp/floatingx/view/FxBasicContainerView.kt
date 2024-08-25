package com.petterp.floatingx.view

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.util.INVALID_LAYOUT_ID
import com.petterp.floatingx.util.safeAddView
import com.petterp.floatingx.view.helper.FxViewAnimationHelper
import com.petterp.floatingx.view.helper.FxViewLocationHelper
import com.petterp.floatingx.view.helper.FxViewTouchHelper

/**
 * Fx基础容器View
 * @author petterp
 */
abstract class FxBasicContainerView @JvmOverloads constructor(
    open val helper: FxBasisHelper,
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), IFxInternalHelper {
    private var isInitLayout = true
    private var _childView: View? = null
    private var _viewHolder: FxViewHolder? = null
    private val touchHelper = FxViewTouchHelper()
    private val animateHelper = FxViewAnimationHelper()
    internal val locationHelper = FxViewLocationHelper()
    private val helpers = listOf(locationHelper, touchHelper, animateHelper)

    abstract fun currentX(): Float
    abstract fun currentY(): Float
    abstract fun updateXY(x: Float, y: Float)
    abstract fun parentSize(): Pair<Int, Int>?

    abstract fun onTouchDown(event: MotionEvent)
    abstract fun onTouchMove(event: MotionEvent)
    abstract fun onTouchCancel(event: MotionEvent)
    open fun preCheckPointerDownTouch(event: MotionEvent): Boolean = true

    open fun onInitChildViewEnd(vh: FxViewHolder) {}

    override val childView: View? get() = _childView
    override val containerView: FrameLayout get() = this
    override val viewHolder: FxViewHolder? get() = _viewHolder


    open fun initView() {
        helpers.forEach { it.initConfig(this) }
        // 先隐藏view,等待初始化完成后再显示,避免位置的影响
        visibility = View.INVISIBLE
    }

    override fun moveToEdge() {
        val (x, y) = locationHelper.getDefaultEdgeXY() ?: return
        moveLocation(x, y, true)
    }

    override fun moveLocation(x: Float, y: Float, useAnimation: Boolean) {
        // 需要考虑状态栏的影响
        safeMoveToXY(x, y, useAnimation)
    }

    override fun moveLocationByVector(x: Float, y: Float, useAnimation: Boolean) {
        safeMoveToXY(x + currentX(), y + currentY(), useAnimation)
    }

    override fun checkPointerDownTouch(id: Int, event: MotionEvent): Boolean {
        val view = findViewById<View>(id) ?: return false
        return checkPointerDownTouch(view, event)
    }

    override fun checkPointerDownTouch(view: View, event: MotionEvent): Boolean {
        val x = event.rawX
        val y = event.rawY
        val location = IntArray(2)
        getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height
        return x >= left && x <= right && y >= top && y <= bottom
    }

    override fun updateView(layoutId: Int) {
        helper.fxLog.d("fxView -> updateView")
        locationHelper.needUpdateLocation()
        removeView(_childView)
        installChildView()
    }

    override fun updateView(layoutView: View) {
        helper.fxLog.d("fxView -> updateView")
        locationHelper.needUpdateLocation()
        removeView(_childView)
        installChildView()
    }

    override fun invokeClick() {
        helper.iFxClickListener?.onClick(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        helpers.forEach { it.onSizeChanged(w, h, oldw, oldh) }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!isInitLayout) return
        isInitLayout = false
        helpers.forEach { it.onInit() }
        visibility = View.VISIBLE
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val isIntercept = helper.iFxTouchListener?.onInterceptTouchEvent(event, this) ?: true
        if (!isIntercept) return false
        return touchHelper.interceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return touchHelper.touchEvent(event, this) || super.onTouchEvent(event)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        helper.iFxViewLifecycles.forEach {
            it.windowsVisibility(visibility)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig ?: return)
        helpers.forEach { it.onConfigurationChanged(newConfig) }
    }

    protected fun safeUpdatingXY(x: Float, y: Float) {
        val safeX = locationHelper.safeX(x, true)
        val safeY = locationHelper.safeY(y, true)
        updateXY(safeX, safeY)
    }

    protected fun installChildView(): View? {
        _childView = inflateLayoutView() ?: inflateLayoutId()
        if (_childView != null) {
            _viewHolder = FxViewHolder(_childView!!)
            onInitChildViewEnd(_viewHolder!!)
            helper.iFxViewLifecycles.forEach { listener ->
                listener.initView(_viewHolder!!)
            }
        }
        return _childView
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        helper.iFxViewLifecycles.forEach {
            it.attach(this)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        helper.iFxViewLifecycles.forEach {
            it.detached(this)
        }
    }

    private fun inflateLayoutView(): View? {
        val view = helper.layoutView ?: return null
        helper.fxLog.d("fxView -> init, way:[layoutView]")
        val lp = view.layoutParams ?: LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        safeAddView(view, lp)
        return view
    }

    private fun inflateLayoutId(): View? {
        if (helper.layoutId == INVALID_LAYOUT_ID) return null
        helper.fxLog.d("fxView -> init, way:[layoutId]")
        val view = LayoutInflater.from(context).inflate(helper.layoutId, this, false)
        safeAddView(view)
        return view
    }

    private fun safeMoveToXY(x: Float, y: Float, useAnimation: Boolean) {
        val endX = locationHelper.safeX(x)
        val endY = locationHelper.safeY(y)
        internalMoveToXY(endX, endY, useAnimation)
    }

    internal fun internalMoveToXY(endX: Float, endY: Float, useAnimation: Boolean = false) {
        val curX = currentX()
        val curY = currentY()
        if (curX == endX && curY == endY) return
        if (useAnimation) {
            animateHelper.start(endX, endY)
        } else {
            updateXY(endX, endY)
        }
        locationHelper.checkOrSaveLocation(endX, endY)
        helper.fxLog.d("fxView -> moveToXY: start($curX,$curY),end($endX,$endY)")
    }

    internal fun preCancelAction() {
        helpers.forEach { it.onPreCancel() }
    }
}
