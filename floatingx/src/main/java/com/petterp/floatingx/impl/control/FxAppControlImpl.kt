package com.petterp.floatingx.impl.control

import android.app.Activity
import android.content.Context
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.config.SystemConfig
import com.petterp.floatingx.util.*
import com.petterp.floatingx.util.FxDebug
import com.petterp.floatingx.util.lazyLoad
import com.petterp.floatingx.util.show
import com.petterp.floatingx.util.topActivity
import java.lang.ref.WeakReference

/**
 * @Author petterp
 * @Date 2021/5/21-2:24 下午
 * @Email ShiyihuiCloud@163.com
 * @Function 全局控制器
 */
open class FxAppControlImpl(private val helper: AppHelper) :
    FxBasisControlImpl(helper) {

    private val windowsInsetsListener by lazyLoad {
        OnApplyWindowInsetsListener { _, insets ->
            FxDebug.v("System--StatusBar---old-(${SystemConfig.statsBarHeight}),new-(${insets.systemWindowInsetTop})")
            SystemConfig.statsBarHeight = insets.systemWindowInsetTop
            insets
        }
    }

    /**
     * 显示悬浮窗
     * @param isAnimation 是否执行动画
     * */
    fun show(isAnimation: Boolean) {
        if (isShow()) return
        attach(topActivity!!)
        managerView?.show(isAnimation)
    }

    override fun initManagerView(layout: Int) {
        super.initManagerView(layout)
        ViewCompat.setOnApplyWindowInsetsListener(managerView!!, windowsInsetsListener)
        managerView?.requestApplyInsets()
    }

    override fun context(): Context {
        return helper.application
    }

    override fun attach(activity: Activity) {
        activity.decorView?.let {
            if (managerView?.parent === it) {
                return
            }
            var isAnimation = false
            if (managerView == null) {
                SystemConfig.updateConfig(activity)
                initManagerView()
                isAnimation = true
            } else {
                if (managerView?.isVisible == false) managerView?.isVisible = true
                removeManagerView(getContainer())
            }
            mContainer = WeakReference(it)
            FxDebug.d("view-lifecycle-> code->addView")
            helper.iFxViewLifecycle?.postAttach()
            getContainer()?.addView(managerView)
            if (isAnimation && helper.enableAnimation && helper.fxAnimation != null) {
                helper.fxAnimation?.fromStartAnimator(managerView)
                FxDebug.d("view->Animation -----start")
            }
        } ?: FxDebug.e("system -> fxParentView==null")
    }

    override fun reset() {
        super.reset()
        FloatingX.reset()
    }
}
