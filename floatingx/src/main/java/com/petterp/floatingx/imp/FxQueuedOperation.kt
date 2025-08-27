package com.petterp.floatingx.imp

import com.petterp.floatingx.listener.provider.IFxHolderProvider

/**
 * Represents a queued operation that should be executed when the floating window is ready
 * @author petterp
 */
internal sealed class FxQueuedOperation {
    data class Show(val dummy: Unit = Unit) : FxQueuedOperation()
    data class Hide(val dummy: Unit = Unit) : FxQueuedOperation()
    data class Move(val x: Float, val y: Float, val useAnimation: Boolean) : FxQueuedOperation()
    data class MoveByVector(val x: Float, val y: Float, val useAnimation: Boolean) : FxQueuedOperation()
    data class UpdateViewContent(val provider: IFxHolderProvider) : FxQueuedOperation()
}