package com.petterp.floatingx.impl.control

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.impl.lifecycle.FxTempAppLifecycleImp
import com.petterp.floatingx.listener.control.IFxAppControl
import com.petterp.floatingx.util.decorView
import com.petterp.floatingx.util.topActivity

/** 全局控制器 */
class FxAppControlImpl(
    private val helper: FxAppHelper,
    private val proxyLifecycleImpl: FxTempAppLifecycleImp,
) : FxBasisControlImpl(helper),
    IFxAppControl,
    Application.ActivityLifecycleCallbacks by proxyLifecycleImpl {

    private var isRegisterAppLifecycle = false

    init {
        proxyLifecycleImpl.init(helper, this)
        checkRegisterAppLifecycle()
    }

    override fun context(): Context = helper.context

    private val windowsInsetsListener = OnApplyWindowInsetsListener { _, insets ->
        val statusBar = insets.stableInsetTop
        if (helper.statsBarHeight != statusBar) {
            helper.fxLog?.v("System--StatusBar---old-(${helper.statsBarHeight}),new-($statusBar))")
            helper.statsBarHeight = statusBar
        }
        insets
    }

    override fun show() {
        val act = topActivity ?: return
        if (!helper.isCanInstall(act) || isShow()) return
        checkRegisterAppLifecycle()
        if (attach(act)) {
            internalShow()
            updateEnableStatus(true)
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
        check(view.context is Application) {
            "view.context != Application,The global floating window must use application as context!"
        }
        super.updateView(view)
    }

    override fun cancel() {
        super.cancel()
        clearWindowsInsetsListener()
        if (!FloatingX.fxs.containsValue(this)) return
        FloatingX.fxs.remove(helper.tag)
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

    @JvmSynthetic
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
                    getManagerView()?.visibility = View.VISIBLE
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

    @JvmSynthetic
    internal fun detach(activity: Activity) {
        detach(activity.decorView)
    }

    private fun clearWindowsInsetsListener() {
        val managerView = getManagerView() ?: return
        ViewCompat.setOnApplyWindowInsetsListener(managerView, null)
    }

    private fun initWindowsInsetsListener() {
        getManagerView()?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it, windowsInsetsListener)
            it.requestApplyInsets()
        }
    }

    private fun checkRegisterAppLifecycle() {
        if (!isRegisterAppLifecycle && helper.enableFx) {
            isRegisterAppLifecycle = true
            helper.context.unregisterActivityLifecycleCallbacks(this)
            helper.context.registerActivityLifecycleCallbacks(this)
        }
    }
}
