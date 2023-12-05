package com.petterp.floatingx.impl.provider.scope

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.listener.provider.IFxPlatformProvider
import com.petterp.floatingx.view.FxViewHolder
import com.petterp.floatingx.view.IFxInternalView
import com.petterp.floatingx.view.default.FxDefaultContainerView
import java.lang.ref.WeakReference

/**
 * Fx局部控制器
 * @author petterp
 */
class FxScopePlatFromProvider(
    override val helper: FxScopeHelper,
) : IFxPlatformProvider<FxScopeHelper> {

    private var _holder: FxViewHolder? = null
    private var _internalView: FxDefaultContainerView? = null
    private var _containerGroup: WeakReference<ViewGroup>? = null

    private val containerGroupView: ViewGroup?
        get() = _containerGroup?.get()
    override val context: Context?
        get() = containerGroupView?.context

    override val internalView: IFxInternalView?
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

    override fun isShow(): Boolean {
        val managerView = _internalView ?: return false
        return managerView.isAttachedToWindow && managerView.visibility == View.VISIBLE
    }

    override fun checkOrInit(): Boolean {
        if (_internalView == null) {
            val containerGroupView = containerGroupView ?: return false
            _internalView = FxDefaultContainerView(containerGroupView.context).init(helper)
            _holder = FxViewHolder(_internalView?.containerView)
            containerGroupView.addView(_internalView)
        }
        return true
    }

    override fun reset() {
        containerGroupView?.removeView(_internalView)
        _containerGroup?.clear()
        _containerGroup = null
    }
}
