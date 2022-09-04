package com.petterp.floatingx.listener.control

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import com.petterp.floatingx.listener.provider.IFxContextProvider
import com.petterp.floatingx.listener.provider.IFxHolderProvider
import com.petterp.floatingx.view.FxMagnetView
import com.petterp.floatingx.view.FxViewHolder

/** FloatingX 基础控制器 */
interface IFxControl {

    /** 获取配置层控制器,以便运行时动态调整某些基础配置 */
    val helperControl: IFxHelperControl

    /** 显示悬浮窗 */
    fun show()

    /** 隐藏悬浮窗-不会解绑app-lifecycle */
    @MainThread
    fun hide()

    /**
     * 当前是否显示
     *
     * @return 是否显示
     */
    fun isShow(): Boolean

    /** 关闭fx,并释放所有监听 在普通模式,这相当于干掉当前悬浮窗 在全局application,这等于只保留helper,移除其他所有监听 */
    fun cancel()

    /** 获取正在显示的浮窗内部视图,即通过layoutId或者自定义View传递进来的 View,一般情况下使用[getManagerView]即可 */
    fun getView(): View?

    /** 用于快速刷新 */
    @MainThread
    fun updateView(provider: IFxHolderProvider)

    /**
     * 获取浮窗管理器view
     */
    fun getManagerView(): FxMagnetView?

    fun getManagerViewHolder(): FxViewHolder?

    /**
     * 更新params
     * @param params 悬浮窗管理器的layoutParams
     */
    @MainThread
    fun updateManagerParams(params: ViewGroup.LayoutParams)

    /**
     * 更新当前view
     *
     * @param resource 新的布局layout
     */
    @MainThread
    fun updateManagerView(@LayoutRes resource: Int)

    /** 更新当前View */
    @MainThread
    fun updateManagerView(view: View)

    /** 更新当前View,如果要通过view更新视图,建议通过此方法,可以帮助选用合适的context,来避免因context所导致的内存泄漏 */
    @MainThread
    fun updateManagerView(provider: IFxContextProvider)

    /** 设置点击事件 */
    fun setClickListener(time: Long = 500L, clickListener: View.OnClickListener)
}
