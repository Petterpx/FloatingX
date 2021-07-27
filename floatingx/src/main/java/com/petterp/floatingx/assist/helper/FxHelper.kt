package com.petterp.floatingx.assist.helper

/**
 * @Author petterp
 * @Date 2021/7/27-10:30 PM
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
object FxHelper {
    fun toAppHelper(obj: AppHelper.AppHelperBuilder.() -> Unit): AppHelper =
        AppHelper.AppHelperBuilder().apply(obj).build()
}
