package com.petterp.floatingx.imp

import android.view.View
import androidx.annotation.LayoutRes
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.listener.control.IFxConfigControl
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.listener.provider.IFxAnimationProvider
import com.petterp.floatingx.listener.provider.IFxContextProvider
import com.petterp.floatingx.listener.provider.IFxHolderProvider
import com.petterp.floatingx.listener.provider.IFxPlatformProvider
import com.petterp.floatingx.util.INVALID_LAYOUT_ID
import com.petterp.floatingx.view.FxBasicContainerView
import com.petterp.floatingx.view.IFxInternalHelper

/**
 * Fx基础控制器,用于协调各provider的分发
 * */
abstract class FxBasisControlImp<F : FxBasisHelper, P : IFxPlatformProvider<F>>(
    protected val helper: F
) : IFxControl {

    protected lateinit var platformProvider: P
    private lateinit var _configControl: IFxConfigControl
    private lateinit var _animationProvider: IFxAnimationProvider
    
    // Queue for operations that are called before the floating window is ready
    private val pendingOperations = mutableListOf<FxQueuedOperation>()
    
    private val internalView: IFxInternalHelper?
        get() = platformProvider.internalView

    override val configControl: IFxConfigControl get() = _configControl
    override fun getX() = getManagerView()?.x ?: 0f
    override fun getY() = getManagerView()?.y ?: 0f
    override fun isShow() = platformProvider.isShow()
    override fun getView() = internalView?.childView
    override fun getViewHolder() = internalView?.viewHolder
    override fun getManagerView() = internalView?.containerView

    abstract fun createPlatformProvider(f: F): P
    open fun createConfigProvider(f: F, p: P): IFxConfigControl = FxBasicConfigProvider(f, p)
    open fun createAnimationProvider(f: F, p: P): IFxAnimationProvider = FxBasicAnimationProvider(f)

    fun initProvider() {
        platformProvider = createPlatformProvider(helper)
        _animationProvider = createAnimationProvider(helper, platformProvider)
        _configControl = createConfigProvider(helper, platformProvider)
    }

    override fun show() {
        if (isShow()) return
        helper.enableFx = true
        if (!platformProvider.checkOrInit()) {
            // Queue the show operation for later execution
            queueOperation(FxQueuedOperation.Show())
            return
        }
        executeShowInternal()
    }
    
    private fun executeShowInternal() {
        val fxView = getManagerView() ?: return
        platformProvider.show()
        helper.fxLog.d("fxView -> showFx")
        if (_animationProvider.canRunAnimation()) {
            _animationProvider.start(fxView)
        }
    }

    override fun hide() {
        // 这里同时增加判断状态,因为有可能view正在等待postAttach
        if (!isShow()) {
            // If not showing but we have a valid floating window, queue the hide operation
            if (getManagerView() != null) {
                executeHideInternal()
            } else {
                queueOperation(FxQueuedOperation.Hide())
            }
            return
        }
        executeHideInternal()
    }
    
    private fun executeHideInternal() {
        helper.enableFx = false
        val fxView = getManagerView() ?: return
        helper.fxLog.d("fxView -> hideFx")
        if (_animationProvider.canCancelAnimation()) {
            _animationProvider.hide(fxView) {
                platformProvider.hide()
            }
        } else {
            platformProvider.hide()
        }
    }

    override fun cancel() {
        val fxView = getManagerView()
        (fxView as? FxBasicContainerView)?.preCancelAction()
        if (fxView != null && isShow() && _animationProvider.canRunAnimation()) {
            _animationProvider.hide(fxView) {
                reset()
            }
        } else {
            reset()
        }
    }

    override fun updateView(@LayoutRes resource: Int) {
        check(resource != INVALID_LAYOUT_ID) { "resource cannot be INVALID_LAYOUT_ID!" }
        helper.layoutView = null
        helper.layoutId = resource
        internalView?.updateView(resource)
    }

    override fun updateView(view: View) {
        helper.layoutId = INVALID_LAYOUT_ID
        helper.layoutView = view
        internalView?.updateView(view)
    }

    override fun updateView(provider: IFxContextProvider) {
        updateView(provider.build(platformProvider.context))
    }

    override fun updateViewContent(provider: IFxHolderProvider) {
        provider.apply(getViewHolder() ?: return)
    }

    override fun setClickListener(time: Long, listener: View.OnClickListener?) {
        helper.clickTime = time
        helper.iFxClickListener = listener
        helper.enableClickListener = listener != null
    }

    override fun setClickListener(listener: View.OnClickListener?) {
        setClickListener(0, listener)
    }

    override fun setLongClickListener(listener: View.OnLongClickListener?) {
        helper.iFxLongClickListener = listener
        helper.enableClickListener = listener != null
    }

    override fun move(x: Float, y: Float) {
        move(x, y, true)
    }

    override fun moveByVector(x: Float, y: Float) {
        moveByVector(x, y, true)
    }

    override fun move(x: Float, y: Float, useAnimation: Boolean) {
        val internalView = this.internalView
        if (internalView != null) {
            internalView.moveLocation(x, y, useAnimation)
        } else {
            // Queue the move operation for later execution
            queueOperation(FxQueuedOperation.Move(x, y, useAnimation))
        }
    }

    override fun moveByVector(x: Float, y: Float, useAnimation: Boolean) {
        val internalView = this.internalView
        if (internalView != null) {
            internalView.moveLocationByVector(x, y, useAnimation)
        } else {
            // Queue the move operation for later execution
            queueOperation(FxQueuedOperation.MoveByVector(x, y, useAnimation))
        }
    }

    override fun updateConfig(obj: IFxConfigControl.() -> Unit) {
        obj.invoke(_configControl)
    }

    protected open fun reset() {
        platformProvider.reset()
        _animationProvider.reset()
        helper.clear()
        // Clear pending operations on reset
        pendingOperations.clear()
        helper.fxLog.d("fxView-lifecycle-> code->cancelFx")
    }
    
    /**
     * Queue an operation to be executed when the floating window is ready
     */
    private fun queueOperation(operation: FxQueuedOperation) {
        helper.fxLog.d("fxView -> queueOperation: $operation")
        pendingOperations.add(operation)
    }
    
    /**
     * Execute all pending operations and clear the queue
     * This should be called when the floating window becomes ready
     */
    internal fun executePendingOperations() {
        if (pendingOperations.isEmpty()) return
        
        helper.fxLog.d("fxView -> executing ${pendingOperations.size} pending operations")
        val operations = pendingOperations.toList()
        pendingOperations.clear()
        
        for (operation in operations) {
            when (operation) {
                is FxQueuedOperation.Show -> executeShowInternal()
                is FxQueuedOperation.Hide -> executeHideInternal()
                is FxQueuedOperation.Move -> {
                    internalView?.moveLocation(operation.x, operation.y, operation.useAnimation)
                }
                is FxQueuedOperation.MoveByVector -> {
                    internalView?.moveLocationByVector(operation.x, operation.y, operation.useAnimation)
                }
            }
        }
    }
}
