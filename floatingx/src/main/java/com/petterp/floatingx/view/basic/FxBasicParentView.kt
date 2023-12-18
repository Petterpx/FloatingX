package com.petterp.floatingx.view.basic

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.util.INVALID_LAYOUT_ID
import com.petterp.floatingx.view.FxViewHolder
import com.petterp.floatingx.view.IFxInternalView

/**
 * @author petterp
 */
abstract class FxBasicParentView @JvmOverloads constructor(
    open val helper: FxBasisHelper,
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), IFxInternalView, View.OnLayoutChangeListener {
    private var isCreated = false
    private var _childView: View? = null
    private var _viewHolder: FxViewHolder? = null
    private val touchHelper = FxViewTouchHelper()
    private val locationHelper = FxLocationHelper()

    abstract fun onLayoutInit()
    abstract fun onTouchDown(event: MotionEvent)
    abstract fun onTouchMove(event: MotionEvent)
    abstract fun onTouchCancel(event: MotionEvent)
    abstract fun interceptTouchEvent(ev: MotionEvent): Boolean

    override val childView: View?
        get() = _childView
    override val containerView: FrameLayout
        get() = this
    override val viewHolder: FxViewHolder?
        get() {
            if (_viewHolder == null) _viewHolder = FxViewHolder(this)
            return _viewHolder
        }

    open fun initView() {
        touchHelper.initConfig(this)
        locationHelper.initConfig(helper)
    }

    override fun moveToEdge() {
    }

    override fun moveLocation(x: Float, y: Float, useAnimation: Boolean) {
    }

    override fun moveLocationByVector(x: Float, y: Float, useAnimation: Boolean) {
    }

    override fun updateView() {
        // 更新子view
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!isCreated) {
            isCreated = true
            onLayoutInit()
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (touchHelper.hasMainPointerId()) return super.onInterceptTouchEvent(event)
        return interceptTouchEvent(event) || super.onInterceptTouchEvent(event)
    }

    protected fun initChildView(): View? {
        _childView = inflateLayoutView() ?: inflateLayoutId()
        return _childView
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
    }
}
