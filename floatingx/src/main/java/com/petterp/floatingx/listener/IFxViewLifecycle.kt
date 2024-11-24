package com.petterp.floatingx.listener

import android.view.View
import com.petterp.floatingx.assist.helper.FxBuilderDsl
import com.petterp.floatingx.view.FxViewHolder

/** fx-悬浮窗的生命周期扩展 */
@FxBuilderDsl
interface IFxViewLifecycle {

    /**
     * 初始化浮窗时调用,每次设置新的浮窗时都会调用
     * @param holder 浮窗内容Holder
     */
    fun initView(holder: FxViewHolder) {}

    @Deprecated("use initView(holder: FxViewHolder) instead", ReplaceWith("initView(holder)"))
    fun initView(view: View) {}

    /** 安装悬浮窗到新窗口时 */
    fun attach(view: View) {}

    /** 窗口可见性监听,即悬浮窗完全可见时 */
    fun windowsVisibility(visibility: Int) {}

    /** 从当前view移除悬浮窗时调用 */
    fun detached(view: View) {}
}
