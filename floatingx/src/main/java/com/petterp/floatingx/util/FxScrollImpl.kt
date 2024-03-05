package com.petterp.floatingx.util

import android.view.MotionEvent
import com.petterp.floatingx.listener.IFxTouchListener

/**
 * IFxScrollListener 的空实现
 * @author petterp
 */
open class FxScrollImpl : IFxTouchListener {
    override fun down() {}

    override fun up() {}

    override fun dragIng(event: MotionEvent, x: Float, y: Float) {
    }

    override fun eventIng(event: MotionEvent) {}
}
