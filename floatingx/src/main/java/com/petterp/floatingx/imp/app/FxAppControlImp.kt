package com.petterp.floatingx.imp.app

import android.app.Activity
import android.app.Application
import android.view.View
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.imp.FxBasisControlImp
import com.petterp.floatingx.listener.control.IFxAppControl
import com.petterp.floatingx.util.decorView
import com.petterp.floatingx.util.topActivity

/** 全局控制器 */
class FxAppControlImp(helper: FxAppHelper) :
    FxBasisControlImp<FxAppHelper, FxAppPlatformProvider>(helper), IFxAppControl {

    override fun createPlatformProvider(f: FxAppHelper) = FxAppPlatformProvider(f, this)

    override fun getBindActivity(): Activity? {
        val groupView = getManagerView()?.parent ?: return null
        if (groupView === topActivity?.decorView) {
            return topActivity
        }
        return null
    }

    override fun updateView(view: View) {
        check(view.context is Application) {
            "view.context != Application,The global floating window must use application as context!"
        }
        super.updateView(view)
    }

    @JvmSynthetic
    internal fun reAttach(activity: Activity) {
        if (!platformProvider.reAttach(activity)) return
        show()
    }

    @JvmSynthetic
    internal fun destroyToDetach(activity: Activity) {
        platformProvider.destroyToDetach(activity)
    }

    override fun reset() {
        super.reset()
        FloatingX.uninstall(helper.tag, this)
    }

    override fun move(x: Float, y: Float, useAnimation: Boolean) {
        // 需要考虑到状态栏的高度
        super.move(x, y + helper.statsBarHeight, useAnimation)
    }
}
