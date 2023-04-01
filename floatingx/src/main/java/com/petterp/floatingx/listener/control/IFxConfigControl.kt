package com.petterp.floatingx.listener.control

import com.petterp.floatingx.assist.FxAnimation
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxScrollListener
import com.petterp.floatingx.listener.IFxViewLifecycle

/**
 * 配置更改接口,使用此接口运行时更改配置层
 * @author petterp
 */
interface IFxConfigControl {

    /** 是否启用动画
     * @param isEnable 是否启用
     * @param animationImpl 具体实现实例
     * */
    fun setEnableAnimation(isEnable: Boolean, animationImpl: FxAnimation)

    /** 是否启用动画 */
    fun setEnableAnimation(isEnable: Boolean)

    /** 设置边框相对应父view的偏移量 */
    fun setBorderMargin(t: Float, l: Float, b: Float, r: Float)

    /** 设置是否启用点击事件 */
    fun setEnableClick(isEnable: Boolean)

    /** 设置边缘偏移量 */
    fun setEdgeOffset(edgeOffset: Float)

    /** 启用边缘回弹
     * */
    fun setEnableEdgeRebound(isEnable: Boolean)

    /** 设置是否启用触摸事件
     * @param isEnable  true,则允许悬浮窗拖动
     * */
    fun setEnableTouch(isEnable: Boolean)

    /**
     * 启用边缘吸附
     * */
    fun setEnableEdgeAdsorption(isEnable: Boolean)

    /** 设置滑动监听 */
    fun setScrollListener(listener: IFxScrollListener)

    /** 设置view-lifecycle监听 */
    fun setViewLifecycleListener(listener: IFxViewLifecycle)

    /** 设置允许保存方向 */
    fun setEnableSaveDirection(impl: IFxConfigStorage, isEnable: Boolean = true)

    /** 设置方向保存开关
     * 设置之前,请确保已经设置了方向保存实例
     * */
    fun setEnableSaveDirection(isEnable: Boolean = true)

    /** 清除保存的位置信息 */
    fun clearLocationStorage()
}
