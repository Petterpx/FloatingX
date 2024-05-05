package com.petterp.floatingx.listener.provider

import android.content.Context
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.view.IFxInternalHelper

interface IFxPlatformProvider<F : FxBasisHelper> : IFxBasicProvider<F> {
    val context: Context?
    val control: IFxControl?
    val internalView: IFxInternalHelper?
    fun show()
    fun hide()
    fun isShow(): Boolean? = null
    fun checkOrInit(): Boolean
}
