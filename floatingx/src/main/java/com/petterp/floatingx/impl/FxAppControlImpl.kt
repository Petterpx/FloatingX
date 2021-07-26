package com.petterp.floatingx.impl

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.FxHelper
import com.petterp.floatingx.config.SystemConfig
import com.petterp.floatingx.listener.IFxAppControl
import com.petterp.floatingx.util.FxDebug
import com.petterp.floatingx.util.fxParentView
import com.petterp.floatingx.util.lazyLoad
import com.petterp.floatingx.util.show
import com.petterp.floatingx.util.topActivity
import com.petterp.floatingx.view.FxMagnetView
import com.petterp.floatingx.view.FxViewHolder
import java.lang.ref.WeakReference

/**
 * @Author petterp
 * @Date 2021/5/21-2:24 下午
 * @Email ShiyihuiCloud@163.com
 * @Function fx基础控制器实现类
 */
open class FxAppControlImpl(private val helper: FxHelper) : IFxAppControl {

    private var managerView: FxMagnetView? = null
    private var viewHolder: FxViewHolder? = null
    private var mContainer: WeakReference<FrameLayout>? = null

    private val managerViewOrContainerIsNull: Boolean
        get() = mContainer == null && managerView == null

    private val windowsInsetsListener: OnApplyWindowInsetsListener =
        OnApplyWindowInsetsListener { _, insets ->
            FxDebug.v("System--StatusBar---old-(${SystemConfig.statsBarHeight}),new-(${insets.systemWindowInsetTop})")
            SystemConfig.statsBarHeight = insets.systemWindowInsetTop
            insets
        }
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

    override fun getManagerView(): View? = managerView

    override fun getView(): View? = managerView?.childView

    override fun show(isAnimation: Boolean) {
        if (isShowRunning()) return
        attach(topActivity!!)
        managerView?.show(isAnimation)
    }

    override fun show(activity: Activity, isAnimation: Boolean) {
        if (isShowRunning()) return
        attach(activity)
        managerView?.show(isAnimation)
    }

    override fun isShowRunning(): Boolean =
        managerView != null && ViewCompat.isAttachedToWindow(managerView!!) && managerView?.isVisible == true

    override fun updateView(obj: (FxViewHolder) -> Unit) {
        viewHolder?.let(obj)
    }

    override fun updateView(@DrawableRes resource: Int) {
        initManagerView(resource)
    }

    override fun updateParams(params: ViewGroup.LayoutParams) {
        managerView?.layoutParams = params
    }

    override fun attach(activity: Activity) {
        activity.fxParentView?.let {
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
                helper.fxAnimation.fromStartAnimator(managerView)
                FxDebug.d("view->Animation -----start")
            }
        } ?: FxDebug.e("system -> fxParentView==null")
    }

    /** 删除view */
    override fun detach(activity: Activity) {
        if (managerViewOrContainerIsNull) return
        activity.fxParentView?.let {
            detach(it)
        }
    }

    private fun detach(container: FrameLayout) {
        if (managerView != null && ViewCompat.isAttachedToWindow(managerView!!)) {
            removeManagerView(container)
        }
        if (container === getContainer()) {
            mContainer = null
        }
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
        } else cancelFx()
    }

    override fun cancel(isAnimation: Boolean) {
        if (isAnimation && helper.enableAnimation &&
            helper.fxAnimation != null
        ) {
            managerView?.removeCallbacks(hideAnimationRunnable)
            val duration = helper.fxAnimation.toEndAnimator(managerView)
            animatorCallback(duration, hideAnimationRunnable)
        } else cancelFx()
    }

    override fun setClickListener(time: Long, obj: (View) -> Unit) {
        helper.clickListener = obj
        helper.clickTime = time
    }

    private fun animatorCallback(long: Long, runnable: Runnable) {
        managerView?.removeCallbacks(runnable)
        managerView?.postDelayed(
            runnable,
            long
        )
    }

    open fun initManagerView(@DrawableRes layout: Int = 0) {
        if (layout != 0) helper.layoutId = layout
        if (helper.layoutId == 0) throw RuntimeException("The layout id cannot be 0")
        managerView = FxMagnetView(helper)
        ViewCompat.setOnApplyWindowInsetsListener(managerView!!, windowsInsetsListener)
        managerView?.requestApplyInsets()
        viewHolder = FxViewHolder(managerView!!)
    }

    private fun getContainer(): FrameLayout? {
        return mContainer?.get()
    }

    private fun cancelFx() {
        helper.enableFx = false
        helper.fxAnimation?.cancelAnimation()
        managerView?.removeCallbacks(hideAnimationRunnable)
        managerView?.removeCallbacks(cancelAnimationRunnable)
        mContainer?.get()?.let {
            detach(it)
        }
        managerView = null
        viewHolder = null
        FloatingX.reset()
        FxDebug.d("view-lifecycle-> code->cancelFx")
    }

    private fun removeManagerView(container: FrameLayout?) {
        if (container == null) return
        FxDebug.d("view-lifecycle-> code->removeView")
        helper.iFxViewLifecycle?.postDetached()
        container.removeView(managerView)
    }
}
