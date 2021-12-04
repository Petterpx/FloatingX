package com.petterp.floatingx.impl

import android.view.MotionEvent
import com.petterp.floatingx.listener.IFxScrollListener

/**
 * IFxScrollListener 的空实现
 * @author petterp
 */
open class FxScrollImpl : IFxScrollListener {
    override fun down() {}

    override fun up() {}

    override fun dragIng(event: MotionEvent, x: Float, y: Float) {
    }

    override fun eventIng(event: MotionEvent) {}
}
