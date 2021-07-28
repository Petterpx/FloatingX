package com.petterp.floatingx.listener

import android.app.Activity
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
 * @Function FloatingX 控制器接口
 */
interface IFxControl {

    /**
     * 显示悬浮窗
     * @param activity 当前Activity
     * @param isAnimation 是否执行动画
     * */
    @MainThread
    fun show(activity: Activity, isAnimation: Boolean = false)

    @MainThread
    fun show(container: ViewGroup, isAnimation: Boolean = false)

    /** 隐藏悬浮窗-不会解绑app-lifecycle
     * @param isAnimation 是否执行动画
     * */
    @MainThread
    fun hide(isAnimation: Boolean = true)

    /**
     * 关闭fx,并释放所有监听
     * @param isAnimation 是否执行动画
     * */
    fun cancel(isAnimation: Boolean = false)

    /** 获取自定义的view
     * @return 悬浮窗view->managerView
     * */
    fun getManagerView(): View?

    fun getView(): View?

    /**
     * 当前是否显示
     * @return 是否显示
     * */
    fun isShow(): Boolean

    /** 更新params
     * @param params 悬浮窗管理器的layoutParams
     * */
    @MainThread
    fun updateParams(params: ViewGroup.LayoutParams)

    /** 提供一个回调入口,用于快捷刷新
     * @param obj
     * */
    @MainThread
    fun updateView(obj: (FxViewHolder) -> Unit)

    /**
     * 更新当前view
     * @param resource 新的布局layout
     * */
    @MainThread
    fun updateView(@LayoutRes resource: Int)

    fun setClickListener(time: Long = FxHelper.clickDefaultTime, obj: (View) -> Unit)
}
