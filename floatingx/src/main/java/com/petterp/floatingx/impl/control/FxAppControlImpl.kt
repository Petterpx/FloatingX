package com.petterp.floatingx.impl.control

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.listener.control.IFxAppControl
import com.petterp.floatingx.util.*
import com.petterp.floatingx.util.FxDebug
import com.petterp.floatingx.util.lazyLoad
import com.petterp.floatingx.util.topActivity
import java.lang.ref.WeakReference

/** 全局控制器 */
open class FxAppControlImpl(private val helper: AppHelper) :
    FxBasisControlImpl(helper), IFxAppControl {

    /** 对于状态栏高度的实时监听,在小屏模式下,效果极好 */
    private val windowsInsetsListener by lazyLoad {
        OnApplyWindowInsetsListener { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            helper.statsBarHeight = statusBar
            FxDebug.v("System--StatusBar---old-(${helper.statsBarHeight}),new-($statusBar))")
            insets
        }
    }

    override fun show(activity: Activity) {
        if (isShow()) return
        attach(activity)
        getManagerView()?.show()
    }

    override fun detach(activity: Activity) {
        activity.decorView?.let {
            detach(it)
        }
    }

    override fun show() {
        if (isShow()) return
        attach(topActivity!!)
        getManagerView()?.show()
    }

    override fun updateMangerView(layout: Int) {
        super.updateMangerView(layout)
        ViewCompat.setOnApplyWindowInsetsListener(getManagerView()!!, windowsInsetsListener)
        getManagerView()?.requestApplyInsets()
    }

    override fun context(): Context {
        return helper.application
    }

    internal fun attach(activity: Activity) {
        activity.decorView?.let {
            if (getManagerView()?.parent === it) {
                return
            }
            var isAnimation = false
            if (getManagerView() == null) {
                helper.updateNavigationBar(activity)
                updateMangerView()
                isAnimation = true
            } else {
                if (getManagerView()?.isVisible == false) getManagerView()?.isVisible = true
                detach()
            }
            mContainer = WeakReference(it)
            FxDebug.d("view-lifecycle-> code->addView")
            helper.iFxViewLifecycle?.postAttach()
            getContainer()?.addView(getManagerView())
            if (isAnimation && helper.enableAnimation && helper.fxAnimation != null) {
                helper.fxAnimation?.fromStartAnimator(getManagerView())
                FxDebug.d("view->Animation -----start")
            }
        } ?: FxDebug.e("system -> fxParentView==null")
    }

    override fun detach(container: ViewGroup?) {
        super.detach(container)
        clearContainer()
    }

    override fun reset() {
        super.reset()
        FloatingX.reset()
    }
}
