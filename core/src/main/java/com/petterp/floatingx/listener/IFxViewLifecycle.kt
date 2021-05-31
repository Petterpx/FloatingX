package com.petterp.floatingx.listener

/**
 * @Author petterp
 * @Date 2021/5/20-4:24 下午
 * @Email ShiyihuiCloud@163.com
 * @Function 关于 悬浮窗的生命周期
 */
interface IFxViewLifecycle {

    // 安装到新窗口时
    fun attach() {}

    // 从窗口移除
    fun detached() {}

    // 窗口可见性监听
    fun windowsVisibility(visibility: Int) {}
}
