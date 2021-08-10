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
import com.petterp.floatingx.util.decorView
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
            helper.fxLog?.v("System--StatusBar---old-(${helper.statsBarHeight}),new-($statusBar))")
            insets
        }
    }

    override fun show(activity: Activity) {
        super.show()
        if (isShow()) return
        if (attach(activity))
            getManagerView()?.show()
    }

    override fun detach(activity: Activity) {
        activity.decorView?.let {
            detach(it)
        }
    }

    override fun show() {
        show(topActivity!!)
    }

    override fun updateMangerView(layout: Int) {
        super.updateMangerView(layout)
        ViewCompat.setOnApplyWindowInsetsListener(getManagerView()!!, windowsInsetsListener)
        getManagerView()?.requestApplyInsets()
    }

    override fun context(): Context {
        return helper.application
    }

    internal fun attach(activity: Activity): Boolean {
        activity.decorView?.let {
            if (getContainer() === it) {
                return false
            }
            var isAnimation = false
            if (getManagerView() == null) {
                helper.updateNavigationBar(activity)
                initManagerView()
                isAnimation = true
            } else {
                if (getManagerView()?.isVisible == false) getManagerView()?.isVisible = true
                detach()
            }
            mContainer = WeakReference(it)
            helper.fxLog?.d("view-lifecycle-> code->addView")
            helper.iFxViewLifecycle?.postAttach()
            getContainer()?.addView(getManagerView())
            if (isAnimation && helper.enableAnimation && helper.fxAnimation != null) {
                helper.fxLog?.d("view->Animation -----start")
                helper.fxAnimation?.fromStartAnimator(getManagerView())
            }
        } ?: helper.fxLog?.e("system -> fxParentView==null")
        return true
    }

    override fun detach(container: ViewGroup?) {
        super.detach(container)
        clearContainer()
    }

    override fun reset() {
        getManagerView()?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it, null)
        }
        super.reset()
        FloatingX.reset()
    }
}
