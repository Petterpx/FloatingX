package com.petterp.floatingx.listener.control

import com.petterp.floatingx.assist.FxAnimation
import com.petterp.floatingx.assist.FxDisplayMode
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.assist.FxAdsorbDirection
import com.petterp.floatingx.listener.IFxTouchListener

/**
 * 配置更改接口,使用此接口运行时更改配置层
 * @author petterp
 */
interface IFxConfigControl {

    /** 是否启用动画
     * @param isEnable 是否启用
     * @param animationImpl 具体实现实例
     * */
    fun setEnableAnimation(isEnable: Boolean, animationImpl: FxAnimation? = null)

    /** 是否启用动画 */
    fun setEnableAnimation(isEnable: Boolean)

    /** 设置边框相对应父view的偏移量 */
    fun setBorderMargin(t: Float, l: Float, b: Float, r: Float)

    /** 设置边缘吸附方向 */
    fun setEdgeAdsorbDirection(direction: FxAdsorbDirection)

    /** 设置是否启用点击事件 */
    fun setEnableClick(isEnable: Boolean)

    /** 设置边缘偏移量 */
    fun setEdgeOffset(edgeOffset: Float)

    /** 启用边缘回弹
     * */
    fun setEnableEdgeRebound(isEnable: Boolean)

    /** 设置是否支启用悬浮窗半隐藏模式
     * */
    fun setEnableHalfHide(isEnable: Boolean)

    /**
     * 设置悬浮窗半隐藏模式的隐藏比例
     * @param isEnable 是否启用
     * @param percent 半隐比例
     * */
    fun setEnableHalfHide(isEnable: Boolean, percent: Float = 0.5f)

    /**
     * 设置浮窗展示模式
     *
     * @param mode 展示模式
     * - [FxDisplayMode.Normal] 默认模式，可以移动与点击
     * - [FxDisplayMode.ClickOnly] 禁止移动，只能响应点击事件
     * - [FxDisplayMode.DisplayOnly] 只能展示，不能移动与响应点击事件
     * */
    fun setDisplayMode(mode: FxDisplayMode)

    /**
     * 启用边缘吸附
     * */
    fun setEnableEdgeAdsorption(isEnable: Boolean)

    /** 设置滑动监听 */
    fun setTouchListener(listener: IFxTouchListener)

    /** 设置view-lifecycle监听 */
    @Deprecated(
        replaceWith = ReplaceWith("addViewLifecycleListener"),
        message = "use addViewLifecycle"
    )
    fun setViewLifecycleListener(listener: IFxViewLifecycle)

    fun addViewLifecycleListener(listener: IFxViewLifecycle)

    /** 设置允许保存方向 */
    fun setEnableSaveDirection(impl: IFxConfigStorage, isEnable: Boolean = true)

    /** 设置方向保存开关
     * 设置之前,请确保已经设置了方向保存实例
     * */
    fun setEnableSaveDirection(isEnable: Boolean = true)

    /** 清除保存的位置信息 */
    fun clearLocationStorage()
}
