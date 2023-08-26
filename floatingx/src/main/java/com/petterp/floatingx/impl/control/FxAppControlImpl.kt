package com.petterp.floatingx.impl.control

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.impl.lifecycle.FxProxyLifecycleCallBackImpl
import com.petterp.floatingx.listener.control.IFxAppControl
import com.petterp.floatingx.util.decorView
import com.petterp.floatingx.util.topActivity

/** 全局控制器 */
class FxAppControlImpl(
    private val helper: AppHelper,
    private val proxyLifecycleImpl: FxProxyLifecycleCallBackImpl,
) : FxBasisControlImpl(helper),
    IFxAppControl,
    Application.ActivityLifecycleCallbacks by proxyLifecycleImpl {

    init {
        proxyLifecycleImpl.init(helper, this)
    }

    private val windowsInsetsListener = OnApplyWindowInsetsListener { _, insets ->
        val statusBar = insets.stableInsetTop
        if (helper.statsBarHeight != statusBar) {
            helper.fxLog?.v("System--StatusBar---old-(${helper.statsBarHeight}),new-($statusBar))")
            helper.statsBarHeight = statusBar
        }
        insets
    }

    override fun show(activity: Activity) {
        if (isShow()) return
        if (attach(activity)) {
            getManagerView()?.show()
            updateEnableStatus(true)
            FloatingX.checkAppLifecycleInstall()
        }
    }

    override fun detach(activity: Activity) {
        activity.decorView?.let {
            detach(it)
        }
    }

    override fun getBindActivity(): Activity? {
        if (getContainerGroup() === topActivity?.decorView) {
            return topActivity
        }
        return null
    }

    /** 注意,全局浮窗下,view必须是全局application对应的context! */
    override fun updateView(view: View) {
        if (view.context !is Application) {
            throw IllegalArgumentException("view.context != Application,The global floating window must use application as context!")
        }
        super.updateView(view)
    }

    override fun context(): Context = FloatingX.context!!

    private fun initWindowsInsetsListener() {
        getManagerView()?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it, windowsInsetsListener)
            it.requestApplyInsets()
        }
    }

    internal fun attach(activity: Activity): Boolean {
        activity.decorView?.let {
            if (getContainerGroup() === it) {
                return false
            }
            var isAnimation = false
            if (getManagerView() == null) {
                helper.updateNavigationBar(activity)
                helper.updateStatsBar(activity)
                initManagerView()
                isAnimation = true
            } else {
                if (getManagerView()?.visibility != View.VISIBLE) {
                    getManagerView()?.visibility =
                        View.VISIBLE
                }
                detach()
            }
            setContainerGroup(it)
            helper.fxLog?.d("fxView-lifecycle-> code->addView")
            helper.iFxViewLifecycle?.postAttach()
            getContainerGroup()?.addView(getManagerView())
            if (isAnimation && helper.enableAnimation && helper.fxAnimation != null) {
                helper.fxLog?.d("fxView->Animation -----start")
                helper.fxAnimation?.fromStartAnimator(getManagerView())
            }
        } ?: helper.fxLog?.e("system -> fxParentView==null")
        return true
    }

    override fun detach(container: ViewGroup?) {
        super.detach(container)
        clearContainer()
    }

    override fun initManager() {
        // 在清除之前移除insets监听
        clearWindowsInsetsListener()
        super.initManager()
        // 移除之后再添加inset监听
        initWindowsInsetsListener()
    }

    override fun reset() {
        // 重置之前记得移除insets
        clearWindowsInsetsListener()
        super.reset()
        FloatingX.uninstall(helper.tag, this)
    }

    private fun clearWindowsInsetsListener() {
        val managerView = getManagerView() ?: return
        ViewCompat.setOnApplyWindowInsetsListener(managerView, null)
    }
}
