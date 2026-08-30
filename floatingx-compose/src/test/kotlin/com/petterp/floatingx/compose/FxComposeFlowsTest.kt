package com.petterp.floatingx.compose

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.gesture.FxDrag
import com.petterp.floatingx.core.gesture.FxGesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class FxComposeFlowsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun parent() = FrameLayout(context).also {
        it.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY))
        it.layout(0, 0, 1080, 1920)
    }

    @Test
    fun `stateFlow tracks show hide cancel and is cached per control`() {
        val p = parent()
        val c = FloatingX.create("f") { view { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }; host = TestHost(p) }
        val flow = c.stateFlow()
        assertSame(flow, c.stateFlow())
        assertEquals(FxState.ATTACHED, flow.value)
        c.show()
        assertEquals(FxState.SHOWN, flow.value)
        c.hide()
        assertEquals(FxState.ATTACHED, flow.value)
        c.cancel()
        assertEquals(FxState.CANCELLED, flow.value)
    }

    /** engine 是「先回调后改 state」，flow 写的是每次转移的目标状态，这里覆盖 detach/attach 一圈 */
    @Test
    fun `stateFlow tracks host lost and ready`() {
        val p = parent()
        val host = TestHost(p)
        val c = FloatingX.create("f") { view { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }; this.host = host }
        val flow = c.stateFlow()
        c.show()
        assertEquals(FxState.SHOWN, flow.value)
        host.lose()
        assertEquals(FxState.INSTALLED, flow.value)
        host.ready()
        assertEquals(FxState.SHOWN, flow.value)
        c.cancel()
        assertEquals(FxState.CANCELLED, flow.value)
    }

    /** 拖动中每一帧都要更新：flow 的值始终等于 control.position（屏幕坐标） */
    @Test
    fun `positionFlow tracks the content while dragging`() {
        val p = parent()
        val c = FloatingX.create("f") {
            view { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }
            gesture(FxGesture(drag = FxDrag.IMMEDIATE))
            host = TestHost(p)
        }
        c.show()
        p.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY))
        p.layout(0, 0, 1080, 1920)
        val flow = c.positionFlow()
        val layer = c.contentView!!.parent as View

        var time = 0L
        fun touch(action: Int, x: Float, y: Float) =
            layer.dispatchTouchEvent(MotionEvent.obtain(0L, ++time, action, x, y, 0))

        touch(MotionEvent.ACTION_DOWN, 10f, 10f)
        touch(MotionEvent.ACTION_MOVE, 210f, 310f)
        assertEquals(c.position, flow.value)
        val afterFirstMove = flow.value
        touch(MotionEvent.ACTION_MOVE, 410f, 510f)
        assertEquals(c.position, flow.value)
        assertNotEquals(afterFirstMove, flow.value)   // 每帧都在动
        touch(MotionEvent.ACTION_UP, 410f, 510f)
        ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)   // 吸附/回弹动画收尾
        assertEquals(c.position, flow.value)
        c.cancel()
    }

    @Test
    fun `positionFlow follows moveTo`() {
        val p = parent()
        val c = FloatingX.create("f") { view { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }; host = TestHost(p) }
        c.show()
        p.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY))
        p.layout(0, 0, 1080, 1920)
        val flow = c.positionFlow()
        c.moveTo(300f, 400f, animate = false)
        assertEquals(300f, flow.value.x, 0f)
        assertEquals(400f, flow.value.y, 0f)
        c.cancel()
    }
}
