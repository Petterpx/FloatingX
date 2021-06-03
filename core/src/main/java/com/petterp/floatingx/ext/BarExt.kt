package com.petterp.floatingx.ext

import android.content.res.Resources

/**
 * @Author petterp
 * @Date 2021/6/2-7:12 下午
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
internal class BarExt private constructor() {
    companion object {
        var realStatusBarHeight: Int = getStatusBarHeight()

        // /////////////////////////////////////////////////////////////////////////
        // navigation bar
        // /////////////////////////////////////////////////////////////////////////
        fun getNavBarHeight(): Int {
            val res = Resources.getSystem()
            val resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resourceId != 0) {
                res.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        }

        fun getStatusBarHeight(): Int {
            val resources: Resources = Resources.getSystem()
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            return resources.getDimensionPixelSize(resourceId)
        }
    }
}
