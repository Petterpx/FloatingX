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
        observerAppLifecycle()
        if (isShow()) return
        if (attach(activity))
            getManagerView()?.show()
    }

    private fun observerAppLifecycle() {
        FloatingX.initAppLifecycle()
    }

    override fun detach(activity: Activity) {
        activity.decorView?.let {
            detach(it)
        }
    }

    /** 请注意：
     * 调用此方法前请确定在初始化fx时,调用了show方法,否则,fx默认不会插入到全局Activity */
    override fun show() {
        if (topActivity == null) {
            helper.fxLog?.e("show-fx---topActivity=null!!!")
            return
        }
        show(topActivity!!)
    }

    override fun context(): Context {
        return helper.application
    }

    private fun initWindowsInsetsListener() {
        getManagerView()?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it, windowsInsetsListener)
            it.requestApplyInsets()
        }
    }

    internal fun attach(activity: Activity): Boolean {
        activity.decorView?.let {
            if (getContainer() === it) {
                return false
            }
            var isAnimation = false
            if (getManagerView() == null) {
                helper.updateNavigationBar(activity)
                helper.updateStatsBar(activity)
                initManagerView()
                isAnimation = true
            } else {
                if (getManagerView()?.isVisible == false) getManagerView()?.isVisible = true
                detach()
            }
            mContainer = WeakReference(it)
            helper.fxLog?.d("fxView-lifecycle-> code->addView")
            helper.iFxViewLifecycle?.postAttach()
            getContainer()?.addView(getManagerView())
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
        FloatingX.reset()
    }

    private fun clearWindowsInsetsListener() {
        getManagerView()?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it, null)
        }
    }
}
