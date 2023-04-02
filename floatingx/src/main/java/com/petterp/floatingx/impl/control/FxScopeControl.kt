package com.petterp.floatingx.impl.control

import android.app.Application
import android.view.View
import androidx.core.view.ViewCompat
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.listener.control.IFxScopeControl

/** Fx普通View控制器 */
class FxScopeControl<T>(helper: BasisHelper) :
    FxBasisControlImpl(helper),
    IFxScopeControl<T> {

    override fun show() {
        if (isShow()) return
        val managerView = getOrInitManagerView() ?: return
        updateEnableStatus(true)
        if (!ViewCompat.isAttachedToWindow(managerView)) {
            getContainerGroup()?.addView(managerView)
        }
        managerView.show()
    }

    override fun updateView(view: View) {
        if (view.context is Application) {
            throw IllegalArgumentException("view == Application,Scope floating windows cannot use application-level views!")
        }
        super.updateView(view)
    }
}
