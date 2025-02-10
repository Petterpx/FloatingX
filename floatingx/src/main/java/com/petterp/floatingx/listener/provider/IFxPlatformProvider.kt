package com.petterp.floatingx.listener.provider

import android.content.Context
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.util.isVisibility
import com.petterp.floatingx.view.IFxInternalHelper

interface IFxPlatformProvider<F : FxBasisHelper> : IFxBasicProvider<F> {
    val context: Context?
    val control: IFxControl?
    val internalView: IFxInternalHelper?

    fun isShow(): Boolean {
        val containerView = internalView?.containerView ?: return false
        return containerView.isAttachedToWindow && containerView.isVisibility
    }

    fun show()
    fun hide()
    fun checkOrInit(): Boolean
}
