package com.petterp.floatingx.impl.control

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.petterp.floatingx.assist.helper.BaseHelper
import com.petterp.floatingx.listener.IFxControl
import com.petterp.floatingx.util.FxDebug
import com.petterp.floatingx.util.lazyLoad
import com.petterp.floatingx.view.FxMagnetView
import com.petterp.floatingx.view.FxViewHolder
import java.lang.ref.WeakReference

/** 基础控制器实现 */
abstract class FxBasisControlImpl(private val helper: BaseHelper) : IFxControl {
    private var managerView: FxMagnetView? = null
    private var viewHolder: FxViewHolder? = null
    protected var mContainer: WeakReference<ViewGroup>? = null

    private val cancelAnimationRunnable by lazyLoad { Runnable { reset() } }
    private val hideAnimationRunnable by lazyLoad { Runnable { detach() } }

    override fun isShow(): Boolean =
        managerView != null && ViewCompat.isAttachedToWindow(managerView!!) && managerView?.isVisible == true

    override fun getView(): View? = managerView?.childView

    override fun getManagerView(): FxMagnetView? = managerView

    override fun getManagerViewHolder(): FxViewHolder? = viewHolder

    override fun updateManagerView(@DrawableRes resource: Int) {
        updateMangerView(resource)
    }

    override fun updateView(obj: (FxViewHolder) -> Unit) {
        viewHolder?.let(obj)
    }

    override fun updateParams(params: ViewGroup.LayoutParams) {
        managerView?.layoutParams = params
    }

    override fun setClickListener(time: Long, obj: (View) -> Unit) {
        helper.clickListener = obj
        helper.clickTime = time
    }

    override fun hide() {
        if (!isShow()) return
        if (helper.enableAnimation &&
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
        } else detach()
    }

    override fun cancel() {
        if (helper.enableAnimation &&
            helper.fxAnimation != null
        ) {
            managerView?.removeCallbacks(cancelAnimationRunnable)
            val duration = helper.fxAnimation!!.toEndAnimator(managerView)
            animatorCallback(duration, cancelAnimationRunnable)
        } else reset()
    }

    /*
    * 以下方法作为基础实现,供子类自行调用
    * */

    protected open fun updateMangerView(@DrawableRes layout: Int = 0) {
        if (layout != 0) helper.layoutId = layout
        initManagerView()
    }

    protected fun initManagerView() {
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
        clearContainer()
        FxDebug.d("view-lifecycle-> code->cancelFx")
    }

    private fun animatorCallback(long: Long, runnable: Runnable) {
        managerView?.removeCallbacks(runnable)
        managerView?.postDelayed(
            runnable,
            long
        )
    }

    protected fun getContainer(): ViewGroup? {
        return mContainer?.get()
    }

    protected fun attach(container: ViewGroup?) {
        container?.let {
            when {
                managerView == null -> {
                    updateMangerView()
                }
                it === getContainer() -> {
                    FxDebug.d("view-attach-> Repeat installation, skip this operation")
                    return
                }
                else -> {
                    detach(it)
                }
            }
            FxDebug.d("view-attach-> code->addView")
            mContainer = WeakReference(it)
            helper.iFxViewLifecycle?.postAttach()
            getContainer()?.addView(managerView)
        } ?: FxDebug.e("system -> fxParentView==null")
    }

    protected open fun detach(container: ViewGroup?) {
        if (managerView != null && container != null) {
            FxDebug.d("view-lifecycle-> code->removeView")
            helper.iFxViewLifecycle?.postDetached()
            container.removeView(managerView)
        }
    }

    protected fun detach() {
        getContainer()?.let {
            detach(it)
        }
    }

    protected open fun context(): Context {
        if (mContainer?.get()?.context == null)
            throw NullPointerException("context cannot be empty")
        return mContainer?.get()?.context!!
    }

    protected fun clearContainer() {
        mContainer?.clear()
        mContainer = null
    }

    protected fun FxMagnetView.show() {
        isVisible = true
        if (helper.enableAnimation &&
            helper.fxAnimation != null && !helper.fxAnimation!!.fromJobRunning
        ) {
            if (helper.fxAnimation?.fromJobRunning == true) {
                FxDebug.d("view->Animation ,startAnimation Executing, cancel this operation!")
                return
            }
            FxDebug.d("view->Animation ,startAnimation Executing, cancel this operation.")
            helper.fxAnimation?.fromStartAnimator(this)
        }
    }
}
