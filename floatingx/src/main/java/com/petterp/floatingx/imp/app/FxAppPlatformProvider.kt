package com.petterp.floatingx.imp.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import com.petterp.floatingx.assist.FxScopeType
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.listener.provider.IFxPlatformProvider
import com.petterp.floatingx.util.decorView
import com.petterp.floatingx.util.topActivity
import com.petterp.floatingx.view.FxDefaultContainerView
import java.lang.ref.WeakReference

/**
 * 免权限的浮窗提供者
 * @author petterp
 */
class FxAppPlatformProvider(
    override val helper: FxAppHelper,
    private val proxyLifecycleImpl: FxAppLifecycleImp,
) : IFxPlatformProvider<FxAppHelper>, Application.ActivityLifecycleCallbacks by proxyLifecycleImpl {

    private var isRegisterAppLifecycle = false
    private var _internalView: FxDefaultContainerView? = null
    private var _containerGroup: WeakReference<ViewGroup>? = null

    private val windowsInsetsListener = OnApplyWindowInsetsListener { _, insets ->
        val statusBar = insets.stableInsetTop
        if (helper.statsBarHeight != statusBar) {
            helper.fxLog.v("System--StatusBar---old-(${helper.statsBarHeight}),new-($statusBar))")
            helper.statsBarHeight = statusBar
        }
        insets
    }

    init {
        checkRegisterAppLifecycle()
    }

    private val containerGroupView: ViewGroup?
        get() = _containerGroup?.get()

    override val context: Context
        get() = helper.context
    override val internalView: FxDefaultContainerView?
        get() = _internalView

    override fun checkOrInit(): Boolean {
        val act = topActivity ?: return false
        if (!helper.isCanInstall(act)) return false
        if (_internalView == null) {
            initWindowsInsetsListener()
            helper.updateNavigationBar(act)
            helper.updateStatsBar(act)
            _internalView = FxDefaultContainerView(helper, helper.context)
            _internalView?.initView()
            checkRegisterAppLifecycle()
            attach(act)
        }
        return true
    }

    override fun show() {
        val fxView = _internalView ?: return
        if (!ViewCompat.isAttachedToWindow(fxView)) {
            fxView.visibility = View.VISIBLE
            containerGroupView?.addView(fxView)
        }
    }

    override fun hide() {
        detach()
    }

    private fun attach(activity: Activity): Boolean {
        val fxView = _internalView ?: return false
        val decorView = activity.decorView ?: return false
        if (containerGroupView === decorView) return false
        if (ViewCompat.isAttachedToWindow(fxView)) containerGroupView?.removeView(fxView)
        _containerGroup = WeakReference(decorView)
        decorView.addView(fxView)
        return true
    }

    fun reAttach(activity: Activity): Boolean {
        val nContainer = activity.decorView ?: return false
        if (_internalView == null) {
            _containerGroup = WeakReference(nContainer)
            return true
        } else {
            if (nContainer === containerGroupView) return false
            containerGroupView?.removeView(_internalView)
            nContainer.addView(_internalView)
            _containerGroup = WeakReference(nContainer)
        }
        return false
    }

    fun destroyToDetach(activity: Activity): Boolean {
        val fxView = _internalView ?: return false
        val oldContainer = containerGroupView ?: return false
        if (!ViewCompat.isAttachedToWindow(fxView)) return false
        val nContainer = activity.decorView ?: return false
        if (nContainer !== oldContainer) return false
        oldContainer.removeView(_internalView)
        return true
    }

    override fun reset() {
        hide()
        clearWindowsInsetsListener()
        _internalView = null
        _containerGroup?.clear()
        _containerGroup = null
        helper.context.unregisterActivityLifecycleCallbacks(this)
    }

    private fun detach() {
        _internalView?.visibility = View.GONE
        containerGroupView?.removeView(_internalView)
    }

    private fun initWindowsInsetsListener() {
        val fxView = _internalView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(fxView, windowsInsetsListener)
        fxView.requestApplyInsets()
    }

    private fun checkRegisterAppLifecycle() {
        if (!isRegisterAppLifecycle && helper.enableFx && helper.scope == FxScopeType.APP) {
            isRegisterAppLifecycle = true
            helper.context.unregisterActivityLifecycleCallbacks(this)
            helper.context.registerActivityLifecycleCallbacks(this)
        }
    }

    private fun clearWindowsInsetsListener() {
        val managerView = _internalView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(managerView, null)
    }
}
