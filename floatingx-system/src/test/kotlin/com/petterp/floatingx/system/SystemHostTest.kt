package com.petterp.floatingx.system

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.gesture.FxDrag
import com.petterp.floatingx.core.gesture.FxGesture
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxRect
import com.petterp.floatingx.system.container.FxWindowContainer
import com.petterp.floatingx.system.permission.FxPermission
import com.petterp.floatingx.system.permission.FxPermissionActivity
import com.petterp.floatingx.system.permission.FxPermissionRequest
import com.petterp.floatingx.system.permission.FxPermissionStrategy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowWindowManagerImpl

/** 屏幕定死 1080x1920（Robolectric 默认只有 320x470，装不下 moveTo 的目标坐标） */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1080dp-h1920dp")
class SystemHostTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /** 降级目标：挂在一个 FrameLayout 上的最小 Layer host */
    private class LayerHost(private val parent: FrameLayout) : FxHost {
        override val context: Context get() = parent.context
        override fun bind(session: FxHostSession) = session.onHostReady()
        override fun createContainer(): FxContainer = FxLayerContainer(parent.context)
        override fun attach(container: FxContainer) { parent.addView(container.view) }
        override fun detach(container: FxContainer) { parent.removeView(container.view) }
        override fun bounds(): FxBounds = FxBounds(FxRect(0f, 0f, parent.width.toFloat(), parent.height.toFloat()))
        override fun release() {}
    }

    private fun content(): FxContent = FxContent.provider { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }
    private fun config(gesture: FxGesture = FxGesture.Normal): FxConfig = FxConfig.builder(content()).anchor(FxGravity.TOP_START).gesture(gesture).build()
    private fun install(host: SystemHost, config: FxConfig = config()): FxControl = FloatingX.install("sys", config, host)
    private fun window(control: FxControl): FxWindowContainer = control.contentView!!.parent as FxWindowContainer

    /** shadowOf(WindowManager) 的静态返回类型是基类 ShadowWindowManager，getViews() 在 Impl 上 */
    private fun windowViews(): List<View> = (shadowOf(wm) as ShadowWindowManagerImpl).views

    /** 手动测量 + 布局，触发 onContentSizeChanged（Robolectric 不会自动布局窗口） */
    private fun layout(w: FxWindowContainer) {
        w.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        w.layout(0, 0, w.measuredWidth, w.measuredHeight)
    }

    /**
     * 模拟"窗口跟着手指走"的一次真实拖动：raw（屏幕）坐标每步前进 [dx]/[dy]，
     * 而相对坐标恒为落点 ([downX], [downY])——系统窗口拖动时就是这样，
     * 所以 core 只能靠 rawX/rawY 算增量（见 FxGestureDetector.absX）。
     *
     * 事件走容器的 `dispatchTouchEvent`（FrameLayout 的正常派发路径：内容 view 不消费触摸，
     * DOWN 先经 onInterceptTouchEvent，随后落到容器自己的 onTouchEvent；
     * 后续 MOVE/UP 因为没有 touch target 直接进 onTouchEvent）。
     */
    private fun drag(w: FxWindowContainer, downX: Float, downY: Float, dx: Float, dy: Float, steps: Int) {
        var rawX = w.contentPosition().x + downX
        var rawY = w.contentPosition().y + downY
        send(w, 0L, MotionEvent.ACTION_DOWN, rawX, rawY)
        for (i in 1..steps) {
            rawX += dx
            rawY += dy
            send(w, i.toLong(), MotionEvent.ACTION_MOVE, rawX, rawY)
        }
        send(w, steps + 1L, MotionEvent.ACTION_UP, rawX, rawY)
    }

    /**
     * 用**当前**窗口位置把屏幕坐标换算成容器相对坐标：`obtain` 出来的事件 raw == x，
     * 再 `offsetLocation` 只移动 x/y、不动 rawX/rawY，正好还原真实事件的形态。
     * 窗口原点用 contentPosition() 而不是 windowParams.x/y——后者在 BOTTOM/RIGHT gravity 下是到对边的距离。
     */
    private fun send(w: FxWindowContainer, eventTime: Long, action: Int, rawX: Float, rawY: Float) {
        val origin = w.contentPosition()
        val ev = MotionEvent.obtain(0L, eventTime, action, rawX, rawY, 0)
        ev.offsetLocation(-origin.x, -origin.y)
        w.dispatchTouchEvent(ev)
        ev.recycle()
    }

    @After
    fun tearDown() = FloatingX.uninstallAll()

    @Test
    fun `granted permission mounts a window`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = install(SystemHost.builder(app).build())
        control.show()
        val w = window(control)
        assertEquals(FxState.SHOWN, control.state)
        assertTrue(windowViews().contains(w))
        assertTrue(w.isAttachedToWm)
        control.cancel()
        assertFalse(windowViews().contains(w))
    }

    @Test
    @Suppress("DEPRECATION") // TYPE_PHONE 只在 O 以下走到
    fun `default layout params`() {
        ShadowSettings.setCanDrawOverlays(true)
        val host = SystemHost.builder(app).build()
        install(host)
        val lp = host.windowLayoutParams
        val expectedType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        assertEquals(expectedType, lp.type)
        assertEquals(WindowManager.LayoutParams.WRAP_CONTENT, lp.width)
        assertEquals(WindowManager.LayoutParams.WRAP_CONTENT, lp.height)
        assertEquals(PixelFormat.TRANSLUCENT, lp.format)
        val required = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        assertEquals(required, lp.flags and required)
    }

    @Test
    @Suppress("DEPRECATION") // SOFT_INPUT_ADJUST_RESIZE 只是随便挑一个可覆写字段
    fun `customizer runs last and windowLayoutParams is a snapshot`() {
        ShadowSettings.setCanDrawOverlays(true)
        val host = SystemHost.builder(app)
            .layoutParams { it.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY; it.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE }
            .build()
        val control = install(host)
        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, window(control).windowParams.type)
        val snapshot = host.windowLayoutParams
        assertNotSame(window(control).windowParams, snapshot)
        snapshot.x = 999
        assertEquals(0, window(control).windowParams.x)
    }

    @Test
    fun `skip strategy never checks permission`() {
        ShadowSettings.setCanDrawOverlays(false)
        val control = install(SystemHost.builder(app).permission(FxPermissionStrategy.skip()).build())
        control.show()
        assertEquals(FxState.SHOWN, control.state)
        assertEquals(null, shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `auto strategy requests and mounts once granted`() {
        ShadowSettings.setCanDrawOverlays(false)
        val host = SystemHost.builder(app).build()
        val control = install(host)
        control.show()
        assertEquals(FxState.INSTALLED, control.state)
        val launch = shadowOf(app).nextStartedActivity
        assertEquals(FxPermissionActivity::class.java.name, launch!!.component!!.className)
        // 模拟用户在设置页授权后返回
        ShadowSettings.setCanDrawOverlays(true)
        val id = launch.getIntExtra("fx_request_id", 0)
        FxPermission.dispatch(id, true)
        assertEquals(FxState.SHOWN, control.state)
    }

    @Test
    fun `denied with fallback swaps to the fallback host`() {
        ShadowSettings.setCanDrawOverlays(false)
        val parent = FrameLayout(app)
        val fallback = LayerHost(parent)
        val control = install(SystemHost.builder(app).fallback(fallback).build())
        control.show()
        val launch = shadowOf(app).nextStartedActivity!!
        FxPermission.dispatch(launch.getIntExtra("fx_request_id", 0), false)
        assertSame(fallback, control.host)
        assertEquals(FxState.SHOWN, control.state)
        assertEquals(1, parent.childCount)
    }

    @Test
    fun `denied without fallback stays installed and retryPermission recovers`() {
        ShadowSettings.setCanDrawOverlays(false)
        val host = SystemHost.builder(app).build()
        val control = install(host)
        control.show()
        val launch = shadowOf(app).nextStartedActivity!!
        FxPermission.dispatch(launch.getIntExtra("fx_request_id", 0), false)
        assertEquals(FxState.INSTALLED, control.state)
        ShadowSettings.setCanDrawOverlays(true)
        host.retryPermission()
        assertEquals(FxState.SHOWN, control.state)
    }

    /**
     * addView 抛异常（权限被撤销 / type 不被允许）后 retryPermission 要能救回来。
     * Robolectric 的影子 WindowManager 不会为任何 type 抛 BadToken/SecurityException，
     * 所以这里手工还原"挂载失败后"的现场：窗口从 WindowManager 摘掉 + isAttachedToWm=false，
     * 而 core 仍停在 SHOWN（真实场景就是这样：attach 吞掉异常，engine 照常进 SHOWN）。
     */
    @Test
    fun `retryPermission remounts a window whose attach failed`() {
        ShadowSettings.setCanDrawOverlays(true)
        val host = SystemHost.builder(app).build()
        val control = install(host)
        control.show()
        val w = window(control)
        wm.removeViewImmediate(w)
        w.isAttachedToWm = false
        assertFalse(windowViews().contains(w))

        host.retryPermission()
        assertTrue(windowViews().contains(w))
        assertTrue(w.isAttachedToWm)
        assertEquals(FxState.SHOWN, control.state)
    }

    /**
     * addView 抛 IllegalStateException（"已经加过了"）说明窗口其实还在屏幕上：
     * 必须记成已挂载，否则 detach 会直接 return，窗口再也摘不掉。
     */
    @Test
    fun `attach on an already added window keeps it marked as attached`() {
        ShadowSettings.setCanDrawOverlays(true)
        val host = SystemHost.builder(app).build()
        val control = install(host)
        control.show()
        val w = window(control)

        host.attach(w) // 重复 addView 抛 IllegalStateException，必须被吞掉
        assertTrue(w.isAttachedToWm)
        // 只看状态位：Robolectric 的影子 WindowManager 在真实 addView 抛异常之前就把 view 记进列表了，
        // 于是它的 views 里会重复一份，assert 列表反而测不出东西
        host.detach(w)
        assertFalse(w.isAttachedToWm)
    }

    /** 窗口被外部（WMS 清理 / 宿主自己 removeView）摘掉后，cancel 里的 removeViewImmediate 会抛 IAE，必须吞掉 */
    @Test
    fun `detach survives a window that is no longer attached to the window manager`() {
        ShadowSettings.setCanDrawOverlays(true)
        val host = SystemHost.builder(app).build()
        val control = install(host)
        control.show()
        val w = window(control)
        wm.removeViewImmediate(w)
        assertTrue(w.isAttachedToWm) // host 还以为挂着

        control.cancel()
        assertFalse(w.isAttachedToWm)
    }

    @Test
    fun `manual strategy hands the decision to the interceptor`() {
        ShadowSettings.setCanDrawOverlays(false)
        var request: FxPermissionRequest? = null
        val parent = FrameLayout(app)
        val fallback = LayerHost(parent)
        val control = install(SystemHost.builder(app).permission(FxPermissionStrategy.manual { request = it }).fallback(fallback).build())
        control.show()
        assertEquals(FxState.INSTALLED, control.state)
        assertEquals(null, shadowOf(app).nextStartedActivity)
        request!!.useFallback()
        assertSame(fallback, control.host)
        assertEquals(FxState.SHOWN, control.state)
    }

    /** Manual 的 deny() 只是"放弃"，配了 fallback 也不能偷偷降级——降级必须由 useFallback() 显式发起 */
    @Test
    fun `manual deny stays installed even with a fallback`() {
        ShadowSettings.setCanDrawOverlays(false)
        var request: FxPermissionRequest? = null
        val parent = FrameLayout(app)
        val fallback = LayerHost(parent)
        val host = SystemHost.builder(app).permission(FxPermissionStrategy.manual { request = it }).fallback(fallback).build()
        val control = install(host)
        control.show()

        request!!.deny()
        assertSame(host, control.host)
        assertEquals(FxState.INSTALLED, control.state)
        assertEquals(0, parent.childCount)

        // 之后拿到权限，retryPermission() 照常恢复成系统窗口
        ShadowSettings.setCanDrawOverlays(true)
        host.retryPermission()
        assertSame(host, control.host)
        assertEquals(FxState.SHOWN, control.state)
    }

    /** 没配 fallback 时 useFallback() 等同 deny()：停在 INSTALLED，不换 host */
    @Test
    fun `manual useFallback without a fallback stays installed`() {
        ShadowSettings.setCanDrawOverlays(false)
        var request: FxPermissionRequest? = null
        val host = SystemHost.builder(app).permission(FxPermissionStrategy.manual { request = it }).build()
        val control = install(host)
        control.show()

        request!!.useFallback()
        assertSame(host, control.host)
        assertEquals(FxState.INSTALLED, control.state)
    }

    @Test
    fun `updateLayout maps the anchor gravity into layout params`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = install(SystemHost.builder(app).build(), FxConfig.builder(content()).anchor(FxGravity.BOTTOM_END).build())
        control.show()
        val w = window(control)
        layout(w) // 触发 onContentSizeChanged → relayout
        assertEquals(Gravity.BOTTOM or Gravity.RIGHT, w.windowParams.gravity)
        assertEquals(0, w.windowParams.x)
        assertEquals(0, w.windowParams.y)
        val b = control.host.bounds()
        assertEquals(b.rect.width - 100f, control.position.x, 0f)
        assertEquals(b.rect.height - 50f, control.position.y, 0f)
    }

    @Test
    fun `moveTo writes layout params`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = install(SystemHost.builder(app).build())
        control.show()
        val w = window(control)
        layout(w)
        control.moveTo(300f, 400f, animate = false)
        assertEquals(300, w.windowParams.x)
        assertEquals(400, w.windowParams.y)
        assertEquals(300f, control.position.x, 0f)
    }

    @Test
    fun `touchable false maps to FLAG_NOT_TOUCHABLE and follows config updates`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = install(SystemHost.builder(app).build(), config(FxGesture.Normal.copy(touchable = false)))
        control.show()
        val w = window(control)
        assertTrue(w.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        control.update(control.config.toBuilder().gesture(FxGesture.Normal).build())
        assertEquals(0, w.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    @Test
    fun `bounds is the real screen size and goes empty after release`() {
        ShadowSettings.setCanDrawOverlays(true)
        val host = SystemHost.builder(app).build()
        val control = install(host)
        val b = host.bounds()
        assertTrue(b.rect.width > 0f)
        assertTrue(b.rect.height > 0f)
        // release 之后没有容器：返回零尺寸，core 会当作"还不能定位"
        control.cancel()
        assertEquals(0f, host.bounds().rect.width, 0f)
        assertEquals(0f, host.bounds().rect.height, 0f)
    }

    @Test
    fun `systemHost dsl installs with a fallback`() {
        ShadowSettings.setCanDrawOverlays(true)
        val parent = FrameLayout(app)
        val control = FloatingX.install("dsl") {
            view { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }
            systemHost(app) { fallback(LayerHost(parent)); layoutParams { it.alpha = 0.9f } }
        }
        control.show()
        assertTrue(control.host is SystemHost)
        assertEquals(0.9f, window(control).windowParams.alpha, 0f)
    }

    /**
     * 端到端拖动：窗口跟着手指走（相对坐标恒定），位置必须严格按 raw 增量累加。
     * 这是 insets 回归的护栏——一旦哪个环节每帧派发 onBoundsChanged，core 会清掉 dragInput，
     * 后面几步增量就全丢了，位移会小于 N×(30,20)。
     */
    @Test
    fun `dragging a window advances the position by the raw deltas`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = install(SystemHost.builder(app).build(), config(FxGesture.Normal.copy(drag = FxDrag.IMMEDIATE)))
        control.show()
        val w = window(control)
        layout(w)
        val startX = w.windowParams.x
        val startY = w.windowParams.y

        // 第一步 (30,20) 必须超过 touchSlop（8dp，mdpi 下 8px），否则拖动根本不会开始
        drag(w, downX = 10f, downY = 10f, dx = 30f, dy = 20f, steps = 5)

        assertEquals(startX + 5 * 30, w.windowParams.x)
        assertEquals(startY + 5 * 20, w.windowParams.y)
        assertEquals(Gravity.TOP or Gravity.LEFT, w.windowParams.gravity)
        assertEquals((startX + 150).toFloat(), control.position.x, 0f)
        assertEquals((startY + 100).toFloat(), control.position.y, 0f)
    }

    /** 非 TOP_START 锚点：LayoutParams 里存的是到对边的距离，位移要看 contentPosition，锚定边不能变 */
    @Test
    fun `dragging keeps the bottom end anchor gravity`() {
        ShadowSettings.setCanDrawOverlays(true)
        val config = FxConfig.builder(content()).anchor(FxGravity.BOTTOM_END, dx = 300f, dy = 300f)
            .gesture(FxGesture.Normal.copy(drag = FxDrag.IMMEDIATE)).build()
        val control = install(SystemHost.builder(app).build(), config)
        control.show()
        val w = window(control)
        layout(w)
        val start = w.contentPosition()
        assertEquals(Gravity.BOTTOM or Gravity.RIGHT, w.windowParams.gravity)

        drag(w, downX = 10f, downY = 10f, dx = 30f, dy = 20f, steps = 5)

        assertEquals(start.x + 150f, w.contentPosition().x, 0f)
        assertEquals(start.y + 100f, w.contentPosition().y, 0f)
        // 松手后锚点被反算提交，落点仍在右下象限，所以窗口的锚定边不变
        assertEquals(Gravity.BOTTOM or Gravity.RIGHT, w.windowParams.gravity)
    }

    @Test
    fun `activity context is unwrapped to the application`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        assertSame(app, SystemHost.builder(activity).build().context)
    }

    /** Activity 常被 ContextThemeWrapper 包一层再传进来（Material 主题）：也必须沿 baseContext 链解包 */
    @Test
    fun `an activity wrapped in a context wrapper is unwrapped too`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val host = SystemHost.builder(ContextThemeWrapper(activity, android.R.style.Theme_DeviceDefault)).build()
        assertNotSame(activity, host.context)
        assertSame(app, host.context)
    }

    @Test
    fun `theme wraps the application context even when an activity was passed in`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val host = SystemHost.builder(ContextThemeWrapper(activity, android.R.style.Theme_DeviceDefault))
            .theme(android.R.style.Theme_DeviceDefault_Light)
            .build()
        val ctx = host.context
        assertTrue(ctx is ContextThemeWrapper)
        assertSame(app, (ctx as ContextThemeWrapper).baseContext)
    }

    @Test
    fun `theme on an application context yields a context theme wrapper`() {
        val host = SystemHost.builder(app).theme(android.R.style.Theme_DeviceDefault).build()
        val ctx = host.context
        assertTrue(ctx is ContextThemeWrapper)
        assertSame(app, (ctx as ContextThemeWrapper).baseContext)
    }

    /**
     * Skip 策略也要能从"挂载失败"里恢复：`permission(skip())` + 需要权限的 type 时 addView 会失败，
     * 恢复条件不能只看 isPermissionGranted（Skip 压根不检查权限，那样这条路径永远走不到）。
     */
    @Test
    fun `retryPermission remounts a failed attach under the skip strategy`() {
        ShadowSettings.setCanDrawOverlays(false)
        val host = SystemHost.builder(app).permission(FxPermissionStrategy.skip()).build()
        val control = install(host)
        control.show()
        val w = window(control)
        // 还原"addView 抛异常后"的现场：窗口没挂上，但 core 仍停在 SHOWN
        wm.removeViewImmediate(w)
        w.isAttachedToWm = false

        host.retryPermission()
        assertTrue(windowViews().contains(w))
        assertTrue(w.isAttachedToWm)
        assertEquals(FxState.SHOWN, control.state)
    }
}
