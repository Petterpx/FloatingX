package com.petterp.floatingx.core.gesture

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class FxGestureDetectorTest {

    private class Recorder : FxGestureDetector.Callback {
        val events = mutableListOf<String>()
        var dragAllowed = true
        var scrollableChild = false
        override fun onClick() { events += "click" }
        override fun onLongPress() { events += "longPress" }
        override fun onDragStart() { events += "dragStart" }
        override fun onDrag(dx: Float, dy: Float) { events += "drag:${dx.toInt()},${dy.toInt()}" }
        override fun onDragEnd() { events += "dragEnd" }
        override fun canDragFrom(x: Float, y: Float): Boolean = dragAllowed
        override fun hasScrollableChildAt(x: Float, y: Float): Boolean = scrollableChild
    }

    private val recorder = Recorder()
    private val detector = FxGestureDetector(touchSlop = 8f, defaultLongPressTimeout = 400L, callback = recorder)
    private var time = 0L

    private fun single(action: Int, x: Float, y: Float): MotionEvent = MotionEvent.obtain(0L, ++time, action, x, y, 0)

    /** points = (pointerId, x, y)；主指抬起后剩余手指保留自己的 id */
    private fun multi(action: Int, actionIndex: Int, vararg points: Triple<Int, Float, Float>): MotionEvent {
        val props = Array(points.size) { i -> MotionEvent.PointerProperties().apply { id = points[i].first; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coords = Array(points.size) { i -> MotionEvent.PointerCoords().apply { x = points[i].second; y = points[i].third } }
        val fullAction = action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        return MotionEvent.obtain(0L, ++time, fullAction, points.size, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
    }

    private fun down(x: Float, y: Float) = detector.onTouch(single(MotionEvent.ACTION_DOWN, x, y))
    private fun move(x: Float, y: Float) = detector.onTouch(single(MotionEvent.ACTION_MOVE, x, y))
    private fun up(x: Float, y: Float) = detector.onTouch(single(MotionEvent.ACTION_UP, x, y))
    private fun idle(ms: Long) = ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)

    @Test
    fun `tap within slop fires click on up`() {
        down(10f, 10f); up(12f, 12f)
        assertEquals(listOf("click"), recorder.events)
    }

    @Test
    fun `click disabled fires nothing`() {
        detector.config = FxGesture(click = false)
        down(10f, 10f); up(12f, 12f)
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `moving beyond slop drags and never clicks`() {
        down(10f, 10f); move(30f, 10f); move(50f, 10f); up(50f, 10f)
        assertEquals(listOf("dragStart", "drag:20,0", "drag:20,0", "dragEnd"), recorder.events)
    }

    @Test
    fun `long press fires during press not on up`() {
        down(10f, 10f)
        idle(401)
        assertEquals(listOf("longPress"), recorder.events)
        up(10f, 10f)
        assertEquals(listOf("longPress"), recorder.events)
    }

    @Test
    fun `long press works without click listener`() {
        detector.config = FxGesture(click = false)
        down(10f, 10f); idle(401)
        assertEquals(listOf("longPress"), recorder.events)
    }

    @Test
    fun `movement beyond slop cancels pending long press`() {
        down(10f, 10f); move(30f, 10f); idle(401)
        assertFalse("longPress" in recorder.events)
    }

    @Test
    fun `custom long press timeout is honoured`() {
        detector.config = FxGesture(longPressTimeout = 100L)
        down(10f, 10f); idle(101)
        assertEquals(listOf("longPress"), recorder.events)
    }

    @Test
    fun `after long press mode drags only once long press fired`() {
        detector.config = FxGesture.LongPressToDrag
        down(10f, 10f); move(40f, 10f)
        assertTrue(recorder.events.isEmpty())          // 长按前移动：既不拖也（因移动）不再长按
        up(40f, 10f)
        assertTrue(recorder.events.isEmpty())

        recorder.events.clear()
        down(10f, 10f); idle(401)
        move(40f, 10f); up(40f, 10f)
        assertEquals(listOf("longPress", "dragStart", "drag:30,0", "dragEnd"), recorder.events)
    }

    @Test
    fun `drag disabled never drags and moved up is not a click`() {
        detector.config = FxGesture.ClickOnly
        down(10f, 10f); move(100f, 10f); up(100f, 10f)
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `touchable false ignores everything`() {
        detector.config = FxGesture.DisplayOnly
        assertFalse(down(10f, 10f))
        assertFalse(detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f)))
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `drag region can deny drag`() {
        recorder.dragAllowed = false
        down(10f, 10f); move(100f, 10f); up(100f, 10f)
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `secondary pointer up does not end drag`() {
        down(10f, 10f); move(40f, 10f)
        detector.onTouch(multi(MotionEvent.ACTION_POINTER_DOWN, 1, Triple(0, 40f, 10f), Triple(1, 100f, 100f)))
        detector.onTouch(multi(MotionEvent.ACTION_POINTER_UP, 1, Triple(0, 40f, 10f), Triple(1, 100f, 100f)))
        move(60f, 10f); up(60f, 10f)
        assertEquals(listOf("dragStart", "drag:30,0", "drag:20,0", "dragEnd"), recorder.events)
    }

    @Test
    fun `primary pointer up transfers drag to remaining pointer`() {
        down(10f, 10f); move(40f, 10f)
        detector.onTouch(multi(MotionEvent.ACTION_POINTER_DOWN, 1, Triple(0, 40f, 10f), Triple(1, 100f, 100f)))
        detector.onTouch(multi(MotionEvent.ACTION_POINTER_UP, 0, Triple(0, 40f, 10f), Triple(1, 100f, 100f)))
        detector.onTouch(multi(MotionEvent.ACTION_MOVE, 0, Triple(1, 110f, 100f)))
        detector.onTouch(multi(MotionEvent.ACTION_UP, 0, Triple(1, 110f, 100f)))
        assertEquals(listOf("dragStart", "drag:30,0", "drag:10,0", "dragEnd"), recorder.events)
    }

    @Test
    fun `intercept steals at slop with parent priority`() {
        detector.config = FxGesture(childPriority = FxChildPriority.PARENT)
        assertFalse(detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f)))
        assertFalse(detector.onIntercept(single(MotionEvent.ACTION_MOVE, 12f, 10f)))
        assertTrue(detector.onIntercept(single(MotionEvent.ACTION_MOVE, 30f, 10f)))
        assertEquals(listOf("dragStart"), recorder.events)
        move(50f, 10f); up(50f, 10f)
        assertEquals(listOf("dragStart", "drag:40,0", "dragEnd"), recorder.events)
    }

    @Test
    fun `intercept never steals with child priority`() {
        detector.config = FxGesture(childPriority = FxChildPriority.CHILD)
        detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f))
        assertFalse(detector.onIntercept(single(MotionEvent.ACTION_MOVE, 100f, 10f)))
    }

    @Test
    fun `auto priority defers to scrollable child for immediate drag`() {
        recorder.scrollableChild = true
        detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f))
        assertFalse(detector.onIntercept(single(MotionEvent.ACTION_MOVE, 100f, 10f)))
    }

    @Test
    fun `auto priority still drags scrollable child after long press`() {
        recorder.scrollableChild = true
        detector.config = FxGesture.LongPressToDrag
        detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f))
        idle(401)
        assertTrue(detector.onIntercept(single(MotionEvent.ACTION_MOVE, 12f, 10f)))
    }

    @Test
    fun `up seen by intercept means child consumed stream so no click`() {
        detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f))
        detector.onIntercept(single(MotionEvent.ACTION_UP, 10f, 10f))
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `cancel ends an active drag`() {
        down(10f, 10f); move(40f, 10f)
        detector.cancel()
        assertEquals(listOf("dragStart", "drag:30,0", "dragEnd"), recorder.events)
    }
}
