package com.petterp.floatingx.core.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FxAdsorbResolverTest {

    private val bounds = FxBounds(FxRect(0f, 0f, 1080f, 1920f), FxInsets(top = 80f, bottom = 120f))
    private val size = FxSize(100f, 200f)
    private fun input(ltr: Boolean = true) = FxLayoutInput(bounds, size, ltr)

    private fun assertPoint(x: Float, y: Float, actual: FxPoint) {
        assertEquals("x", x, actual.x, 0.001f)
        assertEquals("y", y, actual.y, 0.001f)
    }

    @Test
    fun `none only clamps`() {
        assertPoint(0f, 500f, FxAdsorbResolver.target(FxPoint(-50f, 500f), input(), FxAdsorb.none()))
    }

    @Test
    fun `horizontal snaps to nearest side keeping y`() {
        assertPoint(0f, 500f, FxAdsorbResolver.target(FxPoint(200f, 500f), input(), FxAdsorb.horizontal()))
        assertPoint(980f, 500f, FxAdsorbResolver.target(FxPoint(900f, 500f), input(), FxAdsorb.horizontal()))
    }

    @Test
    fun `half hide uses independent start and end ratios`() {
        val adsorb = FxAdsorb.horizontal(FxHalfHide(start = 0.2f, end = 0.8f))
        assertPoint(-20f, 500f, FxAdsorbResolver.target(FxPoint(200f, 500f), input(), adsorb))
        assertPoint(1060f, 500f, FxAdsorbResolver.target(FxPoint(900f, 500f), input(), adsorb))
    }

    @Test
    fun `vertical snaps to nearest top or bottom keeping x`() {
        assertPoint(500f, 80f, FxAdsorbResolver.target(FxPoint(500f, 100f), input(), FxAdsorb.vertical()))
        assertPoint(500f, 1600f, FxAdsorbResolver.target(FxPoint(500f, 1500f), input(), FxAdsorb.vertical()))
    }

    @Test
    fun `all picks the globally nearest edge`() {
        assertPoint(500f, 80f, FxAdsorbResolver.target(FxPoint(500f, 100f), input(), FxAdsorb.all()))
        assertPoint(980f, 900f, FxAdsorbResolver.target(FxPoint(950f, 900f), input(), FxAdsorb.all()))
    }

    @Test
    fun `rtl applies end ratio to the physical left side`() {
        val adsorb = FxAdsorb.horizontal(FxHalfHide(start = 0.2f, end = 0.8f))
        assertPoint(-80f, 500f, FxAdsorbResolver.target(FxPoint(200f, 500f), input(ltr = false), adsorb))
    }

    @Test
    fun `only enabled edges are considered`() {
        val onlyStart = FxAdsorb.Edges(setOf(FxEdge.START))
        assertPoint(0f, 500f, FxAdsorbResolver.target(FxPoint(900f, 500f), input(), onlyStart))
    }

    @Test
    fun `point outside area is clamped before snapping`() {
        assertPoint(980f, 1600f, FxAdsorbResolver.target(FxPoint(2000f, 3000f), input(), FxAdsorb.horizontal()))
    }

    @Test
    fun `half hide ratio must be within 0 and 1`() {
        assertThrows(IllegalArgumentException::class.java) { FxHalfHide(1.5f) }
    }
}
