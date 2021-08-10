package com.petterp.floatingx.impl.control

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.listener.control.IFxHelperControl
import com.petterp.floatingx.util.lazyLoad
import com.petterp.floatingx.view.FxMagnetView
import com.petterp.floatingx.view.FxViewHolder
import java.lang.ref.WeakReference

/** 基础控制器实现 */
abstract class FxBasisControlImpl(private val helper: BasisHelper) : IFxControl, IFxHelperControl {
    private var managerView: FxMagnetView? = null
    private var viewHolder: FxViewHolder? = null
    protected var mContainer: WeakReference<ViewGroup>? = null

    private val cancelAnimationRunnable by lazyLoad { Runnable { reset() } }
    private val hideAnimationRunnable by lazyLoad { Runnable { detach() } }

    override val helperControl: IFxHelperControl
        get() = this

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

    override fun getConfigHelper(): BasisHelper {
        return helper
    }

    override fun show() {
        if (!helper.enableFx) helper.enableFx = true
    }

    override fun cancel() {
        if (managerView == null && viewHolder == null) return
        if (helper.enableAnimation &&
            helper.fxAnimation != null
        ) {
            managerView?.removeCallbacks(cancelAnimationRunnable)
            val duration = helper.fxAnimation!!.toEndAnimator(managerView)
            animatorCallback(duration, cancelAnimationRunnable)
        } else reset()
    }

    override fun hide() {
        if (!isShow()) return
        helper.enableFx = false
        if (helper.enableAnimation &&
            helper.fxAnimation != null
        ) {
            if (helper.fxAnimation!!.endJobRunning) {
                helper.fxLog?.d("view->Animation ,endAnimation Executing, cancel this operation!")
                return
            }
            helper.fxLog?.d("view->Animation ,endAnimation Running")
            managerView?.removeCallbacks(hideAnimationRunnable)
            val duration = helper.fxAnimation!!.toEndAnimator(managerView)
            animatorCallback(duration, hideAnimationRunnable)
        } else detach()
    }

    override fun setBorderMargin(t: Float, l: Float, b: Float, r: Float) {
        super.setBorderMargin(t, l, b, r)
        managerView?.fixLocation()
    }

    override fun setEdgeOffset(edgeOffset: Float) {
        super.setEdgeOffset(edgeOffset)
        managerView?.moveToEdge()
    }

    override fun setEnableEdgeAdsorption(enable: Boolean, lazyStart: Boolean) {
        super.setEnableEdgeAdsorption(enable, lazyStart)
        if (enable && !lazyStart) managerView?.moveToEdge()
    }

    /*
    * 以下方法作为基础实现,供子类自行调用
    * */

    protected open fun updateMangerView(@DrawableRes layout: Int = 0) {
        if (layout != 0) helper.layoutId = layout
        if (getContainer() == null) throw NullPointerException("FloatingX window The parent container cannot be null!")
        val isShow = isShow()
        if (helper.iFxConfigStorage?.hasConfig() != true) {
            val x = managerView?.x ?: 0f
            val y = managerView?.y ?: 0f
            initManagerView()
            managerView?.updateLocation(x, y)
        } else {
            initManagerView()
        }
        // 如果当前显示,再添加到parent里
        if (isShow)
            getContainer()?.addView(managerView)
    }

    protected fun initManagerView() {
        if (helper.layoutId == 0) throw RuntimeException("The layout id cannot be 0")
        getContainer()?.removeView(managerView)
        viewHolder?.clear()
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
        viewHolder?.clear()
        viewHolder = null
        clearContainer()
        helper.fxLog?.d("view-lifecycle-> code->cancelFx")
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

    protected open fun detach(container: ViewGroup?) {
        if (managerView != null && container != null) {
            helper.fxLog?.d("view-lifecycle-> code->removeView")
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
        helper.enableFx = true
        isVisible = true
        if (helper.enableAnimation &&
            helper.fxAnimation != null && !helper.fxAnimation!!.fromJobRunning
        ) {
            if (helper.fxAnimation?.fromJobRunning == true) {
                helper.fxLog?.d("view->Animation ,startAnimation Executing, cancel this operation!")
                return
            }
            helper.fxLog?.d("view->Animation ,startAnimation Executing, cancel this operation.")
            helper.fxAnimation?.fromStartAnimator(this)
        }
    }
}
