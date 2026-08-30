package com.petterp.floatingx.system.container

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxContainerTouchHandler
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxInsets
import com.petterp.floatingx.core.layout.FxPoint
import com.petterp.floatingx.core.layout.FxSize
import com.petterp.floatingx.system.SystemBackListener

/**
 * Window 容器（spec §2.1 / §4）：容器本身就是一个 wrap_content 的 WindowManager 窗口，
 * "移动内容" = 改 LayoutParams.x/y + updateViewLayout。
 * - 首次 applyLayout 之前窗口 GONE（不闪现在 0,0，也不挡触摸）
 * - 隐藏 = 内容 INVISIBLE + 窗口 GONE（窗口收起后不再拦截其下的触摸）
 * - 锚点 gravity 映射到 LayoutParams.gravity，内容尺寸变化时锚定边不动（#187）
 */
public class FxWindowContainer(
    context: Context,
    private val wm: WindowManager,
    public val windowParams: WindowManager.LayoutParams,
    private val backListener: SystemBackListener?,
) : FrameLayout(context), FxContainer {

    override val view: ViewGroup get() = this
    override var contentView: View? = null
        private set
    override val isLtr: Boolean get() = layoutDirection != View.LAYOUT_DIRECTION_RTL
    override val isLayer: Boolean get() = false
    override var touchHandler: FxContainerTouchHandler? = null
    override var onContentSizeChanged: ((FxSize) -> Unit)? = null
    override var onBoundsChanged: (() -> Unit)? = null

    /** host 在 addView/removeView 时维护；只有挂在 WindowManager 上才能 updateViewLayout */
    public var isAttachedToWm: Boolean = false

    /** 键盘弹出期间按返回键（IME 之前收到）：KeyboardFeature 用它收起键盘并恢复不可聚焦 */
    public var onImeBack: (() -> Unit)? = null

    /** 最近一次 onApplyWindowInsets 的 systemBars ∪ displayCutout */
    public var windowInsets: FxInsets = FxInsets.NONE
        private set

    /** 当前用于 gravity 换算的屏幕宽高（[refreshBounds] / [setBounds] 写入） */
    public val boundsWidth: Int get() = boundsW
    public val boundsHeight: Int get() = boundsH

    private var boundsW = 0
    private var boundsH = 0

    /** getRealSize 的复用出参，旋转/insets 每次刷新都不再分配 */
    private val screenPoint = Point()
    private var posX = 0f
    private var posY = 0f
    private var lastGravity = FxGravity.TOP_START
    private var positioned = false
    private var contentVisibleRequested = false
    private var lastW = 0
    private var lastH = 0

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
        visibility = View.GONE
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val i = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val next = FxInsets(i.left.toFloat(), i.top.toFloat(), i.right.toFloat(), i.bottom.toFloat())
            if (next != windowInsets) {
                windowInsets = next
                // 派发前先刷新屏幕尺寸：insets 变化常伴随可用区变化，host 读到的必须是新值
                refreshBounds()
                onBoundsChanged?.invoke()
            }
            insets
        }
    }

    // ---------- host 侧 API ----------

    /** 屏幕尺寸，gravity 换算需要；对外只暴露 [refreshBounds]，直接写值仅供单测构造确定的屏幕尺寸 */
    @VisibleForTesting
    internal fun setBounds(width: Int, height: Int) {
        boundsW = width
        boundsH = height
    }

    /**
     * 从 WindowManager 读一次真实屏幕尺寸并缓存。
     * 旋转 / insets 变化时容器**自己**先刷新再派发 onBoundsChanged：
     * 否则 host 与 WindowLayoutMath 会拿旋转前的旧尺寸换算非 TOP_START 锚点，位置直接跳错。
     */
    public fun refreshBounds() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            setBounds(b.width(), b.height())
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealSize(screenPoint)
            setBounds(screenPoint.x, screenPoint.y)
        }
    }

    /** 提交一次布局：左上角屏幕坐标 + 锚点 gravity（SystemHost.updateLayout 调用） */
    public fun applyLayout(x: Float, y: Float, gravity: FxGravity, ltr: Boolean) {
        posX = x
        posY = y
        lastGravity = gravity
        val c = contentView
        WindowLayoutMath.apply(windowParams, x, y, gravity, ltr, boundsW, boundsH, c?.width ?: 0, c?.height ?: 0)
        if (!positioned) {
            positioned = true
            if (contentVisibleRequested) visibility = View.VISIBLE
        }
        if (isAttachedToWm) wm.updateViewLayout(this, windowParams)
    }

    /** 切换窗口是否可聚焦（KeyboardFeature 唤起键盘时需要焦点） */
    public fun setWindowFocusable(focusable: Boolean) {
        updateFlag(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, !focusable)
    }

    /** touchable=false → FLAG_NOT_TOUCHABLE，触摸全部透传给下层（spec §4） */
    public fun setWindowTouchable(touchable: Boolean) {
        updateFlag(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, !touchable)
    }

    private fun updateFlag(flag: Int, enabled: Boolean) {
        val next = if (enabled) windowParams.flags or flag else windowParams.flags and flag.inv()
        if (next == windowParams.flags) return
        windowParams.flags = next
        if (isAttachedToWm) wm.updateViewLayout(this, windowParams)
    }

    // ---------- FxContainer ----------

    override fun setContent(view: View) {
        releaseContent()
        (view.parent as? ViewGroup)?.removeView(view)
        val lp = (view.layoutParams as? LayoutParams)
            ?: LayoutParams(view.layoutParams?.width ?: LayoutParams.WRAP_CONTENT, view.layoutParams?.height ?: LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.TOP or Gravity.START
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

    override fun contentSize(): FxSize = contentView?.let { FxSize(it.width.toFloat(), it.height.toFloat()) } ?: FxSize.EMPTY

    override fun setContentPosition(x: Float, y: Float) {
        applyLayout(x, y, lastGravity, isLtr)
    }

    override fun contentPosition(): FxPoint = FxPoint(posX, posY)

    /** FLAG_LAYOUT_IN_SCREEN + FLAG_LAYOUT_NO_LIMITS 下 LayoutParams 坐标就是屏幕坐标 */
    override fun contentPositionOnScreen(): FxPoint = FxPoint(posX, posY)

    override fun setContentVisible(visible: Boolean) {
        contentVisibleRequested = visible
        contentView?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        visibility = if (visible && positioned) View.VISIBLE else View.GONE
    }

    override fun hitTest(x: Float, y: Float): Boolean {
        val c = contentView ?: return false
        if (c.visibility != View.VISIBLE) return false
        return x >= 0f && x < c.width && y >= 0f && y < c.height
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = touchHandler?.onIntercept(ev) ?: false

    override fun onTouchEvent(ev: MotionEvent): Boolean = touchHandler?.onTouch(ev) ?: false

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            if (backListener?.onBackPressed() == true) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            val cb = onImeBack
            if (cb != null) {
                cb()
                return true
            }
        }
        return super.dispatchKeyEventPreIme(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        // 旋转/分屏：先把自己的屏幕尺寸换成新的，再让 core 重新定位
        refreshBounds()
        onBoundsChanged?.invoke()
    }
}
