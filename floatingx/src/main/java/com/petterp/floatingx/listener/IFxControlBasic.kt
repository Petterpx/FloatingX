package com.petterp.floatingx.listener

import android.app.Activity
import android.view.ViewGroup

/**
 * @Author petterp
 * @Date 2021/7/28-10:43 PM
 * @Email ShiyihuiCloud@163.com
 * @doc 底层操作方法,这些方法对于用户而言不应该可见或者可操作
 */
interface IFxControlBasic {
    /** 安装在指定activity上 */
    fun attach(activity: Activity)

    /** 从指定activity上删除 */
    fun detach(activity: Activity)

    /** 从指定ViewGroup上删除 */
    fun detach(container: ViewGroup)
}
