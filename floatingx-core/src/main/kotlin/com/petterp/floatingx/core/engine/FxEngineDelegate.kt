package com.petterp.floatingx.core.engine

import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.host.FxHost

/** FxEngine 只做状态与排队，所有 View 相关动作通过它回调给 FxControlImpl */
internal interface FxEngineDelegate {
    fun performAttach()
    fun performDetach()
    fun performShow()
    fun performHide()
    fun perform(command: FxCommand)
    fun onBoundsChanged()
    fun swapHost(fallback: FxHost)
    fun onStateChanged(old: FxState, new: FxState)
}
