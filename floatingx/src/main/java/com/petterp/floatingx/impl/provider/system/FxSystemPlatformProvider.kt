package com.petterp.floatingx.impl.provider.system

import android.content.Context
import android.view.View
import android.view.WindowManager
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.listener.provider.IFxPlatformProvider
import com.petterp.floatingx.util.isVisibility
import com.petterp.floatingx.view.IFxInternalView
import com.petterp.floatingx.view.FxSystemContainerView

/**
 *
 * @author petterp
 */
class FxSystemPlatformProvider(override val helper: FxAppHelper) :
    IFxPlatformProvider<FxAppHelper> {
    private var _internalView: FxSystemContainerView? = null
    private var wm: WindowManager? = null

    override val context: Context
        get() = helper.context
    override val internalView: IFxInternalView?
        get() = _internalView

    override fun show() {
        val internalView = _internalView ?: return
        internalView.registerWM(wm ?: return)
        internalView.isVisibility = true
    }

    override fun hide() {
        val internalView = _internalView ?: return
        // FIXME: 这里本来想直接remove,但是会引发LeakCanary的内存泄漏警告，故才用Gone
        internalView.isVisibility = false
    }

    override fun isShow(): Boolean {
        val internalView = _internalView ?: return false
        return internalView.isAttachToWM && internalView.visibility == View.VISIBLE
    }

    override fun reset() {
        val internalView = _internalView ?: return
        internalView.isVisibility = false
        wm?.removeViewImmediate(internalView)
    }

    override fun checkOrInit(): Boolean {
        if (_internalView == null) {
            wm = helper.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            _internalView = FxSystemContainerView(helper, wm!!, context)
            _internalView!!.initView()
        }
        return true
    }
}
