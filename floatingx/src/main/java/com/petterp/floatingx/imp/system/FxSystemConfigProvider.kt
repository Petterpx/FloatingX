package com.petterp.floatingx.imp.system

import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.imp.FxBasicConfigProvider

/**
 * Fx系统浮窗ConfigProvider
 * @author petterp
 */
class FxSystemConfigProvider(
    helper: FxAppHelper,
    platformProvider: FxSystemPlatformProvider?
) : FxBasicConfigProvider<FxAppHelper, FxSystemPlatformProvider>(helper, platformProvider) {
    override fun setEnableHalfHide(isEnable: Boolean) {
        if (helper.enableHalfHide != isEnable) p?.internalView?.updateEnableHalfStatus(isEnable)
        super.setEnableHalfHide(isEnable)
    }

    override fun setEnableHalfHide(isEnable: Boolean, percent: Float) {
        if (helper.enableHalfHide != isEnable) p?.internalView?.updateEnableHalfStatus(isEnable)
        super.setEnableHalfHide(isEnable, percent)
    }
}