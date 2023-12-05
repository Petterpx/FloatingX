package com.petterp.floatingx.listener.provider

import com.petterp.floatingx.assist.helper.FxBasisHelper

/**
 * 基础fx提供者
 * @author petterp
 */
interface IFxBasicProvider<F : FxBasisHelper> {
    val helper: F
    fun reset() {}
}
