package com.petterp.floatingx.core

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.feature.ModalScrimFeature
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxMargin
import com.petterp.floatingx.core.storage.FxStorage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class FloatingXEndToEndTest {

    private class MemoryStorage : FxStorage {
        val map = HashMap<String, FxAnchor>()
        override fun save(key: String, anchor: FxAnchor) { map[key] = anchor }
        override fun load(key: String): FxAnchor? = map[key]
        override fun clear(key: String) { map.remove(key) }
    }

    private class Events : FxListener {
        val list = mutableListOf<String>()
        override fun onAttach(control: FxControl) { list += "attach" }
        override fun onDetach(control: FxControl) { list += "detach" }
        override fun onShow(control: FxControl) { list += "show" }
        override fun onHide(control: FxControl) { list += "hide" }
        override fun onClick(control: FxControl, view: View) { list += "click" }
        override fun onDragStart(control: FxControl) { list += "dragStart" }
        override fun onDragEnd(control: FxControl, x: Float, y: Float) { list += "dragEnd" }
        override fun onPositionChanged(control: FxControl, anchor: FxAnchor) { list += "anchor:${anchor.gravity}" }
        override fun onCancel(control: FxControl) { list += "cancel" }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val parent = FrameLayout(context)
    private val events = Events()
    private val storage = MemoryStorage()

    private fun content(): View = TextView(context).apply { layoutParams = FrameLayout.LayoutParams(100, 200); text = "fx" }

    private fun config(block: FxConfig.Builder.() -> Unit = {}): FxConfig =
        FxConfig.builder(FxContent.view(content())).anchor(FxGravity.BOTTOM_END).margin(FxMargin.all(16f)).storage(storage).apply(block).build()

    private fun layoutParent() {
        parent.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY))
        parent.layout(0, 0, 1080, 1920)
    }

    private fun positionOf(c: FxControl) = c.contentView!!.let { it.translationX to it.translationY }

    private var time = 0L
    private fun touch(action: Int, x: Float, y: Float) {
        val container = parent.getChildAt(0)
        container.dispatchTouchEvent(MotionEvent.obtain(0L, ++time, action, x, y, 0))
    }

    @After
    fun tearDown() = FloatingX.uninstallAll()

    @Test
    fun `install attaches immediately when host is ready and registers by tag`() {
        val host = TestHost(parent)
        val c = FloatingX.install("a", config(), host)
        assertSame(c, FloatingX.control("a"))
        assertTrue(FloatingX.isInstalled("a"))
        assertEquals(FxState.ATTACHED, c.state)
        assertEquals(1, host.attachCount)
        assertEquals(View.INVISIBLE, c.contentView!!.visibility)
        assertNotNull(c.holder)
    }

    @Test
    fun `show makes content visible and positions by anchor after layout`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.addListener(events)
        c.show()
        assertEquals(FxState.SHOWN, c.state)
        assertTrue(c.isShowing)
        assertEquals(View.VISIBLE, c.contentView!!.visibility)
        layoutParent()
        assertEquals(964f to 1704f, positionOf(c))
        assertEquals(listOf("show"), events.list)
    }

    @Test
    fun `moveTo without animation commits anchor persists and notifies`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.addListener(events)
        c.show(); layoutParent()
        c.moveTo(100f, 300f, animate = false)
        assertEquals(100f to 300f, positionOf(c))
        assertEquals(FxAnchor(FxGravity.TOP_START, 84f, 284f), c.anchor)
        assertEquals(c.anchor, storage.map.values.single())
        assertTrue("anchor:TOP_START" in events.list)
    }

    @Test
    fun `moveTo before host ready is replayed after first layout`() {
        val host = TestHost(parent, readyOnBind = false)
        val c = FloatingX.install("a", config(), host)
        c.moveTo(50f, 60f, animate = false)
        c.show()
        assertEquals(FxState.INSTALLED, c.state)
        host.ready()
        assertEquals(FxState.SHOWN, c.state)
        layoutParent()
        assertEquals(50f to 60f, positionOf(c))
    }

    @Test
    fun `updateContent works before show`() {
        val c = FloatingX.install("a", config(), TestHost(parent, readyOnBind = false))
        c.updateContent { holder -> (holder.view as TextView).text = "changed" }
        assertEquals("changed", (c.contentView as TextView).text)
    }

    @Test
    fun `hide then host lost then ready restores hidden state`() {
        val host = TestHost(parent)
        val c = FloatingX.install("a", config(), host)
        c.addListener(events)
        c.show(); c.hide()
        assertEquals(FxState.ATTACHED, c.state)
        assertEquals(View.INVISIBLE, c.contentView!!.visibility)
        host.lose()
        assertEquals(FxState.INSTALLED, c.state)
        assertEquals(0, parent.childCount)
        host.ready()
        assertEquals(FxState.ATTACHED, c.state)
        assertEquals(1, parent.childCount)
        assertEquals(listOf("show", "hide", "detach", "attach"), events.list)
    }

    @Test
    fun `host lost while shown re-shows on ready keeping the same content view`() {
        val host = TestHost(parent)
        val c = FloatingX.install("a", config(), host)
        c.show()
        val view = c.contentView
        host.lose(); host.ready()
        assertEquals(FxState.SHOWN, c.state)
        assertSame(view, c.contentView)
        assertEquals(2, host.attachCount)
    }

    @Test
    fun `size change keeps the anchored corner`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.show(); layoutParent()
        assertEquals(964f to 1704f, positionOf(c))
        c.contentView!!.layoutParams = FrameLayout.LayoutParams(300, 200)
        c.contentView!!.requestLayout()
        layoutParent()
        assertEquals(764f to 1704f, positionOf(c))          // 右边缘不动（#206/#187）
    }

    @Test
    fun `update changes anchor and relayouts`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.show(); layoutParent()
        c.update { anchor(FxGravity.TOP_START); margin() }
        assertEquals(FxAnchor(FxGravity.TOP_START), c.anchor)
        assertEquals(0f to 0f, positionOf(c))
    }

    @Test
    fun `stored anchor wins over config anchor at install`() {
        storage.map["a:${context.resources.configuration.orientation}"] = FxAnchor(FxGravity.TOP_START, 10f, 20f)
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.show(); layoutParent()
        assertEquals(26f to 36f, positionOf(c))
    }

    @Test
    fun `click on content dispatches onClick`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.addListener(events)
        c.show(); layoutParent()
        touch(MotionEvent.ACTION_DOWN, 970f, 1710f)
        touch(MotionEvent.ACTION_UP, 972f, 1712f)
        assertTrue("click" in events.list)
    }

    @Test
    fun `drag moves content and adsorbs to nearest edge on release`() {
        val c = FloatingX.install("a", config { adsorb(FxAdsorb.horizontal()) }, TestHost(parent))
        c.addListener(events)
        c.show(); layoutParent()
        touch(MotionEvent.ACTION_DOWN, 970f, 1710f)
        touch(MotionEvent.ACTION_MOVE, 900f, 1710f)
        touch(MotionEvent.ACTION_MOVE, 400f, 1200f)
        assertEquals(394f to 1194f, positionOf(c))          // 跟手：位移 = 手指位移
        touch(MotionEvent.ACTION_UP, 400f, 1200f)
        ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)   // 吸附动画 200ms
        assertEquals(16f to 1194f, positionOf(c))
        assertEquals(FxGravity.BOTTOM_START, c.anchor.gravity)
        assertEquals(listOf("show", "dragStart", "dragEnd", "anchor:BOTTOM_START"), events.list.filter { it != "click" })
    }

    @Test
    fun `touch outside content passes through unless modal`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.show(); layoutParent()
        val outside = MotionEvent.obtain(0L, 1L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        assertFalse(parent.getChildAt(0).dispatchTouchEvent(outside))

        val modal = FloatingX.install("m", config { addFeature(ModalScrimFeature(dismissOnOutsideTouch = true)) }, TestHost(parent))
        modal.show(); layoutParent()
        assertTrue(parent.getChildAt(1).dispatchTouchEvent(MotionEvent.obtain(0L, 2L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)))
        assertEquals(FxState.ATTACHED, modal.state)          // dismissOnOutsideTouch → hide
    }

    @Test
    fun `cancel detaches releases host and unregisters`() {
        val host = TestHost(parent)
        val c = FloatingX.install("a", config(), host)
        c.addListener(events)
        c.show(); c.cancel()
        assertEquals(FxState.CANCELLED, c.state)
        assertEquals(0, parent.childCount)
        assertTrue(host.released)
        assertNull(FloatingX.controlOrNull("a"))
        assertEquals("cancel", events.list.last())
        assertThrows(IllegalStateException::class.java) { c.show() }
    }

    @Test
    fun `installing the same tag cancels the previous control`() {
        val first = FloatingX.install("a", config(), TestHost(parent))
        val second = FloatingX.install("a", config(), TestHost(parent))
        assertEquals(FxState.CANCELLED, first.state)
        assertSame(second, FloatingX.control("a"))
        assertEquals(listOf(second), FloatingX.controls())
    }

    @Test
    fun `create does not register`() {
        val c = FloatingX.create(config(), TestHost(parent))
        assertTrue(FloatingX.controls().isEmpty())
        c.show()
        assertEquals(FxState.SHOWN, c.state)
        c.cancel()
    }

    @Test
    fun `control with unknown tag throws`() {
        assertThrows(IllegalStateException::class.java) { FloatingX.control("nope") }
        assertNull(FloatingX.controlOrNull("nope"))
    }

    @Test
    fun `calls off the main thread throw`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        var error: Throwable? = null
        val t = Thread { runCatching { c.show() }.onFailure { error = it } }
        t.start(); t.join()
        assertTrue(error is IllegalStateException)
    }
}
