package com.petterp.floatingx.imp.scope

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.listener.provider.IFxPlatformProvider
import com.petterp.floatingx.view.FxDefaultContainerViewHelper
import com.petterp.floatingx.view.IFxInternalViewHelper
import java.lang.ref.WeakReference

/**
 * Fx局部控制器
 * @author petterp
 */
class FxScopePlatFromProvider(
    override val helper: FxScopeHelper,
    override val control: FxScopeControl,
) : IFxPlatformProvider<FxScopeHelper> {

    private var _internalView: FxDefaultContainerViewHelper? = null
    private var _containerGroup: WeakReference<ViewGroup>? = null

    private val containerGroupView: ViewGroup?
        get() = _containerGroup?.get()
    override val context: Context?
        get() = containerGroupView?.context

    override val internalView: IFxInternalViewHelper?
        get() = _internalView

    fun setContainerGroup(viewGroup: ViewGroup) {
        _containerGroup = WeakReference(viewGroup)
    }

    override fun show() {
        _internalView?.visibility = View.VISIBLE
    }

    override fun hide() {
        _internalView?.visibility = View.GONE
    }

    override fun checkOrInit(): Boolean {
        if (_internalView == null) {
            val parentView = containerGroupView ?: return false
            _internalView = FxDefaultContainerViewHelper(helper, parentView.context)
            _internalView?.initView()
            parentView.addView(_internalView)
        }
        return true
    }

    override fun reset() {
        containerGroupView?.removeView(_internalView)
        _containerGroup?.clear()
        _containerGroup = null
    }
}
