package com.petterp.floatingx.impl.control

import android.app.Application
import android.view.View
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.listener.control.IFxScopeControl

/** Fx普通View控制器 */
class FxScopeControl(helper: BasisHelper) :
    FxBasisControlImpl(helper),
    IFxScopeControl {

    override fun show() {
        if (isShow()) return
        if (getManagerView() == null) initManagerView()
        updateEnableStatus(true)
        getContainerGroup()?.addView(getManagerView())
        getManagerView()?.show()
    }

    override fun updateView(view: View) {
        if (view.context is Application) {
            throw IllegalArgumentException("view == Application,Scope floating windows cannot use application-level views!")
        }
        super.updateView(view)
    }
}
