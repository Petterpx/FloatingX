package com.petterp.floatingx.impl.control

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import com.petterp.floatingx.assist.FxAnimation
import com.petterp.floatingx.assist.FxDisplayMode
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxScrollListener
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.listener.control.IFxConfigControl
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.listener.provider.IFxContextProvider
import com.petterp.floatingx.listener.provider.IFxHolderProvider
import com.petterp.floatingx.util.lazyLoad
import com.petterp.floatingx.view.FxManagerView
import com.petterp.floatingx.view.FxViewHolder
import java.lang.ref.WeakReference

/** Fx基础控制器实现 */
open class FxBasisControlImpl(private val helper: BasisHelper) : IFxControl, IFxConfigControl {
    private var managerView: FxManagerView? = null
    private var viewHolder: FxViewHolder? = null
    private var mContainer: WeakReference<ViewGroup>? = null
    private val cancelAnimationRunnable by lazyLoad { Runnable { reset() } }
    private val hideAnimationRunnable by lazyLoad { Runnable { detach() } }

    /*
    * 控制接口相关实现
    * */
    override val configControl: IFxConfigControl get() = this

    override fun cancel() {
        if ((managerView == null && viewHolder == null)) return
        if (isShow() && helper.enableAnimation && helper.fxAnimation != null) {
            managerView?.removeCallbacks(cancelAnimationRunnable)
            val duration = helper.fxAnimation!!.toEndAnimator(managerView)
            animatorCallback(duration, cancelAnimationRunnable)
        } else {
            reset()
        }
    }

    override fun hide() {
        if (!isShow()) return
        updateEnableStatus(false)
        if (helper.enableAnimation && helper.fxAnimation != null) {
            if (helper.fxAnimation!!.endJobIsRunning()) {
                helper.fxLog?.d("fxView->Animation ,endAnimation Executing, cancel this operation!")
                return
            }
            helper.fxLog?.d("fxView->Animation ,endAnimation Running")
            managerView?.removeCallbacks(hideAnimationRunnable)
            val duration = helper.fxAnimation!!.toEndAnimator(managerView)
            animatorCallback(duration, hideAnimationRunnable)
        } else {
            detach()
        }
    }

    override fun isShow(): Boolean =
        managerView != null && ViewCompat.isAttachedToWindow(managerView!!) && managerView!!.visibility == View.VISIBLE

    override fun getView(): View? = managerView?.childFxView

    override fun getViewHolder(): FxViewHolder? = viewHolder

    override fun getManagerView(): FxManagerView? = managerView

    override fun updateView(@LayoutRes resource: Int) {
        if (resource == 0) throw IllegalArgumentException("resource cannot be 0!")
        helper.layoutView = null
        updateMangerView(resource)
    }

    override fun updateView(view: View) {
        helper.layoutView = view
        updateMangerView(0)
    }

    override fun updateView(provider: IFxContextProvider) {
        val view = provider.build(context())
        updateView(view)
    }

    override fun updateViewContent(provider: IFxHolderProvider) {
        provider.apply(viewHolder ?: return)
    }

    override fun setClickListener(time: Long, clickListener: View.OnClickListener) {
        helper.clickTime = time
        helper.enableClickListener = true
        helper.iFxClickListener = clickListener
    }

    override fun setClickListener(clickListener: View.OnClickListener) {
        setClickListener(0, clickListener)
    }

    override fun move(x: Float, y: Float) {
        move(x, y, true)
    }

    override fun moveByVector(x: Float, y: Float) {
        moveByVector(x, y, true)
    }

    override fun move(x: Float, y: Float, useAnimation: Boolean) {
        managerView?.moveLocation(x, y, useAnimation)
    }

    override fun moveByVector(x: Float, y: Float, useAnimation: Boolean) {
        managerView?.moveLocationByVector(x, y, useAnimation)
    }

    /*
    * config配置相关接口实现
    * */
    override fun setEnableClick(isEnable: Boolean) {
        helper.enableClickListener = isEnable
    }

    override fun setEnableAnimation(isEnable: Boolean, animationImpl: FxAnimation) {
        helper.enableAnimation = isEnable
        helper.fxAnimation = animationImpl
    }

