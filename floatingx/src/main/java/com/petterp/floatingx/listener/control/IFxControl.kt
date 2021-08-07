package com.petterp.floatingx.listener.control

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import com.petterp.floatingx.view.FxMagnetView
import com.petterp.floatingx.view.FxViewHolder

/** FloatingX 基础控制器 */
interface IFxControl {

    /** 显示悬浮窗 */
    fun show()

    /** 隐藏悬浮窗-不会解绑app-lifecycle
     * */
    @MainThread
    fun hide()

    /**
     * 当前是否显示
     * @return 是否显示
     * */
    fun isShow(): Boolean

    /**
     * 关闭fx,并释放所有监听
     * 在普通模式,这相当于干掉当前悬浮窗
     * 在全局application,这等于只保留helper,移除其他所有监听
     * */
    fun cancel()

    /** 获取自定义的view
     * @return 悬浮窗view->managerView
     * */
    fun getManagerView(): FxMagnetView?
    fun getManagerViewHolder(): FxViewHolder?

    fun getView(): View?

    /** 更新params
     * @param params 悬浮窗管理器的layoutParams
     * */
    @MainThread
    fun updateParams(params: ViewGroup.LayoutParams)

    /** 提供一个回调入口,用于快捷刷新
     * @param obj
     * */
    @MainThread
    fun updateView(obj: (FxViewHolder) -> Unit)

    /**
     * 更新当前view
     * @param resource 新的布局layout
     * */
    @MainThread
    fun updateManagerView(@LayoutRes resource: Int)

    /** 设置点击事件 */
    fun setClickListener(time: Long = 500L, obj: (View) -> Unit)
}
