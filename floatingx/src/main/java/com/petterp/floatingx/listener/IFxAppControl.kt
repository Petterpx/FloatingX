package com.petterp.floatingx.listener

import android.app.Activity

/**
 * @Author petterp
 * @Date 2021/8/5-11:31 PM
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
interface IFxAppControl {
    fun show(activity: Activity)

    fun detach(activity: Activity)
}
