package com.petterp.floatingx.listener

import android.view.View
import com.petterp.floatingx.view.FxViewHolder

/** fx-悬浮窗的生命周期扩展 */
interface IFxViewLifecycle {

    /**
     * 初始化浮窗时调用,每次设置新的浮窗时都会调用
     *
     * @param view 浮窗view
     */
    fun initView(view: View) {}

    /**
     * 初始化浮窗时调用,每次设置新的浮窗时都会调用
     *
     * @param holder 浮窗Holder
     */
    fun initView(holder: FxViewHolder) {}

    /** 安装悬浮窗到新窗口前调用 */
    fun postAttach() {}

    /** 安装悬浮窗到新窗口时 */
    fun attach() {}

    /** 窗口可见性监听,即悬浮窗完全可见时 */
    fun windowsVisibility(visibility: Int) {}

    /** 窗口移除前调用 */
    fun postDetached() {}

    /** 从当前view移除悬浮窗时调用 */
    fun detached() {}
}
