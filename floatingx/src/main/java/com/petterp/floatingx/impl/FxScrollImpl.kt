package com.petterp.floatingx.impl

import com.petterp.floatingx.listener.IFxScrollListener

/**
 * IFxScrollListener 的空实现
 * @author petterp
 */
class FxScrollImpl : IFxScrollListener {
    override fun down() {}

    override fun up() {}

    override fun dragIng(x: Float, y: Float) {}
}
