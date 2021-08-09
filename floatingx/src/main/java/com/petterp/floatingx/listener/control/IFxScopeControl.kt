package com.petterp.floatingx.listener.control

import android.app.Activity
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * Fx局部控制器
 */
interface IFxScopeControl<T> {
    fun init(viewGroup: ViewGroup): T
    fun init(fragment: Fragment): T
    fun init(activity: Activity): T
}
