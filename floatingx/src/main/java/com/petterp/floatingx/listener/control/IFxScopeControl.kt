package com.petterp.floatingx.listener.control

import android.app.Activity
import android.widget.FrameLayout
import androidx.fragment.app.Fragment

/**
 * Fx局部控制器
 */
interface IFxScopeControl<T> {
    /**
     * 在FrameLayout中初始化
     *@param group 根布局必须为[FrameLayout],以便fx正常插入
     * */
    fun init(group: FrameLayout): T

    /**
     * 在FrameLayout中初始化
     *@param fragment 您Fragment根布局必须为[FrameLayout],否则将抛出异常
     * */
    fun init(fragment: Fragment): T

    /**
     * 在Activity中初始化
     * @param activity 最终会插入到 R.id.content 中
     * */
    fun init(activity: Activity): T
}
