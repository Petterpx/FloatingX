package com.petterp.floatingx.scope

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.layout.FxGravity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViewGroupHostTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var control: FxControl? = null

    /** 记录 host 回调顺序的最小 session */
    private class RecordingSession : FxHostSession {
        val events = mutableListOf<String>()
        override fun onHostReady() { events += "ready" }
        override fun onHostLost() { events += "lost" }
        override fun onBoundsChanged() { events += "bounds" }
        override fun requestSwap(fallback: FxHost) { events += "swap" }
    }

    private fun parent(w: Int = 1080, h: Int = 1920): FrameLayout = FrameLayout(context).also {
        it.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
        it.layout(0, 0, w, h)
    }

    private fun content(): FxContent = FxContent.provider { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }

    private fun install(host: ViewGroupHost): FxControl =
        FloatingX.create(FxConfig.builder(content()).anchor(FxGravity.TOP_START).build(), host, "vg").also { control = it }

    @After
    fun tearDown() {
        control?.takeIf { it.state != FxState.CANCELLED }?.cancel()
        control = null
    }

    @Test
    fun `bind reports ready immediately`() {
        val host = ViewGroupHost(parent())
        val session = RecordingSession()
        host.bind(session)
        assertEquals(listOf("ready"), session.events)
    }

    @Test
    fun `attach adds match_parent layer to the view group and detach removes it`() {
        val p = parent()
        val host = ViewGroupHost(p)
        val c = install(host)
        c.show()
        assertEquals(1, p.childCount)
        val layer = p.getChildAt(0)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, layer.layoutParams.width)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, layer.layoutParams.height)
        assertEquals(FxState.SHOWN, c.state)
        c.cancel()
        assertEquals(0, p.childCount)
    }

    @Test
    fun `content is positioned by anchor after parent layout`() {
        val p = parent()
        val c = install(ViewGroupHost(p))
        c.show()
        // 容器与内容尚未布局：跑一次父容器布局，容器 onSizeChanged → onBoundsChanged → 按锚点定位
        p.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY))
        p.layout(0, 0, 1080, 1920)
        assertEquals(0f, c.position.x, 0f)
        assertEquals(0f, c.position.y, 0f)
        c.moveTo(300f, 400f, animate = false)
        assertEquals(300f, c.position.x, 0f)
        assertEquals(400f, c.position.y, 0f)
    }

    @Test
    fun `window detach reports lost and re-attach reports ready`() {
        // 用真实 Activity 拿到会经历 window attach/detach 的 ViewGroup
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val contentView = controller.get().findViewById<ViewGroup>(android.R.id.content)
        val host = ViewGroupHost(contentView)
        val c = install(host)
        c.show()
        assertEquals(FxState.SHOWN, c.state)
        assertEquals(1, contentView.childCount)

        controller.pause().stop().destroy() // DecorView 从 window 卸下 → onViewDetachedFromWindow
        assertEquals(FxState.INSTALLED, c.state)
        assertEquals(0, contentView.childCount)
    }

    @Test
    fun `bounds follow view group size with no insets`() {
        val host = ViewGroupHost(parent(720, 1280))
        val b = host.bounds()
        assertEquals(720f, b.rect.width, 0f)
        assertEquals(1280f, b.rect.height, 0f)
        assertEquals(0f, b.insets.top, 0f)
    }

    @Test
    fun `release stops forwarding window events`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val contentView = controller.get().findViewById<ViewGroup>(android.R.id.content)
        val host = ViewGroupHost(contentView)
        val session = RecordingSession()
        host.bind(session)
        host.release()
        controller.pause().stop().destroy()
        assertEquals(listOf("ready"), session.events)
    }

    @Test
    fun `of is a java friendly factory`() {
        val p = parent()
        assertSame(p, ViewGroupHost.of(p).viewGroup)
    }
}
