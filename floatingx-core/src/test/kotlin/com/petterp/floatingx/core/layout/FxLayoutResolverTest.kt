package com.petterp.floatingx.core.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class FxLayoutResolverTest {

    // 1080x1920 屏，状态栏 80，导航栏 120；内容 100x200
    private val bounds = FxBounds(FxRect(0f, 0f, 1080f, 1920f), FxInsets(top = 80f, bottom = 120f))
    private val size = FxSize(100f, 200f)

    private fun input(
        ltr: Boolean = true,
        margin: FxMargin = FxMargin.NONE,
        overflow: FxOverflow = FxOverflow.NONE,
        safeArea: Boolean = true,
        size: FxSize = this.size,
    ) = FxLayoutInput(bounds, size, ltr, margin, overflow, safeArea)

    private fun assertPoint(x: Float, y: Float, actual: FxPoint) {
        assertEquals("x", x, actual.x, 0.001f)
        assertEquals("y", y, actual.y, 0.001f)
    }

    @Test
    fun `area subtracts insets and margin`() {
        val a = input(margin = FxMargin.all(16f)).area
        assertEquals(FxRect(16f, 96f, 1064f, 1784f), a)
    }

    @Test
    fun `top start lands on safe area corner`() {
        assertPoint(0f, 80f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_START), input()))
    }

    @Test
    fun `bottom end subtracts size and insets`() {
        assertPoint(980f, 1600f, FxLayoutResolver.resolve(FxAnchor(FxGravity.BOTTOM_END), input()))
    }

    @Test
    fun `center centers within area`() {
        assertPoint(490f, 840f, FxLayoutResolver.resolve(FxAnchor(FxGravity.CENTER), input()))
    }

    @Test
    fun `dx dy offset inward from the anchored edge`() {
        assertPoint(10f, 100f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_START, 10f, 20f), input()))
        assertPoint(970f, 1580f, FxLayoutResolver.resolve(FxAnchor(FxGravity.BOTTOM_END, 10f, 20f), input()))
    }

    @Test
    fun `margin shrinks area`() {
        assertPoint(16f, 96f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_START), input(margin = FxMargin.all(16f))))
    }

    @Test
    fun `safeArea false ignores insets`() {
        assertPoint(0f, 0f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_START), input(safeArea = false)))
    }

    @Test
    fun `rtl swaps start and end`() {
        assertPoint(980f, 80f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_START), input(ltr = false)))
        assertPoint(0f, 80f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_END), input(ltr = false)))
    }

    @Test
    fun `clamp keeps point inside area`() {
        assertPoint(0f, 1600f, FxLayoutResolver.clamp(FxPoint(-50f, 5000f), input()))
    }

    @Test
    fun `clamp with overflow allows leaving that side`() {
        assertPoint(0f, -300f, FxLayoutResolver.clamp(FxPoint(-50f, -300f), input(overflow = FxOverflow(top = true))))
        assertPoint(-50f, 80f, FxLayoutResolver.clamp(FxPoint(-50f, -300f), input(overflow = FxOverflow(left = true))))
    }

    @Test
    fun `content wider than area aligns to start`() {
        // x：内容宽 2000 > 可用区宽 1080，END 解析出 -920，clamp 的 max < min 兜底回 area.left = 0
        // y：CENTER_END 的纵向是 CENTER，(80+1800)/2 - 200/2 = 840
        assertPoint(0f, 840f, FxLayoutResolver.resolve(FxAnchor(FxGravity.CENTER_END), input(size = FxSize(2000f, 200f))))
    }

    @Test
    fun `toAnchor picks nearest horizontal and vertical edge`() {
        val anchor = FxLayoutResolver.toAnchor(FxPoint(900f, 100f), input())
        assertEquals(FxGravity.TOP_END, anchor.gravity)
        assertEquals(80f, anchor.dx, 0.001f)
        assertEquals(20f, anchor.dy, 0.001f)
    }

    @Test
    fun `toAnchor in rtl maps physical right edge to START`() {
        val anchor = FxLayoutResolver.toAnchor(FxPoint(900f, 100f), input(ltr = false))
        assertEquals(FxGravity.TOP_START, anchor.gravity)
        assertEquals(80f, anchor.dx, 0.001f)
    }

    @Test
    fun `resolve then toAnchor round trips for edge anchors`() {
        listOf(FxGravity.TOP_START, FxGravity.TOP_END, FxGravity.BOTTOM_START, FxGravity.BOTTOM_END).forEach { g ->
            listOf(true, false).forEach { ltr ->
                val anchor = FxAnchor(g, 30f, 40f)
                val back = FxLayoutResolver.toAnchor(FxLayoutResolver.resolve(anchor, input(ltr = ltr)), input(ltr = ltr))
                assertEquals("gravity ltr=$ltr", g, back.gravity)
                assertEquals(30f, back.dx, 0.001f)
                assertEquals(40f, back.dy, 0.001f)
            }
        }
    }
}
