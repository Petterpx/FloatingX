package com.petterp.floatingx.impl.control

import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.listener.control.IFxSystemControl

/**
 *
 * @author petterp
 */
class FxSystemControlImpl(private val helper: FxAppHelper) :
    FxBasisControlImpl(helper), IFxSystemControl {
    override fun show() {
    }

    override fun hide() {
        super.hide()
    }

    override fun isShow(): Boolean {
        return true
    }
}
