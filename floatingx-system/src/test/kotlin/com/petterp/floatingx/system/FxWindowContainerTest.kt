package com.petterp.floatingx.system

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.container.FxContainerTouchHandler
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxInsets
import com.petterp.floatingx.core.layout.FxSize
import com.petterp.floatingx.system.container.FxWindowContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowWindowManagerImpl

@RunWith(RobolectricTestRunner::class)
class FxWindowContainerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private fun lp() = WindowManager.LayoutParams().apply {
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
    }

    private fun container(back: SystemBackListener? = null): FxWindowContainer = FxWindowContainer(context, wm, lp(), back).also { c ->
        c.setContent(View(context).apply { layoutParams = ViewGroup.LayoutParams(100, 50) })
        measureAndLayout(c)
    }

    private fun measureAndLayout(c: FxWindowContainer) {
        c.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        c.layout(0, 0, c.measuredWidth, c.measuredHeight)
    }

    @Test
    fun `is not a layer and wraps content`() {
        val c = container()
        assertFalse(c.isLayer)
        assertEquals(100f, c.contentSize().width, 0f)
        assertEquals(50f, c.contentSize().height, 0f)
    }

    @Test
    fun `window stays gone until first layout is applied`() {
        val c = container()
        c.setContentVisible(true)
        assertEquals(View.GONE, c.visibility)
        c.setBounds(1080, 1920)
        c.applyLayout(10f, 20f, FxGravity.TOP_START, ltr = true)
        assertEquals(View.VISIBLE, c.visibility)
        c.setContentVisible(false)
        assertEquals(View.GONE, c.visibility)
        assertEquals(View.INVISIBLE, c.contentView!!.visibility)
    }

    @Test
    fun `applyLayout writes layout params and updates the window when attached`() {
        val c = container()
        c.setBounds(1080, 1920)
        wm.addView(c, c.windowParams)
        c.isAttachedToWm = true
        c.applyLayout(1080f - 100f - 20f, 30f, FxGravity.TOP_END, ltr = true)
        assertEquals(20, c.windowParams.x)
        assertEquals(30, c.windowParams.y)
        assertEquals(android.view.Gravity.TOP or android.view.Gravity.RIGHT, c.windowParams.gravity)
        assertEquals(960f, c.contentPosition().x, 0f)
        assertEquals(960f, c.contentPositionOnScreen().x, 0f)
        // shadowOf(WindowManager) 的静态返回类型是基类 ShadowWindowManager，getViews() 在 Impl 上
        assertTrue((shadowOf(wm) as ShadowWindowManagerImpl).views.contains(c))
    }

    @Test
    fun `setContentPosition keeps the last gravity`() {
        val c = container()
        c.setBounds(1080, 1920)
        c.applyLayout(0f, 0f, FxGravity.BOTTOM_END, ltr = true)
        c.setContentPosition(980f, 1870f)
        assertEquals(android.view.Gravity.BOTTOM or android.view.Gravity.RIGHT, c.windowParams.gravity)
        assertEquals(0, c.windowParams.x); assertEquals(0, c.windowParams.y)
    }

    @Test
    fun `hitTest is bounded by the content`() {
        val c = container()
        c.setBounds(1080, 1920)
        c.applyLayout(0f, 0f, FxGravity.TOP_START, ltr = true)
        c.setContentVisible(true)
        assertTrue(c.hitTest(50f, 25f))
        assertFalse(c.hitTest(150f, 25f))
        c.setContentVisible(false)
        assertFalse(c.hitTest(50f, 25f))
    }

    @Test
    fun `focusable and touchable toggles flip the window flags`() {
        val c = container()
        c.setWindowFocusable(true)
        assertEquals(0, c.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        c.setWindowFocusable(false)
        assertTrue(c.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        c.setWindowTouchable(false)
        assertTrue(c.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        c.setWindowTouchable(true)
        assertEquals(0, c.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    @Test
    fun `back key goes to the listener`() {
        var hits = 0
        val c = container(SystemBackListener { hits++; true })
        val consumed = c.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        assertTrue(consumed)
        assertEquals(1, hits)
    }

    @Test
    fun `releaseContent removes the content view`() {
        val c = container()
        c.releaseContent()
        assertEquals(0, c.childCount)
        assertEquals(null, c.contentView)
    }

    @Test
    fun `touch events are forwarded to the touch handler`() {
        val c = container()
        val intercepted = mutableListOf<Int>()
        val touched = mutableListOf<Int>()
        c.touchHandler = object : FxContainerTouchHandler {
            override fun onIntercept(ev: MotionEvent): Boolean {
                intercepted += ev.actionMasked
                return false
            }

            override fun onTouch(ev: MotionEvent): Boolean {
                touched += ev.actionMasked
                return true
            }
        }
        val down = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        assertFalse(c.onInterceptTouchEvent(down))
        assertTrue(c.onTouchEvent(down))
        down.recycle()
        assertEquals(listOf(MotionEvent.ACTION_DOWN), intercepted)
        assertEquals(listOf(MotionEvent.ACTION_DOWN), touched)
    }

    @Test
    fun `content size is reported once per real change`() {
        val c = FxWindowContainer(context, wm, lp(), null)
        val sizes = mutableListOf<FxSize>()
        c.onContentSizeChanged = { sizes += it }
        c.setContent(View(context).apply { layoutParams = ViewGroup.LayoutParams(100, 50) })
        measureAndLayout(c)
        assertEquals(listOf(FxSize(100f, 50f)), sizes)
        // 同尺寸重新布局（内容自己 requestLayout）不该再回调
        c.contentView!!.requestLayout()
        measureAndLayout(c)
        assertEquals(1, sizes.size)
    }

    @Test
    fun `configuration change refreshes the screen size and notifies`() {
        val c = container()
        var hits = 0
        c.onBoundsChanged = { hits++ }
        c.setBounds(1, 1)
        c.dispatchConfigurationChanged(context.resources.configuration)
        assertEquals(1, hits)
        // 旋转后容器必须先刷新自己的屏幕尺寸，否则 host 拿到的是旋转前的旧值
        assertEquals(context.resources.displayMetrics.widthPixels, c.boundsWidth)
        assertEquals(context.resources.displayMetrics.heightPixels, c.boundsHeight)
    }

    /**
     * insets 是屏幕级的：由 refreshBounds 从 WindowMetrics（sdk 35）读，与窗口自身位置无关。
     * Robolectric 的默认 WindowMetrics 不带系统栏，所以这里的期望值是 NONE；
     * 关键是这条路径不抛异常、且每次刷新都会给 windowInsets 赋一个确定的值。
     */
    @Test
    fun `refreshBounds fills the screen level insets`() {
        val c = container()
        c.refreshBounds()
        assertEquals(FxInsets.NONE, c.windowInsets)
        assertEquals(context.resources.displayMetrics.widthPixels, c.boundsWidth)
        assertEquals(context.resources.displayMetrics.heightPixels, c.boundsHeight)
    }

    /**
     * 回归：**窗口自身的** onApplyWindowInsets 不能再驱动 onBoundsChanged。
     * wrap_content 窗口拿到的 insets 是与自身 frame 相交的结果，浮窗拖到状态栏边缘时会突然变化，
     * 每帧派发 onBoundsChanged 会让 core 清掉 dragInput，拖动直接卡死在屏幕边缘。
     */
    @Test
    fun `dispatching window insets no longer notifies bounds changed`() {
        val c = container()
        var hits = 0
        c.onBoundsChanged = { hits++ }
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 63, 0, 126))
            .build()
        ViewCompat.dispatchApplyWindowInsets(c, insets)
        assertEquals(0, hits)
        assertEquals(FxInsets.NONE, c.windowInsets)
    }

    /**
     * 直接加到 WindowManager 上的 view 没有父级可继承布局方向，默认恒为 LTR，
     * RTL 语言下 END 会解析成右边——所以容器必须显式跟随 Configuration。
     *
     * 两个 Robolectric 细节：
     * - 只写 `ldrtl` 不够，会被 locale（默认 en-rUS）改回 ldltr，必须给一个 RTL locale；
     * - `View.resolveLayoutDirection()` 受 `ApplicationInfo.FLAG_SUPPORTS_RTL` 闸门控制，
     *   不声明 `android:supportsRtl="true"` 的应用整体按 LTR 解析（这也是真实行为），
     *   所以这里补上该标志，模拟一个支持 RTL 的宿主应用。
     */
    @Test
    @Config(qualifiers = "ar-rXB")
    fun `layout direction follows the configuration in rtl`() {
        context.applicationInfo.flags = context.applicationInfo.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
        val c = container()
        assertFalse(c.isLtr)
        c.setBounds(1080, 1920)
        c.applyLayout(10f, 20f, FxGravity.TOP_END, c.isLtr)
        assertEquals(Gravity.TOP or Gravity.LEFT, c.windowParams.gravity)
    }

    /**
     * 一次返回键 = DOWN + UP。DOWN 在 pre-IME 阶段被 onImeBack 吃掉（收键盘）后，
     * 配套的 UP 必须也被吞掉，否则同一次返回还会再触发 backListener（通常是关浮窗）。只吞一个。
     */
    @Test
    fun `back consumed by the ime callback swallows exactly one up`() {
        var hits = 0
        val c = container(SystemBackListener { hits++; true })
        c.onImeBack = {}
        assertTrue(c.dispatchKeyEventPreIme(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)))
        assertTrue(c.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)))
        assertEquals(0, hits)

        // 下一次返回键（键盘已收起，onImeBack 不再消费）必须正常交给业务
        c.onImeBack = null
        c.dispatchKeyEventPreIme(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        assertTrue(c.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)))
        assertEquals(1, hits)
    }
}
