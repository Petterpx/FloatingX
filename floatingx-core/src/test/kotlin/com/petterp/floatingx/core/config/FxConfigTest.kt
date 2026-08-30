package com.petterp.floatingx.core.config

import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope
import com.petterp.floatingx.core.feature.ModalScrimFeature
import com.petterp.floatingx.core.gesture.FxDrag
import com.petterp.floatingx.core.gesture.FxGesture
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxMargin
import com.petterp.floatingx.core.layout.FxOverflow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FxConfigTest {

    private val content = FxContent.layout(1)
    private val feature = object : FxFeature {
        override fun onAttach(scope: FxFeatureScope) = Unit
        override fun onDetach() = Unit
    }

    @Test
    fun `builder defaults`() {
        val c = FxConfig.builder(content).build()
        assertSame(content, c.content)
        assertEquals(FxAnchor.DEFAULT, c.anchor)
        assertEquals(FxMargin.NONE, c.margin)
        assertEquals(FxOverflow.NONE, c.overflow)
        assertTrue(c.safeArea)
        assertEquals(FxAdsorb.None, c.adsorb)
        assertEquals(FxGesture.Normal, c.gesture)
        assertNull(c.animation)
        assertNull(c.storage)
        assertTrue(c.features.isEmpty())
        assertNull(c.logger)
    }

    @Test
    fun `builder sets everything and toBuilder copies`() {
        val c = FxConfig.builder(content)
            .anchor(FxGravity.BOTTOM_END, 1f, 2f)
            .margin(1f, 2f, 3f, 4f)
            .overflow(FxOverflow(top = true))
            .safeArea(false)
            .adsorb(FxAdsorb.horizontal())
            .gesture(FxGesture.LongPressToDrag)
            .addFeature(feature)
            .enableLog("demo")
            .build()
        val copy = c.toBuilder().build()
        assertEquals(FxAnchor(FxGravity.BOTTOM_END, 1f, 2f), copy.anchor)
        assertEquals(FxMargin(1f, 2f, 3f, 4f), copy.margin)
        assertEquals(FxOverflow(top = true), copy.overflow)
        assertFalse(copy.safeArea)
        assertEquals(FxAdsorb.horizontal(), copy.adsorb)
        assertEquals(FxGesture.LongPressToDrag, copy.gesture)
        assertEquals(listOf(feature), copy.features)
        assertTrue(copy.logger != null)
    }

    @Test
    fun `dsl builds the same config as builder`() {
        val scope = FxConfigScope(null).apply {
            content(content)
            anchor(FxGravity.CENTER_END, dy = 120f)
            margin(top = 24f)
            overflow(top = true)
            safeArea = false
            adsorb(FxAdsorb.horizontal())
            gesture { drag = FxDrag.AFTER_LONG_PRESS; longPress = false }
        }
        val c = scope.build()
        assertEquals(FxAnchor(FxGravity.CENTER_END, 0f, 120f), c.anchor)
        assertEquals(FxMargin(top = 24f), c.margin)
        assertEquals(FxOverflow(top = true), c.overflow)
        assertFalse(c.safeArea)
        assertEquals(FxGesture(longPress = false, drag = FxDrag.AFTER_LONG_PRESS), c.gesture)
    }

    @Test
    fun `dsl from base keeps unspecified values`() {
        val base = FxConfig.builder(content).anchor(FxGravity.TOP_END).gesture(FxGesture.ClickOnly).build()
        val c = FxConfigScope(base).apply { margin(left = 5f) }.build()
        assertEquals(FxAnchor(FxGravity.TOP_END), c.anchor)
        assertEquals(FxGesture.ClickOnly, c.gesture)
        assertEquals(FxMargin(left = 5f), c.margin)
        assertSame(content, c.content)
    }

    @Test
    fun `dsl without content fails loudly`() {
        assertThrows(IllegalArgumentException::class.java) { FxConfigScope(null).apply { anchor(FxGravity.CENTER) }.build() }
    }

    @Test
    fun `install scope has no host by default`() {
        assertNull(FxInstallScope().host)
    }

    @Test
    fun `dsl modal adds a single ModalScrimFeature`() {
        val config = FxConfigScope(null).apply {
            content(content)
            modal()
            modal(dismissOnOutsideTouch = true)
        }.build()
        assertEquals(1, config.features.count { it is ModalScrimFeature })
    }

    @Test
    fun `dsl modal false removes ModalScrimFeature`() {
        val base = FxConfigScope(null).apply { content(content); modal() }.build()
        val updated = FxConfigScope(base).apply { modal(enabled = false) }.build()
        assertTrue(updated.features.none { it is ModalScrimFeature })
    }

    @Test
    fun `builder modal mirrors dsl`() {
        val config = FxConfig.builder(content).modal().modal(false).modal(true, true).build()
        assertEquals(1, config.features.count { it is ModalScrimFeature })
    }
}