    override fun setEnableAnimation(isEnable: Boolean) {
        helper.enableAnimation = isEnable
    }

    override fun setBorderMargin(t: Float, l: Float, b: Float, r: Float) {
        helper.fxBorderMargin.apply {
            this.t = t
            this.l = l
            this.b = b
            this.r = r
        }
        managerView?.moveToEdge()
    }

    override fun setEdgeOffset(edgeOffset: Float) {
        helper.edgeOffset = edgeOffset
        managerView?.moveToEdge()
    }

    override fun setEnableEdgeRebound(isEnable: Boolean) {
        helper.enableEdgeRebound = isEnable
        managerView?.moveToEdge()
    }

    override fun setScrollListener(listener: IFxScrollListener) {
        helper.iFxScrollListener = listener
    }

    override fun setViewLifecycleListener(listener: IFxViewLifecycle) {
        helper.iFxViewLifecycle = listener
    }

    override fun setEnableSaveDirection(impl: IFxConfigStorage, isEnable: Boolean) {
        helper.iFxConfigStorage = impl
        helper.enableSaveDirection = isEnable
    }

    override fun setEnableSaveDirection(isEnable: Boolean) {
        helper.enableSaveDirection = isEnable
    }

    override fun clearLocationStorage() {
        helper.iFxConfigStorage?.clear()
    }

    override fun setEnableTouch(isEnable: Boolean) {
        val mode = if (isEnable) FxDisplayMode.Normal else FxDisplayMode.ClickOnly
        setDisplayMode(mode)
    }

    override fun setDisplayMode(mode: FxDisplayMode) {
        helper.displayMode = mode
        managerView?.updateDisplayMode()
    }

    override fun setEnableEdgeAdsorption(isEnable: Boolean) {
        helper.enableEdgeAdsorption = isEnable
    }

    /*
    * 以下方法作为基础实现,供子类自行调用
    * */
    protected open fun updateMangerView(@LayoutRes layout: Int = 0) {
        helper.layoutId = layout
        if (getContainerGroup() == null) throw NullPointerException("FloatingX window The parent container cannot be null!")
        val x = managerView?.x ?: 0f
        val y = managerView?.y ?: 0f
        initManagerView()
        managerView?.restoreLocation(x, y)
        getContainerGroup()?.addView(managerView)
    }

    protected fun initManagerView() {
        if (helper.layoutId == 0 && helper.layoutView == null) throw RuntimeException("The layout id cannot be 0 ,and layoutView==null")
        getContainerGroup()?.removeView(managerView)
        initManager()
    }

    protected open fun initManager() {
        managerView = FxManagerView(context()).init(helper)
        val fxContentView = managerView?.childFxView ?: return
        viewHolder = FxViewHolder(fxContentView)
        val fxViewLifecycle = helper.iFxViewLifecycle ?: return
        // 后续此方法将会移除,建议进行过渡
        fxViewLifecycle.initView(fxContentView)
        fxViewLifecycle.initView(viewHolder!!)
    }

    protected fun getOrInitManagerView(): FxManagerView? {
        if (managerView == null) initManagerView()
        return managerView
    }

    protected fun getContainerGroup(): ViewGroup? {
        return mContainer?.get()
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
    protected fun FxManagerView.show() {
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

    @JvmSynthetic
    protected open fun reset() {
        managerView?.removeCallbacks(hideAnimationRunnable)
        managerView?.removeCallbacks(cancelAnimationRunnable)
        detach(mContainer?.get())
        managerView = null
        viewHolder = null
        helper.clear()
        clearContainer()
        helper.fxLog?.d("fxView-lifecycle-> code->cancelFx")
    }

    /** 更新启用状态 */
    protected fun updateEnableStatus(newStatus: Boolean) {
        if (helper.enableFx == newStatus) return
        helper.enableFx = newStatus
    }

    internal fun setContainerGroup(viewGroup: ViewGroup) {
        mContainer = WeakReference(viewGroup)
    }

    private fun animatorCallback(long: Long, runnable: Runnable) {
        val magnetView = managerView ?: return
        magnetView.removeCallbacks(runnable)
        magnetView.postDelayed(runnable, long)
    }
}
