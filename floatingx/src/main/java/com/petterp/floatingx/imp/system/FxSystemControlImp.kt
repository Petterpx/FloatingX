package com.petterp.floatingx.imp.system

import android.app.Activity
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.FxScopeType
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.imp.FxBasisControlImp
import com.petterp.floatingx.listener.control.IFxAppControl
import com.petterp.floatingx.listener.control.IFxConfigControl

/**
 * FxSystemControl
 * @author petterp
 */
class FxSystemControlImp(helper: FxAppHelper) :
    FxBasisControlImp<FxAppHelper, FxSystemPlatformProvider>(helper), IFxAppControl {

    override fun createPlatformProvider(f: FxAppHelper) = FxSystemPlatformProvider(helper, this)

    override fun createConfigProvider(
        f: FxAppHelper,
        p: FxSystemPlatformProvider
    ) = FxSystemConfigProvider(f, p)

    override fun getBindActivity(): Activity? {
        return null
    }

    override fun reset() {
        super.reset()
        FloatingX.uninstall(helper.tag, this)
    }

    /**
     * 浮窗内部实现自动降级的方式
     * */
    internal fun checkReInstallShow() {
        helper.fxLog.e("tag:[${helper.tag}] auto downgrade to app activity scope!")
        helper.scope = FxScopeType.APP
        helper.reInstall = true
        return FloatingX.install(helper).show()
    }
}
