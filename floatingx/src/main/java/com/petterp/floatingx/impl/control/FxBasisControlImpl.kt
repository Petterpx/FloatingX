package com.petterp.floatingx.impl.control

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.listener.control.IFxConfigControl
import com.petterp.floatingx.listener.provider.IFxContextProvider
import com.petterp.floatingx.listener.provider.IFxHolderProvider
import com.petterp.floatingx.util.lazyLoad
import com.petterp.floatingx.view.FxMagnetView
import com.petterp.floatingx.view.FxViewHolder
import java.lang.ref.WeakReference

/** 基础控制器实现 */
abstract class FxBasisControlImpl(private val helper: BasisHelper) : IFxControl, IFxConfigControl {
    private var managerView: FxMagnetView? = null
    private var viewHolder: FxViewHolder? = null
    private var mContainer: WeakReference<ViewGroup>? = null
    private val cancelAnimationRunnable by lazyLoad { Runnable { reset() } }
    private val hideAnimationRunnable by lazyLoad { Runnable { detach() } }

    override val configControl: IFxConfigControl get() = this

    override fun isShow(): Boolean =
        managerView != null && ViewCompat.isAttachedToWindow(managerView!!) && managerView!!.visibility == View.VISIBLE

    override fun getView(): View? = managerView?.childFxView

    override fun getManagerViewHolder(): FxViewHolder? = viewHolder

    override fun getManagerView(): FxMagnetView? = managerView

    override fun updateManagerView(@LayoutRes resource: Int) {
        if (resource == 0) throw IllegalArgumentException("resource cannot be 0!")
        helper.layoutView?.clear()
        helper.layoutView = null
        updateMangerView(resource)
    }

    override fun updateManagerView(view: View) {
        helper.layoutView = WeakReference(view)
        updateMangerView(0)
    }

    override fun updateManagerView(provider: IFxContextProvider) {
        val view = provider.build(context())
        updateManagerView(view)
    }

    override fun updateView(provider: IFxHolderProvider) {
        provider.apply(viewHolder ?: return)
    }

    override fun updateManagerParams(params: ViewGroup.LayoutParams) {
        managerView?.layoutParams = params
    }

    override fun setClickListener(time: Long, clickListener: View.OnClickListener) {
        helper.iFxClickListener = clickListener
        helper.clickTime = time
        helper.enableClickListener = true
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
        if (helper.enableAnimation && helper.fxAnimation != null) {
            if (helper.fxAnimation!!.endJobIsRunning()) {
                helper.fxLog?.d("fxView->Animation ,endAnimation Executing, cancel this operation!")
                return
            }
            helper.fxLog?.d("fxView->Animation ,endAnimation Running")
            managerView?.removeCallbacks(hideAnimationRunnable)
            val duration = helper.fxAnimation!!.toEndAnimator(managerView)
            animatorCallback(duration, hideAnimationRunnable)
        } else detach()
    }

    override fun setBorderMargin(t: Float, l: Float, b: Float, r: Float) {
        super.setBorderMargin(t, l, b, r)
        managerView?.moveToEdge()
    }

    override fun setEdgeOffset(edgeOffset: Float) {
        super.setEdgeOffset(edgeOffset)
        managerView?.moveToEdge()
    }

    override fun setEnableEdgeAdsorption(isEnable: Boolean, lazyStart: Boolean) {
        getConfigHelper().enableEdgeAdsorption = isEnable
        if (isEnable && !lazyStart) managerView?.moveToEdge()
    }

    override fun setEnableEdgeRebound(isEnable: Boolean, lazyStart: Boolean) {
        getConfigHelper().enableEdgeRebound = isEnable
        if (!lazyStart) managerView?.moveToEdge()
    }

    private fun animatorCallback(long: Long, runnable: Runnable) {
        val magnetView = managerView ?: return
        magnetView.removeCallbacks(runnable)
        magnetView.postDelayed(
            runnable,
            long
        )
    }

    /*
    * 以下方法作为基础实现,供子类自行调用
    * */
    protected open fun updateMangerView(@LayoutRes layout: Int = 0) {
        helper.layoutId = layout
        if (getContainerGroup() == null) throw NullPointerException("FloatingX window The parent container cannot be null!")
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
        if (isShow) {
            getContainerGroup()?.addView(managerView)
        }
    }

    protected fun initManagerView() {
        if (helper.layoutId == 0 && helper.layoutView == null) throw RuntimeException("The layout id cannot be 0 ,and layoutView==null")
        getContainerGroup()?.removeView(managerView)
        initManager()
    }

    protected open fun initManager() {
        managerView = FxMagnetView(context(), helper)
        viewHolder = FxViewHolder(managerView!!)
    }

    protected fun getContainerGroup(): ViewGroup? {
        return mContainer?.get()
    }

    protected fun setContainerGroup(viewGroup: ViewGroup) {
        mContainer = WeakReference(viewGroup)
    }

    protected open fun detach(container: ViewGroup?) {
        if (managerView != null && container != null) {
            helper.fxLog?.d("fxView-lifecycle-> code->removeView")
            helper.iFxViewLifecycle?.postDetached()
            container.removeView(managerView)
        }
    }

    protected fun detach() {
        val containerGroup = getContainerGroup() ?: return
        detach(containerGroup)
    }

    protected open fun context(): Context {
        if (mContainer?.get()?.context == null) {
            throw NullPointerException("context cannot be null")
        }
        return mContainer?.get()?.context!!
    }

    protected fun clearContainer() {
        mContainer?.clear()
        mContainer = null
    }

    @JvmSynthetic
    protected fun FxMagnetView.show() {
        helper.enableFx = true
        visibility = View.VISIBLE
        val fxAnimation = helper.fxAnimation ?: return
        if (helper.enableAnimation) {
            if (fxAnimation.fromJobIsRunning()) {
                helper.fxLog?.d("fxView->Animation ,startAnimation Executing, cancel this operation!")
                return
            }
            helper.fxLog?.d("fxView->Animation ,startAnimation Executing, cancel this operation.")
            fxAnimation.fromStartAnimator(this)
        }
    }

    protected open fun reset() {
        helper.enableFx = false
        helper.fxAnimation?.cancelAnimation()
        managerView?.removeCallbacks(hideAnimationRunnable)
        managerView?.removeCallbacks(cancelAnimationRunnable)
        detach(mContainer?.get())
        managerView = null
        viewHolder = null
        clearContainer()
        helper.fxLog?.d("fxView-lifecycle-> code->cancelFx")
    }
}
