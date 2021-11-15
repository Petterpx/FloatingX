package com.petterp.floatingx.listener.control

import android.view.View
import com.petterp.floatingx.assist.BorderMargin
import com.petterp.floatingx.assist.FxAnimation
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxScrollListener
import com.petterp.floatingx.listener.IFxViewLifecycle

/**
 * 配置更改接口,使用此接口运行时更改配置层
 * @author petterp
 */
interface IFxHelperControl {

    /** 获取配置helper */
    fun getConfigHelper(): BasisHelper

    /** 是否启用动画
     * @param enable 是否启用
     * @param animationImpl 具体实现实例
     * */
    fun setEnableAnimation(
        enable: Boolean,
        animationImpl: FxAnimation
    ) {
        getConfigHelper().enableAnimation = enable
        getConfigHelper().fxAnimation = animationImpl
    }

    /** 是否启用动画 */
    fun setEnableAnimation(
        enable: Boolean
    ) {
        getConfigHelper().enableAnimation = enable
    }

    /** 设置边框相对应父view的偏移量 */
    fun setBorderMargin(t: Float, l: Float, b: Float, r: Float) {
        getConfigHelper().borderMargin = BorderMargin(t, l, b, r)
    }

    /** 设置点击事件监听器 */
    fun setClickListener(clickListener: (View) -> Unit) {
        getConfigHelper().clickListener = clickListener
    }

    /** 设置是否启用点击事件 */
    fun setEnableClickListener(enable: Boolean) {
        getConfigHelper().enableClickListener = enable
    }

    fun setEdgeOffset(edgeOffset: Float) {
        getConfigHelper().edgeOffset = edgeOffset
    }

    /** 启用位置修复 */
    fun setEnableAbsoluteFix(isEnable: Boolean) {
        getConfigHelper().enableAbsoluteFix = isEnable
    }

    /** 启用边缘回弹
     * */
    fun setEnableEdgeRebound(isEnable: Boolean) {
        getConfigHelper().enableEdgeRebound = isEnable
    }

    /** 设置是否启用触摸事件
     * @param enable==true,则允许悬浮窗拖动
     * */
    fun setEnableTouch(isEnable: Boolean) {
        getConfigHelper().enableTouch = isEnable
    }

    /** 是否启用边缘吸附
     * @param enable 是否启用,默认true
     * @param lazyStart 是否下次拖动再生效,false 代表立即生效,即立即边缘吸附
     * */
    fun setEnableEdgeAdsorption(isEnable: Boolean, lazyStart: Boolean = false)

    /** 设置滑动监听 */
    fun setScrollListener(listener: IFxScrollListener) {
        getConfigHelper().iFxScrollListener = listener
    }

    /** 设置view-lifecycle监听 */
    fun setViewLifecycleListener(listener: IFxViewLifecycle) {
        getConfigHelper().iFxViewLifecycle = listener
    }

    /** 设置允许保存方向 */
    fun setEnableSaveDirection(impl: IFxConfigStorage, isEnable: Boolean = true) {
        getConfigHelper().iFxConfigStorage = impl
        getConfigHelper().enableSaveDirection = isEnable
    }

    /** 设置方向保存开关
     * 设置之前,请确保已经设置了方向保存实例
     * */
    fun setEnableSaveDirection(isEnable: Boolean = true) {
        getConfigHelper().enableSaveDirection = isEnable
    }

    /** 清除保存的位置信息 */
    fun clearLocationStorage() {
        getConfigHelper().iFxConfigStorage?.clear()
    }
}
