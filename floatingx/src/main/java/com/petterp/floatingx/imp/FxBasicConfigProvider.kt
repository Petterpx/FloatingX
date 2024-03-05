package com.petterp.floatingx.imp

import com.petterp.floatingx.assist.FxAdsorbDirection
import com.petterp.floatingx.assist.FxAnimation
import com.petterp.floatingx.assist.FxDisplayMode
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxScrollListener
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.listener.control.IFxConfigControl
import com.petterp.floatingx.listener.provider.IFxPlatformProvider
import com.petterp.floatingx.view.IFxInternalHelper

/**
 * Fx基础配置更改 提供者
 * @author petterp
 */
class FxBasicConfigProvider<F : FxBasisHelper, P : IFxPlatformProvider<F>>(
    private val helper: F,
    private var p: P?
) : IFxConfigControl {

    private val internalView: IFxInternalHelper?
        get() = p?.internalView

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
        helper.iFxTouchListener = listener
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
}
