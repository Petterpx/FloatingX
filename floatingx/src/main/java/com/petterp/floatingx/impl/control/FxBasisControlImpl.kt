package com.petterp.floatingx.impl.control

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.petterp.floatingx.assist.helper.BaseHelper
import com.petterp.floatingx.listener.IFxControl
import com.petterp.floatingx.util.FxDebug
import com.petterp.floatingx.util.decorView
import com.petterp.floatingx.util.lazyLoad
import com.petterp.floatingx.util.show
import com.petterp.floatingx.view.FxMagnetView
import com.petterp.floatingx.view.FxViewHolder
import java.lang.ref.WeakReference

/**
 * @Author petterp
 * @Date 2021/7/28-3:10 PM
 * @Email ShiyihuiCloud@163.com
 * @Function 基础控制器实现
 */
open class FxBasisControlImpl(private val helper: BaseHelper) : IFxControl {
    protected var managerView: FxMagnetView? = null
    private var viewHolder: FxViewHolder? = null
    protected var mContainer: WeakReference<ViewGroup>? = null

    private val managerViewOrContainerIsNull: Boolean
        get() = mContainer == null && managerView == null

    private val cancelAnimationRunnable by lazyLoad {
        Runnable {
            reset()
        }
    }
    private val hideAnimationRunnable by lazyLoad {
        Runnable {
            mContainer?.get()?.let { detach(it) }
        }
    }

    override fun show(activity: Activity, isAnimation: Boolean) {
        if (isShow() && activity.decorView === getContainer()) return
        detach(activity)
        attach(activity)
        managerView?.show(isAnimation)
    }

    override fun show(container: ViewGroup, isAnimation: Boolean) {
        if (isShow() && container === getContainer()) return
        detach(container)
        attach(container)
        managerView?.show(isAnimation)
    }

    override fun isShow(): Boolean =
        managerView != null && ViewCompat.isAttachedToWindow(managerView!!) && managerView?.isVisible == true

    override fun updateView(obj: (FxViewHolder) -> Unit) {
        viewHolder?.let(obj)
    }

    override fun getManagerView(): View? = managerView

    override fun getView(): View? = managerView?.childView

    override fun updateView(@DrawableRes resource: Int) {
        initManagerView(resource)
    }

    override fun updateParams(params: ViewGroup.LayoutParams) {
        managerView?.layoutParams = params
    }

    override fun hide(isAnimation: Boolean) {
        if (!isShow()) return
        if (isAnimation && helper.enableAnimation &&
            helper.fxAnimation != null
        ) {
            if (helper.fxAnimation!!.endJobRunning) {
                FxDebug.d("view->Animation ,endAnimation Executing, cancel this operation!")
                return
            }
            FxDebug.d("view->Animation ,endAnimation Running")
            managerView?.removeCallbacks(hideAnimationRunnable)
            val duration = helper.fxAnimation!!.toEndAnimator(managerView)
            animatorCallback(duration, hideAnimationRunnable)
        } else mContainer?.get()?.let { detach(it) }
    }

    override fun cancel(isAnimation: Boolean) {
        if (isAnimation && helper.enableAnimation &&
            helper.fxAnimation != null
        ) {
            managerView?.removeCallbacks(cancelAnimationRunnable)
            val duration = helper.fxAnimation!!.toEndAnimator(managerView)
            animatorCallback(duration, cancelAnimationRunnable)
        } else reset()
    }

    override fun setClickListener(time: Long, obj: (View) -> Unit) {
        helper.clickListener = obj
        helper.clickTime = time
    }

    /*
    * 提供给子类,便于定制的方法
    * */
    protected open fun context(): Context {
        if (mContainer?.get()?.context == null)
            throw NullPointerException("context cannot be empty")
        return mContainer?.get()?.context!!
    }

    protected open fun initManagerView(@DrawableRes layout: Int = 0) {
        if (layout != 0) helper.layoutId = layout
        if (helper.layoutId == 0) throw RuntimeException("The layout id cannot be 0")
        managerView = FxMagnetView(context(), helper)
        viewHolder = FxViewHolder(managerView!!)
    }

    protected open fun reset() {
        helper.enableFx = false
        helper.fxAnimation?.cancelAnimation()
        managerView?.removeCallbacks(hideAnimationRunnable)
        managerView?.removeCallbacks(cancelAnimationRunnable)
        mContainer?.get()?.let {
            detach(it)
        }
        managerView = null
        viewHolder = null
        FxDebug.d("view-lifecycle-> code->cancelFx")
    }

    internal open fun attach(activity: Activity) {
        attach(activity.decorView)
    }

    /*
    * 以下方法不应让用户可调用,作为基础实现方法,用户不应该关心这些实现
    * */
    private fun animatorCallback(long: Long, runnable: Runnable) {
        managerView?.removeCallbacks(runnable)
        managerView?.postDelayed(
            runnable,
            long
        )
    }

    internal fun getContainer(): ViewGroup? {
        return mContainer?.get()
    }

    private fun attach(container: ViewGroup?) {
        container?.let {
            if (managerView == null || managerView?.parent !== it) {
                mContainer = WeakReference(it)
                initManagerView()
                FxDebug.d("view-lifecycle-> code->addView")
                helper.iFxViewLifecycle?.postAttach()
                getContainer()?.addView(managerView)
            } else {
                FxDebug.d("view-attach-> Repeat installation, skip this operation")
            }
        } ?: FxDebug.e("system -> fxParentView==null")
    }

    internal fun detach(activity: Activity) {
        if (managerViewOrContainerIsNull) return
        activity.decorView?.let {
            detach(it)
        }
    }

    private fun detach(container: ViewGroup) {
        if (managerView != null && ViewCompat.isAttachedToWindow(managerView!!)) {
            removeManagerView(container)
        }
    }

    internal fun removeManagerView(container: ViewGroup?) {
        if (container == null) return
        FxDebug.d("view-lifecycle-> code->removeView")
        helper.iFxViewLifecycle?.postDetached()
        container.removeView(managerView)
    }
}
