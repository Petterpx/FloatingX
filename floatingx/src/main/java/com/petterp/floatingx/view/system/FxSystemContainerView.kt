package com.petterp.floatingx.view.system

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.util.INVALID_LAYOUT_ID
import com.petterp.floatingx.view.FxViewHolder
import com.petterp.floatingx.view.IFxInternalView

/** 基础悬浮窗View */
@SuppressLint("ViewConstructor")
class FxSystemContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs), IFxInternalView {

    private lateinit var helper: FxBasisHelper
    private lateinit var wl: WindowManager.LayoutParams
    private lateinit var wm: WindowManager
    private var _childFxView: View? = null
    private var _viewHolder: FxViewHolder? = null
    private var cX = 0
    private var cY = 0

    override val containerView: FrameLayout
        get() = this
    override val childView: View?
        get() = _childFxView
    override val viewHolder: FxViewHolder?
        get() {
            if (_viewHolder == null) _viewHolder = FxViewHolder(this)
            return _viewHolder
        }

    fun init(
        config: FxAppHelper
    ): FxSystemContainerView {
        this.helper = config
        initView()
        return this
    }

    private fun initView() {
        _childFxView = inflateLayoutView() ?: inflateLayoutId()
        wl = WindowManager.LayoutParams().apply {
            // 设置大小 自适应
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            format = PixelFormat.TRANSPARENT
            /**
             * 注意，flag的值可以为：
             * 下面的flags属性的效果形同“锁定”。
             * 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
             * LayoutParams.FLAG_NOT_TOUCH_MODAL 不影响后面的事件
             * LayoutParams.FLAG_NOT_FOCUSABLE 不可聚焦
             * LayoutParams.FLAG_NOT_TOUCHABLE 不可触摸
             */
            flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
        }
        wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.addView(this, wl)
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

    override fun moveLocation(x: Float, y: Float, useAnimation: Boolean) {
    }

    override fun moveLocationByVector(x: Float, y: Float, useAnimation: Boolean) {
    }

    override fun moveToEdge() {
    }

    override fun updateView() {
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                cX = event.rawX.toInt()
                cY = event.rawY.toInt()
            }

            MotionEvent.ACTION_MOVE -> {
                val nowX = event.rawX.toInt()
                val nowY = event.rawY.toInt()
                val movedX = nowX - cX
                val movedY = nowY - cY
                cX = nowX
                cY = nowY
                wl.apply {
                    x += movedX
                    y += movedY
                }
                // 更新悬浮球控件位置
                wm.updateViewLayout(this, wl)
            }

            else -> {
            }
        }
        return super.onTouchEvent(event)
    }
}
