package com.petterp.floatingx.core.container

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.layout.FxSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxLayerContainerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val container = FxLayerContainer(context)
    private val content = View(context).apply { layoutParams = FrameLayout.LayoutParams(100, 200) }

    private fun layout() {
        container.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY))
        container.layout(0, 0, 1080, 1920)
    }

    private fun event(action: Int, x: Float, y: Float) = MotionEvent.obtain(0L, 0L, action, x, y, 0)

    @Test
    fun `setContent adds child at top start keeping size`() {
        container.setContent(content)
        val lp = content.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.TOP or Gravity.START, lp.gravity)
        assertEquals(100, lp.width)
        assertEquals(content, container.contentView)
        assertEquals(1, container.childCount)
    }

    @Test
    fun `setContent replaces previous content and reparents`() {
        val other = FrameLayout(context).also { it.addView(content) }
        container.setContent(View(context))
        container.setContent(content)
        assertEquals(0, other.childCount)
        assertEquals(1, container.childCount)
        assertEquals(content, container.contentView)
    }

    @Test
    fun `content size reported after layout only when changed`() {
        val sizes = mutableListOf<FxSize>()
        container.onContentSizeChanged = { sizes += it }
        container.setContent(content)
        layout(); layout()
        assertEquals(listOf(FxSize(100f, 200f)), sizes)
        assertEquals(FxSize(100f, 200f), container.contentSize())
    }

    @Test
    fun `position uses translation and hitTest follows it`() {
        container.setContent(content); layout()
        container.setContentPosition(500f, 600f)
        assertEquals(500f, content.translationX, 0f)
        assertEquals(600f, content.translationY, 0f)
        assertTrue(container.hitTest(550f, 700f))
        assertFalse(container.hitTest(10f, 10f))
        assertFalse(container.hitTest(600f, 700f))
    }

    @Test
    fun `hidden content is not hit`() {
        container.setContent(content); layout()
        container.setContentVisible(false)
        assertEquals(View.INVISIBLE, content.visibility)
        assertFalse(container.hitTest(10f, 10f))
    }

    @Test
    fun `down outside content passes through when not modal`() {
        container.setContent(content); layout()
        assertFalse(container.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, 900f, 900f)))
    }

    @Test
    fun `modal consumes outside touch and notifies`() {
        var outside = 0
        container.setContent(content); layout()
        container.modal = true
        container.onOutsideTouch = { outside++ }
        assertTrue(container.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, 900f, 900f)))
        assertTrue(container.dispatchTouchEvent(event(MotionEvent.ACTION_UP, 900f, 900f)))
        assertEquals(1, outside)
    }

    @Test
    fun `down inside content goes to touch handler`() {
        val seen = mutableListOf<String>()
        container.touchHandler = object : FxContainerTouchHandler {
            override fun onIntercept(ev: MotionEvent): Boolean { seen += "intercept:${ev.actionMasked}"; return false }
            override fun onTouch(ev: MotionEvent): Boolean { seen += "touch:${ev.actionMasked}"; return true }
        }
        container.setContent(content); layout()
        assertTrue(container.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, 10f, 10f)))
        assertEquals(listOf("intercept:${MotionEvent.ACTION_DOWN}", "touch:${MotionEvent.ACTION_DOWN}"), seen)
    }

    @Test
    fun `size change reports bounds changed`() {
        var bounds = 0
        container.onBoundsChanged = { bounds++ }
        layout()
        assertEquals(1, bounds)
    }

    @Test
    fun `screen position adds container location`() {
        container.setContent(content); layout()
        container.setContentPosition(5f, 6f)
        val p = container.contentPositionOnScreen()
        assertEquals(5f, p.x, 0f)
        assertEquals(6f, p.y, 0f)
    }
}
