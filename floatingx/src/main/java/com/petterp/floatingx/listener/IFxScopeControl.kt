package com.petterp.floatingx.listener

import android.app.Activity
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * @Author petterp
 * @Date 2021/8/5-11:29 PM
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
interface IFxScopeControl<T> {
    fun init(viewGroup: ViewGroup): T
    fun init(fragment: Fragment): T
    fun init(activity: Activity): T
}
