package com.petterp.floatingx.listener

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import com.petterp.floatingx.assist.FxHelper
import com.petterp.floatingx.view.FxViewHolder

/**
 * @Author petterp
 * @Date 2021/5/28-9:54 上午
 * @Email ShiyihuiCloud@163.com
 * @Function FloatingX 基础控制器
 */
interface IFxControlBasis {

    @MainThread
    fun show(isAnimation: Boolean = true)

    @MainThread
    fun hide(isAnimation: Boolean = true)

    /** 关闭 */
    @MainThread
    fun dismiss()

    /** 获取自定义的view */
    fun getView(): View?

    /** 当前是否显示 */
    fun isShowRunning(): Boolean

    /** 更新params */
    @MainThread
    fun updateParams(params: ViewGroup.LayoutParams)

    /** 提供一个回调入口,用于快捷刷新 */
    @MainThread
    fun updateView(obj: (FxViewHolder) -> Unit)

    /** 更新当前view */
    @MainThread
    fun updateView(@LayoutRes resource: Int)

    fun setClickListener(time: Long = FxHelper.clickDefaultTime, obj: (View) -> Unit)
}
