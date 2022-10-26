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

    /** 显示悬浮窗 */
    fun show()

    /** 隐藏悬浮窗-不会解绑app-lifecycle */
    fun hide()

    /**
     * 当前浮窗是否显示
     */
    fun isShow(): Boolean

    /** 关闭fx,并释放所有监听 在普通模式,这相当于干掉当前悬浮窗 在全局application,这等于只保留helper,移除其他所有监听 */
    fun cancel()

    /** 获取正在显示的浮窗内容视图,即通过layoutId或者自定义View传递进来的 View */
    fun getView(): View?

    /** 获取浮窗内容视图所对应的Holder */
    fun getViewHolder(): FxViewHolder?

    /**
     * 获取浮窗管理器view,即浮窗底层容器
     */
    fun getManagerView(): FxManagerView?

    /** 用于快速刷新视图内容 */
    fun updateViewContent(provider: IFxHolderProvider)

    /**
     * 更新当前view
     *
     * @param resource 新的布局layout
     */
    fun updateView(@LayoutRes resource: Int)

    /** 更新当前View */
    fun updateView(view: View)

    /** 更新当前View,如果要通过view更新视图,建议通过此方法,可以帮助选用合适的context,来避免因context所导致的内存泄漏 */
    fun updateView(provider: IFxContextProvider)
}
