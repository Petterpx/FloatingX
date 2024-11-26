package com.petterp.floatingx.listener

import android.app.Activity
import com.petterp.floatingx.assist.helper.FxBuilderDsl
import com.petterp.floatingx.util.FxPermissionResultAction

/**
 * 在开始权限请求前进行检查，可以继续延续请求
 * @return true: 拦截权限请求，自行处理
 * @return false: 不拦截，由fx自行处理
 * */
typealias IFxPermissionInterceptor = (activity: Activity, controller: IFxPermissionAskControl) -> Unit

@FxBuilderDsl
interface IFxPermissionAskControl {

    /**
     * 主动请求浮窗权限
     * @param activity 当前Activity
     * */
    fun requestPermission(activity: Activity) = requestPermission(activity, true)

    /**
     * 主动请求浮窗权限
     * @param activity 当前Activity
     * @param isAutoShow 有权限后，是否自动显示浮窗
     * */
    fun requestPermission(activity: Activity, isAutoShow: Boolean) =
        requestPermission(activity, isAutoShow, true)

    /**
     * 主动请求浮窗权限
     * @param activity 当前Activity
     * @param isAutoShow 有权限后，是否自动显示浮窗
     * @param canUseAppScope 无权限时，是否需要浮窗降级为应用内浮窗
     * */
    fun requestPermission(activity: Activity, isAutoShow: Boolean, canUseAppScope: Boolean) =
        requestPermission(activity, isAutoShow, canUseAppScope, null)

    /**
     * 主动请求浮窗权限
     * @param activity 当前activity
     * @param isAutoShow 有权限后，是否自动显示浮窗
     * @param canUseAppScope 无权限时，是否需要浮窗降级为应用内浮窗
     * @param resultListener 结果回调
     * */
    fun requestPermission(
        activity: Activity,
        isAutoShow: Boolean,
        canUseAppScope: Boolean,
        resultListener: FxPermissionResultAction?
    )

    /**
     * 释放浮窗配置，一般是用户拒绝了权限
     * @param isRelease 是否释放配置
     * */
    fun releaseConfig(isRelease: Boolean)

    /**
     * 降级到应用浮窗
     * */
    fun downgradeToAppScope()
}
