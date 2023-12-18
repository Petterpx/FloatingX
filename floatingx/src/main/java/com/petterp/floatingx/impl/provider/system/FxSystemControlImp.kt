package com.petterp.floatingx.impl.provider.system

import android.app.Activity
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.impl.provider.FxBasisControlImpl
import com.petterp.floatingx.listener.control.IFxAppControl

/**
 *
 * @author petterp
 */
class FxSystemControlImp(helper: FxAppHelper) :
    FxBasisControlImpl<FxAppHelper, FxSystemPlatformProvider>(helper), IFxAppControl {
    override fun createPlatformProvider(f: FxAppHelper) = FxSystemPlatformProvider(helper)
    override fun getBindActivity(): Activity? {
        return null
    }
}
