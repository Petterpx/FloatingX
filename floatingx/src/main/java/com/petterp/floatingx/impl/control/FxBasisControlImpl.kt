package com.petterp.floatingx.impl.control

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import com.petterp.floatingx.assist.FxAdsorbDirection
import com.petterp.floatingx.assist.FxAnimation
import com.petterp.floatingx.assist.FxDisplayMode
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxScrollListener
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.listener.control.IFxConfigControl
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.listener.provider.IFxContextProvider
import com.petterp.floatingx.listener.provider.IFxHolderProvider
import com.petterp.floatingx.util.INVALID_LAYOUT_ID
import com.petterp.floatingx.util.lazyLoad
import com.petterp.floatingx.view.FxViewHolder
import com.petterp.floatingx.view.IFxInternalView
import com.petterp.floatingx.view.default.FxDefaultContainerView
import java.lang.ref.WeakReference

/** Fx基础控制器实现 */
open class FxBasisControlImpl(private val helper: FxBasisHelper) : IFxControl, IFxConfigControl {
    private var viewHolder: FxViewHolder? = null
    private var mContainer: WeakReference<ViewGroup>? = null
    private var internalView: IFxInternalView? = null
    private val cancelAnimationRunnable by lazyLoad { Runnable { reset() } }
    private val hideAnimationRunnable by lazyLoad { Runnable { detach() } }

    override val configControl: IFxConfigControl get() = this

    override fun cancel() {
        if ((getManagerView() == null && viewHolder == null)) return
        if (isShow() && helper.enableAnimation && helper.fxAnimation != null) {
            getManagerView()?.removeCallbacks(cancelAnimationRunnable)
            val duration = helper.fxAnimation!!.toEndAnimator(internalView?.containerView)
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
            getManagerView()?.removeCallbacks(hideAnimationRunnable)
            val duration = helper.fxAnimation!!.toEndAnimator(getManagerView())
            animatorCallback(duration, hideAnimationRunnable)
        } else {
            detach()
        }
    }

    override fun isShow(): Boolean {
        val managerView = getManagerView() ?: return false
        return ViewCompat.isAttachedToWindow(managerView) && managerView.visibility == View.VISIBLE
    }

    override fun getView(): View? = internalView?.childView

    override fun getViewHolder(): FxViewHolder? = viewHolder

    override fun getManagerView(): FrameLayout? = internalView?.containerView

    override fun updateView(@LayoutRes resource: Int) {
        check(resource != INVALID_LAYOUT_ID) { "resource cannot be INVALID_LAYOUT_ID!" }
        helper.layoutView = null
        helper.layoutId = resource
        updateMangerView()
    }

    override fun updateView(view: View) {
        helper.layoutId = INVALID_LAYOUT_ID
        helper.layoutView = view
        updateMangerView()
    }

    override fun updateView(provider: IFxContextProvider) {
        updateView(provider.build(context()))
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
        internalView?.moveLocation(x, y, useAnimation)
    }

    override fun moveByVector(x: Float, y: Float, useAnimation: Boolean) {
        internalView?.moveLocationByVector(x, y, useAnimation)
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
        internalView?.moveToEdge()
    }

    override fun setEnableEdgeAdsorption(isEnable: Boolean) {
        helper.enableEdgeAdsorption = isEnable
        internalView?.moveToEdge()
    }

    override fun setEdgeAdsorbDirection(direction: FxAdsorbDirection) {
        helper.adsorbDirection = direction
        internalView?.moveToEdge()
    }

    override fun setEdgeOffset(edgeOffset: Float) {
        helper.edgeOffset = edgeOffset
        internalView?.moveToEdge()
    }

    override fun setEnableEdgeRebound(isEnable: Boolean) {
        helper.enableEdgeRebound = isEnable
        internalView?.moveToEdge()
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
    }

    @JvmSynthetic
    internal fun setContainerGroup(viewGroup: ViewGroup) {
        mContainer = WeakReference(viewGroup)
    }

    /*
    * 以下方法作为基础实现,供子类自行调用
    * */
    private fun updateMangerView() {
        if (getContainerGroup() == null) throw NullPointerException("FloatingX window The parent container cannot be null!")
        internalView?.updateView()
    }

    protected fun initManagerView() {
        check(helper.layoutId != INVALID_LAYOUT_ID || helper.layoutView != null) {
            "The layout id cannot be 0 ,and layoutView==null"
        }
        getContainerGroup()?.removeView(getManagerView())
        initManager()
    }

    protected open fun initManager() {
        val context = context() ?: return
        internalView = FxDefaultContainerView(context).init(helper)
        val fxContentView = internalView?.childView ?: return
        viewHolder = FxViewHolder(fxContentView)
        val fxViewLifecycle = helper.iFxViewLifecycle ?: return
        // 后续此方法将会移除,建议进行过渡
        fxViewLifecycle.initView(fxContentView)
        fxViewLifecycle.initView(viewHolder!!)
    }

    protected fun getOrInitManagerView(): FrameLayout? {
        if (getManagerView() == null) initManagerView()
        return getManagerView()
    }

    protected fun getContainerGroup(): ViewGroup? {
        return mContainer?.get()
    }

    protected open fun detach(container: ViewGroup?) {
        if (internalView != null && container != null) {
            helper.fxLog?.d("fxView-lifecycle-> code->removeView")
            helper.iFxViewLifecycle?.postDetached()
            container.removeView(getManagerView())
        }
    }

    protected fun detach() {
        val containerGroup = getContainerGroup() ?: return
        detach(containerGroup)
    }

    protected open fun context(): Context? {
        val context = mContainer?.get()?.context
        if (context == null) {
            helper.fxLog?.e("context = null,check your rule!")
        }
        return context
    }

    protected fun clearContainer() {
        mContainer?.clear()
        mContainer = null
    }

    protected open fun reset() {
        getManagerView()?.apply {
            removeCallbacks(hideAnimationRunnable)
            removeCallbacks(cancelAnimationRunnable)
        }
        detach(mContainer?.get())
        internalView = null
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

    protected open fun internalShow() {
        val managerView = getManagerView() ?: return
        helper.enableFx = true
        managerView.visibility = View.VISIBLE
        val fxAnimation = helper.fxAnimation ?: return
        if (helper.enableAnimation) {
            if (fxAnimation.fromJobIsRunning()) {
                helper.fxLog?.d("fxView->Animation ,startAnimation Executing, cancel this operation!")
                return
            }
            helper.fxLog?.d("fxView->Animation ,startAnimation Executing, cancel this operation.")
            fxAnimation.fromStartAnimator(managerView)
        }
    }

    private fun animatorCallback(long: Long, runnable: Runnable) {
        val magnetView = getManagerView() ?: return
        magnetView.removeCallbacks(runnable)
        magnetView.postDelayed(runnable, long)
    }
}
