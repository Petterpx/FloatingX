package com.petterp.floatingx.system

import android.view.Gravity
import android.view.WindowManager
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.system.container.WindowLayoutMath
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WindowLayoutMathTest {

    private fun lp() = WindowManager.LayoutParams()

    private fun apply(x: Float, y: Float, g: FxGravity, ltr: Boolean = true, bw: Int = 1080, bh: Int = 1920, cw: Int = 100, ch: Int = 50) =
        lp().also { WindowLayoutMath.apply(it, x, y, g, ltr, bw, bh, cw, ch) }

    @Test
    fun `top start keeps coordinates`() {
        val lp = apply(20f, 30f, FxGravity.TOP_START)
        assertEquals(Gravity.TOP or Gravity.LEFT, lp.gravity)
        assertEquals(20, lp.x); assertEquals(30, lp.y)
    }

    @Test
    fun `bottom end measures from the far edges`() {
        val lp = apply(1080f - 100f - 20f, 1920f - 50f - 30f, FxGravity.BOTTOM_END)
        assertEquals(Gravity.BOTTOM or Gravity.RIGHT, lp.gravity)
        assertEquals(20, lp.x); assertEquals(30, lp.y)
    }

    @Test
    fun `center measures from the center`() {
        val lp = apply((1080 - 100) / 2f + 5f, (1920 - 50) / 2f - 7f, FxGravity.CENTER)
        assertEquals(Gravity.CENTER, lp.gravity)
        assertEquals(5, lp.x); assertEquals(-7, lp.y)
    }

    @Test
    fun `rtl swaps start and end`() {
        val lp = apply(20f, 0f, FxGravity.TOP_END, ltr = false)
        assertEquals(Gravity.TOP or Gravity.LEFT, lp.gravity)
        assertEquals(20, lp.x)
    }

    @Test
    fun `unknown bounds fall back to top left`() {
        val lp = apply(300f, 400f, FxGravity.BOTTOM_END, bw = 0, bh = 0)
        assertEquals(Gravity.TOP or Gravity.LEFT, lp.gravity)
        assertEquals(300, lp.x); assertEquals(400, lp.y)
    }

    @Test
    fun `unknown content size falls back to top left`() {
        val lp = apply(300f, 400f, FxGravity.CENTER_END, cw = 0, ch = 0)
        assertEquals(Gravity.TOP or Gravity.LEFT, lp.gravity)
        assertEquals(300, lp.x); assertEquals(400, lp.y)
    }
}
