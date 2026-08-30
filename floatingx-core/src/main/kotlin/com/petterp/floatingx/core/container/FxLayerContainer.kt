package com.petterp.floatingx.core.container

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.core.layout.FxPoint
import com.petterp.floatingx.core.layout.FxSize

/**
 * Layer 容器（spec §2.1）：一个 match_parent 的透明覆盖层，内容子 view 在其中用 translation 定位。
 * - 移动只改 translation，父层永不 re-layout（修 #240）
 * - 落在内容之外的 DOWN 直接返回 false 透传给下层（修 #243）；modal 时则消费（#212）
 * - clipChildren=false 允许内容随 FxOverflow 超出边界（#235）
 */
public class FxLayerContainer(context: Context) : FrameLayout(context), FxContainer {

    override val view: ViewGroup get() = this
    override var contentView: View? = null
        private set
    override val isLtr: Boolean get() = layoutDirection != View.LAYOUT_DIRECTION_RTL
    override val isLayer: Boolean get() = true
    override var touchHandler: FxContainerTouchHandler? = null
    override var onContentSizeChanged: ((FxSize) -> Unit)? = null
    override var onBoundsChanged: (() -> Unit)? = null

    /** true 时拦截内容之外的触摸（ModalScrimFeature 设置） */
    public var modal: Boolean = false
    public var onOutsideTouch: (() -> Unit)? = null

    private val screenLocation = IntArray(2)
    private var lastW = 0
    private var lastH = 0
    private var consumingOutside = false

    private val contentLayoutListener = View.OnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
        val w = r - l
        val h = b - t
        if (w != lastW || h != lastH) {
            lastW = w
            lastH = h
            onContentSizeChanged?.invoke(FxSize(w.toFloat(), h.toFloat()))
        }
    }

    init {
        setWillNotDraw(true)
        isClickable = false
        isFocusable = false
        clipChildren = false
        clipToPadding = false
    }

    override fun setContent(view: View) {
        contentView?.let {
            it.removeOnLayoutChangeListener(contentLayoutListener)
            removeView(it)
        }
        (view.parent as? ViewGroup)?.removeView(view)
        val lp = (view.layoutParams as? LayoutParams)
            ?: LayoutParams(view.layoutParams?.width ?: LayoutParams.WRAP_CONTENT, view.layoutParams?.height ?: LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.TOP or Gravity.START
        lastW = 0
        lastH = 0
        addView(view, lp)
        view.addOnLayoutChangeListener(contentLayoutListener)
        contentView = view
    }

    override fun releaseContent() {
        val c = contentView ?: return
        c.removeOnLayoutChangeListener(contentLayoutListener)
        removeView(c)
        contentView = null
        lastW = 0
        lastH = 0
    }

    override fun contentSize(): FxSize =
        contentView?.let { FxSize(it.width.toFloat(), it.height.toFloat()) } ?: FxSize.EMPTY

    override fun setContentPosition(x: Float, y: Float) {
        val c = contentView ?: return
        c.translationX = x
        c.translationY = y
    }

    override fun contentPosition(): FxPoint =
        contentView?.let { FxPoint(it.translationX, it.translationY) } ?: FxPoint.ZERO

    override fun contentPositionOnScreen(): FxPoint {
        getLocationOnScreen(screenLocation)
        val c = contentView ?: return FxPoint(screenLocation[0].toFloat(), screenLocation[1].toFloat())
        return FxPoint(screenLocation[0] + c.translationX, screenLocation[1] + c.translationY)
    }

    override fun setContentVisible(visible: Boolean) {
        contentView?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    override fun hitTest(x: Float, y: Float): Boolean {
        val c = contentView ?: return false
        if (c.visibility != View.VISIBLE) return false
        val left = c.translationX
        val top = c.translationY
        return x >= left && x < left + c.width && y >= top && y < top + c.height
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        // 新的一次按下一定是新手势：上一轮若丢了 UP/CANCEL（父层拦截等），别让标志位卡死
        if (action == MotionEvent.ACTION_DOWN) consumingOutside = false
        if (consumingOutside) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) consumingOutside = false
            return true
        }
        if (action == MotionEvent.ACTION_DOWN && !hitTest(ev.x, ev.y)) {
            if (modal) {
                consumingOutside = true
                onOutsideTouch?.invoke()
                return true
            }
            return false
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = touchHandler?.onIntercept(ev) ?: false

    override fun onTouchEvent(ev: MotionEvent): Boolean = touchHandler?.onTouch(ev) ?: false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) onBoundsChanged?.invoke()
    }
}
