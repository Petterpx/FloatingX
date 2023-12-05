package com.petterp.floatingx.listener.provider

import android.content.Context
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.view.IFxInternalView

interface IFxPlatformProvider<F : FxBasisHelper> : IFxBasicProvider<F> {
    val context: Context?
    val internalView: IFxInternalView?
    fun show()
    fun hide()
    fun isShow(): Boolean
    fun checkOrInit(): Boolean
}
