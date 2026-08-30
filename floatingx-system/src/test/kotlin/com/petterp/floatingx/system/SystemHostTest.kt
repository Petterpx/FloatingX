package com.petterp.floatingx.system

import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
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
    fun `bounds is the real screen size`() {
        ShadowSettings.setCanDrawOverlays(true)
        val host = SystemHost.builder(app).build()
        install(host)
        val b = host.bounds()
        assertTrue(b.rect.width > 0f)
        assertTrue(b.rect.height > 0f)
    }

    @Test
    fun `activity context is unwrapped to the application`() {
        val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        assertSame(app, SystemHost.builder(activity).build().context)
    }
}
