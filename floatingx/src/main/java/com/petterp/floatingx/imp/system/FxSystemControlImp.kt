package com.petterp.floatingx.imp.system

import android.app.Activity
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.imp.FxBasisControlImp
import com.petterp.floatingx.listener.control.IFxAppControl

/**
 *
 * @author petterp
 */
class FxSystemControlImp(helper: FxAppHelper) :
    FxBasisControlImp<FxAppHelper, FxSystemPlatformProvider>(helper), IFxAppControl {
    override fun createPlatformProvider(f: FxAppHelper) =
        FxSystemPlatformProvider(helper, FxSystemLifecycleImp(helper))

    override fun getBindActivity(): Activity? {
        return null
    }
}
