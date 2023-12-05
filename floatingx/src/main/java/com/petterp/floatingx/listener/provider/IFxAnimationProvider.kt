package com.petterp.floatingx.listener.provider

import android.widget.FrameLayout
import com.petterp.floatingx.assist.helper.FxBasisHelper

/**
 *
 * @author petterp
 */
interface IFxAnimationProvider : IFxBasicProvider<FxBasisHelper> {
    fun start(view: FrameLayout, obj: (() -> Unit)? = null)

    fun hide(view: FrameLayout, obj: (() -> Unit)? = null)

    fun canRunAnimation(): Boolean
    fun canCancelAnimation(): Boolean
}
