package com.petterp.floatingx.listener

import android.app.Activity
import android.widget.FrameLayout
import androidx.annotation.MainThread

/**
 * @Author petterp
 * @Date 2021/5/21-11:40 上午
 * @Email ShiyihuiCloud@163.com
 * @Function FloatingX 全局悬浮窗控制器
 */
interface IFxControl : IFxControlBasis {

    @MainThread
    fun show(activity: Activity)

    /** 安装在指定activity上 */
    fun attach(activity: Activity)

    /** 安装在指定FrameLayout上 */
    fun attach(container: FrameLayout)

    /** 从指定activity上删除 */
    fun detach(activity: Activity)

    /** 从指定FrameLayout上删除 */
    fun detach(container: FrameLayout)
}
