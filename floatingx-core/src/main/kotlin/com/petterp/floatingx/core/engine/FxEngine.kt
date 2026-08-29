package com.petterp.floatingx.core.engine

import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession

/**
 * 显示状态机 + 命令队列（spec §2.2）。
 *
 *   INSTALLED --ready--> ATTACHED --show--> SHOWN
 *       ^                   |  ^              |
 *       +----- hostLost ----+  +---- hide ----+
 *   任意 --cancel--> CANCELLED
 */
internal class FxEngine(private val delegate: FxEngineDelegate) : FxHostSession {

    var state: FxState = FxState.INSTALLED
        private set

    /** 用户意图：最近一次是 show 还是 hide。hostLost 不改它，ready 后按它恢复 */
    var desiredVisible: Boolean = false
        private set

    var hostReady: Boolean = false
        private set

    private val pending = ArrayDeque<FxCommand>()

    fun show() {
        checkAlive()
        desiredVisible = true
        when (state) {
            FxState.INSTALLED -> if (hostReady) attachAndRestore()
            FxState.ATTACHED -> transition(FxState.SHOWN) { delegate.performShow() }
            FxState.SHOWN, FxState.CANCELLED -> Unit
        }
    }

    fun hide() {
        checkAlive()
        desiredVisible = false
        if (state == FxState.SHOWN) transition(FxState.ATTACHED) { delegate.performHide() }
    }

    fun dispatch(command: FxCommand) {
        checkAlive()
        if (state == FxState.INSTALLED) pending.addLast(command) else delegate.perform(command)
    }

    fun cancel() {
        if (state == FxState.CANCELLED) return
        pending.clear()
        val old = state
        if (old != FxState.INSTALLED) delegate.performDetach()
        state = FxState.CANCELLED
        delegate.onStateChanged(old, state)
    }

    override fun onHostReady() {
        if (state == FxState.CANCELLED) return
        hostReady = true
        if (state == FxState.INSTALLED) attachAndRestore()
    }

    override fun onHostLost() {
        hostReady = false
        if (state == FxState.ATTACHED || state == FxState.SHOWN) {
            transition(FxState.INSTALLED) { delegate.performDetach() }
        }
    }

    override fun onBoundsChanged() {
        if (state == FxState.ATTACHED || state == FxState.SHOWN) delegate.onBoundsChanged()
    }

    override fun requestSwap(fallback: FxHost) {
        if (state == FxState.CANCELLED) return
        onHostLost()
        delegate.swapHost(fallback)
    }

    private fun attachAndRestore() {
        transition(FxState.ATTACHED) { delegate.performAttach() }
        while (pending.isNotEmpty()) delegate.perform(pending.removeFirst())
        if (desiredVisible) transition(FxState.SHOWN) { delegate.performShow() }
    }

    private inline fun transition(to: FxState, action: () -> Unit) {
        val old = state
        action()
        state = to
        delegate.onStateChanged(old, to)
    }

    private fun checkAlive() = check(state != FxState.CANCELLED) { "FxControl 已被 cancel，不能再操作" }
}
