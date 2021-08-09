package com.petterp.floatingx.listener.control

import com.petterp.floatingx.assist.BorderMargin
import com.petterp.floatingx.assist.FxAnimation
import com.petterp.floatingx.assist.helper.BaseHelper
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxScrollListener
import com.petterp.floatingx.listener.IFxViewLifecycle

/**
 * 配置更改接口,使用此接口运行时更改配置层
 * @author petterp
 */
interface IFxHelperControl {

    /** 获取配置helper */
    fun getConfigHelper(): BaseHelper

    /** 是否启用动画
     * @param enable 是否启用
     * @param animationImpl 具体实现实例
     * */
    fun enableAnimation(
        enable: Boolean,
        animationImpl: FxAnimation
    ) {
        getConfigHelper().enableAnimation = enable
        getConfigHelper().fxAnimation = animationImpl
    }

    /** 是否启用动画 */
    fun enableAnimation(
        enable: Boolean
    ) {
        getConfigHelper().enableAnimation = enable
    }

    /** 设置边框相对应父view的偏移量 */
    fun setBorderMargin(t: Float, l: Float, b: Float, r: Float) {
        getConfigHelper().borderMargin = BorderMargin(t, l, b, r)
    }

    fun setEdgeOffset(edgeOffset: Float) {
        getConfigHelper().edgeOffset = edgeOffset
    }

    /** 启用位置修复 */
    fun enableAbsoluteFix(enable: Boolean) {
        getConfigHelper().enableAbsoluteFix = enable
    }

    /** 启用边缘回弹
     * */
    fun enableEdgeRebound(enable: Boolean) {
        getConfigHelper().enableEdgeRebound = enable
    }

    /** 是否启用边缘吸附
     * @param enable 是否启用,默认true
     * @param lazyStart 是否下次拖动再生效,false 代表立即生效,即立即边缘吸附
     * */
    fun enableEdgeAdsorption(enable: Boolean, lazyStart: Boolean = false) {
        getConfigHelper().enableEdgeAdsorption = enable
    }

    /** 设置滑动监听 */
    fun setScrollListener(listener: IFxScrollListener) {
        getConfigHelper().iFxScrollListener = listener
    }

    /** 设置view-lifecycle监听 */
    fun setViewLifecycleListener(listener: IFxViewLifecycle) {
        getConfigHelper().iFxViewLifecycle = listener
    }

    /** 设置允许保存方向 */
    fun setSaveDirection(impl: IFxConfigStorage, enable: Boolean = true) {
        getConfigHelper().iFxConfigStorage = impl
        getConfigHelper().enableSaveDirection = enable
    }
}
