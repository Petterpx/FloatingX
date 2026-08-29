package com.petterp.floatingx.core.engine

import android.content.Context
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FxEngineTest {

    private class FakeDelegate : FxEngineDelegate {
        val calls = mutableListOf<String>()
        val transitions = mutableListOf<String>()
        var swappedTo: FxHost? = null
        override fun performAttach() { calls += "attach" }
        override fun performDetach() { calls += "detach" }
        override fun performShow() { calls += "show" }
        override fun performHide() { calls += "hide" }
        override fun perform(command: FxCommand) { calls += "perform:$command" }
        override fun onBoundsChanged() { calls += "bounds" }
        override fun swapHost(fallback: FxHost) { swappedTo = fallback }
        override fun onStateChanged(old: FxState, new: FxState) { transitions += "$old->$new" }
    }

    private val stubHost = object : FxHost {
        override val context: Context get() = error("unused")
        override fun bind(session: FxHostSession) = Unit
        override fun createContainer(): FxContainer = error("unused")
        override fun attach(container: FxContainer) = Unit
        override fun detach(container: FxContainer) = Unit
        override fun bounds(): FxBounds = error("unused")
        override fun release() = Unit
    }

    private val delegate = FakeDelegate()
    private val engine = FxEngine(delegate)

    @Test
    fun `show before host ready only records desire`() {
        engine.show()
        assertEquals(FxState.INSTALLED, engine.state)
        assertTrue(engine.desiredVisible)
        assertTrue(delegate.calls.isEmpty())
    }

    @Test
    fun `host ready attaches then shows when desired`() {
        engine.show()
        engine.onHostReady()
        assertEquals(listOf("attach", "show"), delegate.calls)
        assertEquals(FxState.SHOWN, engine.state)
        assertEquals(listOf("INSTALLED->ATTACHED", "ATTACHED->SHOWN"), delegate.transitions)
    }

    @Test
    fun `host ready without show only attaches`() {
        engine.onHostReady()
        assertEquals(listOf("attach"), delegate.calls)
        assertEquals(FxState.ATTACHED, engine.state)
    }

    @Test
    fun `commands queued before ready replay in order before show`() {
        val move = FxCommand.MoveTo(10f, 20f, animate = false)
        val by = FxCommand.MoveBy(1f, 1f, animate = true)
        engine.dispatch(move)
        engine.show()
        engine.dispatch(by)
        engine.onHostReady()
        assertEquals(listOf("attach", "perform:$move", "perform:$by", "show"), delegate.calls)
    }

    @Test
    fun `commands after ready execute immediately`() {
        engine.onHostReady()
        val move = FxCommand.MoveTo(1f, 2f, animate = true)
        engine.dispatch(move)
        assertEquals(listOf("attach", "perform:$move"), delegate.calls)
    }

    @Test
    fun `host lost keeps desired visibility and restores on ready`() {
        engine.show(); engine.onHostReady()
        engine.onHostLost()
        assertEquals(FxState.INSTALLED, engine.state)
        assertTrue(engine.desiredVisible)
        assertEquals("detach", delegate.calls.last())
        engine.onHostReady()
        assertEquals(listOf("attach", "show", "detach", "attach", "show"), delegate.calls)
        assertEquals(FxState.SHOWN, engine.state)
    }

    @Test
    fun `hide from shown goes back to attached`() {
        engine.show(); engine.onHostReady()
        engine.hide()
        assertEquals(FxState.ATTACHED, engine.state)
        assertFalse(engine.desiredVisible)
        assertEquals("hide", delegate.calls.last())
        engine.onHostLost(); engine.onHostReady()
        assertEquals(FxState.ATTACHED, engine.state)
        assertEquals(listOf("attach", "show", "hide", "detach", "attach"), delegate.calls)
    }

    @Test
    fun `hide before ready is a no-op besides desire`() {
        engine.show(); engine.hide()
        engine.onHostReady()
        assertEquals(listOf("attach"), delegate.calls)
    }

    @Test
    fun `bounds changed only forwarded while attached`() {
        engine.onBoundsChanged()
        assertTrue(delegate.calls.isEmpty())
        engine.onHostReady()
        engine.onBoundsChanged()
        assertEquals(listOf("attach", "bounds"), delegate.calls)
    }

    @Test
    fun `cancel detaches clears queue and is terminal`() {
        engine.dispatch(FxCommand.MoveTo(0f, 0f, false))
        engine.show(); engine.onHostReady()
        engine.cancel()
        assertEquals(FxState.CANCELLED, engine.state)
        assertEquals("detach", delegate.calls.last())
        assertEquals("SHOWN->CANCELLED", delegate.transitions.last())
        assertThrows(IllegalStateException::class.java) { engine.show() }
        assertThrows(IllegalStateException::class.java) { engine.dispatch(FxCommand.MoveBy(1f, 1f, false)) }
        engine.onHostReady() // 终态后 host 事件被忽略
        assertEquals(FxState.CANCELLED, engine.state)
    }

    @Test
    fun `cancel while installed does not detach`() {
        engine.cancel()
        assertTrue(delegate.calls.isEmpty())
        assertEquals(listOf("INSTALLED->CANCELLED"), delegate.transitions)
    }

    @Test
    fun `request swap detaches and hands over fallback`() {
        engine.show(); engine.onHostReady()
        engine.requestSwap(stubHost)
        assertEquals(FxState.INSTALLED, engine.state)
        assertTrue(engine.desiredVisible)
        assertEquals(stubHost, delegate.swappedTo)
        assertEquals("detach", delegate.calls.last())
    }
}
