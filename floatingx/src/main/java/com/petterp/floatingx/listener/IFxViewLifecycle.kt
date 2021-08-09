package com.petterp.floatingx.listener

/**
 * fx-悬浮窗的生命周期扩展
 */
interface IFxViewLifecycle {

    /** 安装悬浮窗到新窗口前调用 */
    fun postAttach() {}

    /**  安装悬浮窗到新窗口时 */
    fun attach() {}

    /** 窗口可见性监听,即悬浮窗完全可见时 */
    fun windowsVisibility(visibility: Int) {}

    /** 窗口移除前调用 */
    fun postDetached() {}

    /** 从当前view移除悬浮窗时调用 */
    fun detached() {}
}
