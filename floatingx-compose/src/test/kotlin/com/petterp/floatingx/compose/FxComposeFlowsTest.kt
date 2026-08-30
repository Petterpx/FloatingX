package com.petterp.floatingx.compose

import android.content.Context
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
