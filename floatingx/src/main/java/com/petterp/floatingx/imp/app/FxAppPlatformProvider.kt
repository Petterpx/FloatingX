package com.petterp.floatingx.imp.app

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.listener.provider.IFxPlatformProvider
import com.petterp.floatingx.util.decorView
import com.petterp.floatingx.util.safeAddView
import com.petterp.floatingx.util.safeRemoveView
import com.petterp.floatingx.util.topActivity
import com.petterp.floatingx.view.FxDefaultContainerView
import java.lang.ref.WeakReference

/**
 * 免权限的浮窗提供者
 * @author petterp
 */
class FxAppPlatformProvider(
    override val helper: FxAppHelper,
    override val control: FxAppControlImp,
) : IFxPlatformProvider<FxAppHelper> {

    private var _lifecycleImp: FxAppLifecycleImp? = null
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

    private val containerGroupView: ViewGroup?
        get() = _containerGroup?.get()

    override val context: Context
        get() = helper.context
    override val internalView: FxDefaultContainerView?
        get() = _internalView

    init {
        // 这里仅仅是为了兼容旧版逻辑
        checkRegisterAppLifecycle()
    }

    override fun checkOrInit(): Boolean {
        checkRegisterAppLifecycle()
        // topActivity==null,依然返回true,因为在某些情况下，可能会在Activity未创建时，就调用show
        val act = topActivity ?: return true
        if (!helper.isCanInstall(act)) {
            helper.fxLog.d("fx not show,This ${act.javaClass.simpleName} is not in the list of allowed inserts!")
            return false
        }
        if (_internalView == null) {
            _internalView = FxDefaultContainerView(helper, helper.context)
            _internalView?.initView()
            checkOrInitSafeArea(act)
            attach(act)
        }
        return true
    }

    override fun show() {
        val fxView = _internalView ?: return
        if (!ViewCompat.isAttachedToWindow(fxView)) {
            fxView.visibility = View.VISIBLE
            checkOrReInitGroupView()?.safeAddView(fxView)
        } else if (fxView.visibility != View.VISIBLE) {
            fxView.visibility = View.VISIBLE
        }
    }

    override fun hide() {
        detach()
    }

    private fun checkOrReInitGroupView(): ViewGroup? {
        val curGroup = containerGroupView
        if (curGroup == null || curGroup !== topActivity?.decorView) {
            _containerGroup = WeakReference(topActivity?.decorView)
            helper.fxLog.v("view-----> reinitialize the fx container")
        }
        return containerGroupView
    }

    private fun attach(activity: Activity): Boolean {
        val fxView = _internalView ?: return false
        val decorView = activity.decorView ?: return false
        if (containerGroupView === decorView) return false
        if (ViewCompat.isAttachedToWindow(fxView)) containerGroupView?.safeRemoveView(fxView)
        _containerGroup = WeakReference(decorView)
        decorView.safeAddView(fxView)
        return true
    }

    fun reAttach(activity: Activity): Boolean {
        val nContainer = activity.decorView ?: return false
        if (_internalView == null) {
            _containerGroup = WeakReference(nContainer)
            return true
        } else {
            if (nContainer === containerGroupView) return false
            containerGroupView?.safeRemoveView(_internalView)
            nContainer.safeAddView(_internalView)
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
        oldContainer.safeRemoveView(_internalView)
        return true
    }

    override fun reset() {
        hide()
        clearWindowsInsetsListener()
        _internalView = null
        _containerGroup?.clear()
        _containerGroup = null
        helper.context.unregisterActivityLifecycleCallbacks(_lifecycleImp)
        _lifecycleImp = null
    }

    private fun detach() {
        _internalView?.visibility = View.GONE
        containerGroupView?.safeRemoveView(_internalView)
        _containerGroup?.clear()
        _containerGroup = null
    }

    private fun checkRegisterAppLifecycle() {
        if (!helper.enableFx || _lifecycleImp != null) return
        _lifecycleImp = FxAppLifecycleImp(helper, control)
        helper.context.registerActivityLifecycleCallbacks(_lifecycleImp)
    }

    private fun checkOrInitSafeArea(act: Activity) {
        if (!helper.enableSafeArea) return
        helper.updateStatsBar(act)
        helper.updateNavigationBar(act)
        val fxView = _internalView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(fxView, windowsInsetsListener)
        fxView.requestApplyInsets()
    }

    private fun clearWindowsInsetsListener() {
        val managerView = _internalView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(managerView, null)
    }
}
