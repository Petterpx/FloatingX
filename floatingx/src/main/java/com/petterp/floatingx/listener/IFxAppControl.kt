package com.petterp.floatingx.listener

import android.app.Activity
import androidx.annotation.MainThread

/**
 * @Author petterp
 * @Date 2021/5/21-11:40 上午
 * @Email ShiyihuiCloud@163.com
 * @Function FloatingX 全局悬浮窗控制器
 */
interface IFxAppControl : IFxControlBasis {

    /**
     * 显示悬浮窗
     * @param activity 当前Activity
     * @param isAnimation 是否执行动画
     * */
    @MainThread
    fun show(activity: Activity, isAnimation: Boolean = true)

    /** 安装在指定activity上 */
    fun attach(activity: Activity)

    /** 从指定activity上删除 */
    fun detach(activity: Activity)
}
