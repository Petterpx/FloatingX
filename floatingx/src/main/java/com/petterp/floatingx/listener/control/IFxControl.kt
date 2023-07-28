package com.petterp.floatingx.listener.control

import android.view.View
import androidx.annotation.LayoutRes
import com.petterp.floatingx.listener.provider.IFxContextProvider
import com.petterp.floatingx.listener.provider.IFxHolderProvider
import com.petterp.floatingx.view.FxManagerView
import com.petterp.floatingx.view.FxViewHolder

/** FloatingX 基础控制器 */
interface IFxControl {

    /** 获取配置层控制器,以便运行时动态调整某些基础配置 */
    val configControl: IFxConfigControl

    /** 隐藏悬浮窗-不会解绑app-lifecycle */
    fun hide()

    /** 当前浮窗是否显示 */
    fun isShow(): Boolean

    /**
     * 关闭fx,并释放所有监听 在普通模式,这相当于干掉当前悬浮窗
     *
     * 在全局浮窗,如果当前浮窗个数为0时，我们将移除所有配置监听，比如取消AppLifecycle的订阅
     */
    fun cancel()

    /** 获取正在显示的浮窗内容视图,即通过layoutId或者自定义View传递进来的 View */
    fun getView(): View?

    /** 获取浮窗内容视图所对应的Holder */
    fun getViewHolder(): FxViewHolder?

    /** 获取浮窗管理器view,即浮窗底层容器 */
    fun getManagerView(): FxManagerView?

    /** 用于快速刷新视图内容 */
    fun updateViewContent(provider: IFxHolderProvider)

    /**
     * 更新当前view
     * @param resource 新的布局layout
     */
    fun updateView(@LayoutRes resource: Int)

    /** 更新当前View */
    fun updateView(view: View)

    /** 更新当前View,如果要通过view更新视图,建议通过此方法,可以帮助选用合适的context,来避免因context所导致的内存泄漏 */
    fun updateView(provider: IFxContextProvider)

    /** 设置点击事件,同时增加防重 */
    fun setClickListener(time: Long = 300L, clickListener: View.OnClickListener)

    /** 设置点击事件 */
    fun setClickListener(clickListener: View.OnClickListener)

    /**
     * 移动浮窗到指定位置，该方法会帮助你处理越界问题，默认带动画
     * @param x 要移动到的x坐标
     * @param y 要移动到的y坐标
     * */
    fun move(x: Float, y: Float)

    /**
     * 移动浮窗到指定位置，该方法会帮助你处理越界问题
     * @param x 要移动到的x坐标
     * @param y 要移动到的y坐标
     * @param useAnimation 是否使用动画
     * */
    fun move(x: Float, y: Float, useAnimation: Boolean)

    /**
     * 按照向量移动浮窗，该方法会帮你处理越界问题
     * @param x x坐标要增加或减少的值
     * @param y y坐标要增加或减少的值
     * */
    fun moveByVector(x: Float, y: Float)

    /**
     * 按照向量移动浮窗，该方法会帮你处理越界问题
     * @param x x坐标要增加或减少的值
     * @param y y坐标要增加或减少的值
     * @param useAnimation 是否使用动画
     * */
    fun moveByVector(x: Float, y: Float, useAnimation: Boolean)
}
