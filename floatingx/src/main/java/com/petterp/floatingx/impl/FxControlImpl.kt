package com.petterp.floatingx.impl

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.petterp.floatingx.assist.FxHelper
import com.petterp.floatingx.config.SystemConfig
import com.petterp.floatingx.ext.FxDebug
import com.petterp.floatingx.ext.fxParentView
import com.petterp.floatingx.ext.lazyLoad
import com.petterp.floatingx.ext.show
import com.petterp.floatingx.ext.topActivity
import com.petterp.floatingx.listener.IFxControl
import com.petterp.floatingx.view.FxMagnetView
import com.petterp.floatingx.view.FxViewHolder
import java.lang.ref.WeakReference

/**
 * @Author petterp
 * @Date 2021/5/21-2:24 下午
 * @Email ShiyihuiCloud@163.com
 * @Function fx基础控制器实现类
 */
open class FxControlImpl(private val helper: FxHelper) : IFxControl {

    private var managerView: FxMagnetView? = null
    private var viewHolder: FxViewHolder? = null
    private var mContainer: WeakReference<FrameLayout>? = null
    private val managerViewOrContainerIsNull: Boolean
        get() = mContainer == null && managerView == null
    private val cancelAnimationRunnable by lazyLoad {
        Runnable {
            cancelFx()
        }
    }
    private val hideAnimationRunnable by lazyLoad {
        Runnable {
            mContainer?.get()?.let { detach(it) }
        }
    }

    override fun show(isAnimation: Boolean) {
        if (isShowRunning()) return
        attach(topActivity!!)
        managerView?.show(isAnimation)
    }

    override fun show(activity: Activity, isAnimation: Boolean) {
        attach(activity)
        managerView?.show(isAnimation)
    }

    override fun hide(isAnimation: Boolean) {
        if (!isShowRunning()) return
        if (isAnimation && helper.enableAnimation &&
            helper.fxAnimation != null
        ) {
            if (helper.fxAnimation.endJobRunning) {
                FxDebug.d("view->Animation ,endAnimation Executing, cancel this operation!")
                return
            }
            FxDebug.d("view->Animation ,endAnimation Running")
            managerView?.removeCallbacks(hideAnimationRunnable)
            val duration = helper.fxAnimation.toEndAnimator(managerView)
            animatorCallback(duration, hideAnimationRunnable)
        }
    }

    override fun getView(): View? = managerView

    override fun isShowRunning(): Boolean =
        managerView != null && ViewCompat.isAttachedToWindow(managerView!!) && managerView?.isVisible == true

    override

    fun updateView(obj: (FxViewHolder) -> Unit) {
        viewHolder?.let(obj)
    }

    override fun updateView(@DrawableRes resource: Int) {
        initManagerView(resource)
    }

    override fun updateParams(params: ViewGroup.LayoutParams) {
        managerView?.layoutParams = params
    }

    override fun attach(activity: Activity) {
        SystemConfig.updateConfig(activity)
        activity.fxParentView?.let {
            attach(it)
        } ?: FxDebug.e("system -> fxParentView==null")
    }

    // 安装到fragmentLayout上
    override fun attach(container: FrameLayout) {
        if (managerView?.parent === container) {
            return
        }
        var isAnimation = false
        if (managerView == null) {
            initManagerView()
            isAnimation = true
        } else {
            if (managerView?.isVisible == false) managerView?.isVisible = true
            removeManagerView(getContainer())
        }
        mContainer = WeakReference(container)
        FxDebug.d("view-lifecycle-> code->addView")
        helper.iFxViewLifecycle?.postAttach()
        getContainer()?.addView(managerView)
        if (isAnimation && helper.enableAnimation && helper.fxAnimation != null) {
            helper.fxAnimation.fromStartAnimator(managerView)
            FxDebug.d("view->Animation -----start")
        }
    }

    /** 删除view */
    override fun detach(activity: Activity) {
        if (managerViewOrContainerIsNull) return
        activity.fxParentView?.let {
            detach(it)
        }
    }

    override fun detach(container: FrameLayout) {
        if (managerView != null && ViewCompat.isAttachedToWindow(managerView!!)) {
            removeManagerView(container)
        }
        if (container === getContainer()) {
            mContainer = null
        }
    }

    override fun setClickListener(time: Long, obj: (View) -> Unit) {
        helper.clickListener = obj
        helper.clickTime = time
    }

    override fun dismiss() {
        FxDebug.d("view->dismiss-----------")
        if (helper.enableAnimation && helper.fxAnimation != null) {
            val endDuration = helper.fxAnimation.toEndAnimator(managerView)
            animatorCallback(endDuration, cancelAnimationRunnable)
            FxDebug.d("view->Animation -----end")
        } else {
            cancelFx()
        }
    }

    private fun animatorCallback(long: Long, runnable: Runnable) {
        managerView?.removeCallbacks(runnable)
        managerView?.postDelayed(
            runnable,
            long
        )
    }

    private fun cancelFx() {
        helper.enableFx = false
        mContainer?.get()?.let {
            detach(it)
        }
        managerView?.removeCallbacks(hideAnimationRunnable)
        managerView?.removeCallbacks(cancelAnimationRunnable)
        helper.fxAnimation?.cancelAnimation()
        managerView = null
        viewHolder = null
        FxDebug.d("view-lifecycle-> code->cancelFx")
    }

    private fun removeManagerView(container: FrameLayout?) {
        if (container == null) return
        FxDebug.d("view-lifecycle-> code->removeView")
        helper.iFxViewLifecycle?.postDetached()
        container.removeView(managerView)
    }

    open fun initManagerView(@DrawableRes layout: Int = 0) {
        if (layout != 0) helper.layoutId = layout
        if (helper.layoutId == 0) throw RuntimeException("The layout id cannot be 0")
        managerView = FxMagnetView(helper)
        ViewCompat.setOnApplyWindowInsetsListener(managerView!!, windowsInsetsListener)
        managerView?.requestApplyInsets()
        viewHolder = FxViewHolder(managerView!!)
    }

    @SuppressLint("WrongConstant")
    val windowsInsetsListener: OnApplyWindowInsetsListener =
        OnApplyWindowInsetsListener { _, insets ->
            FxDebug.v("System--StatusBar---old-(${SystemConfig.statsBarHeight}),new-(${insets.systemWindowInsetTop})")
            SystemConfig.statsBarHeight = insets.systemWindowInsetTop
            insets
        }

    private fun getContainer(): FrameLayout? {
        return mContainer?.get()
    }
}
