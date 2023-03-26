package com.petterp.floatingx.view

import android.content.res.Configuration
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.util.topActivity

/**
 * FxView的状态
 * @author petterp
 */
class FxViewScreenChangedHelper {
    // 当前屏幕的大小
    private var screenW = 0
    private var screenH = 0

    // 缓存浮窗位置相对于屏幕的比例
    private var ratioX: Float = 0f
    private var ratioY: Float = 0f

    // 屏幕大小是否已更新
    private var screenChanged: Boolean = false

    fun isScreenChanged() = screenChanged

    fun saveLocation(
        x: Float,
        y: Float,
        parentW: Float,
        parentH: Float
    ): FxViewScreenChangedHelper {
        ratioX = x / parentW
        ratioY = y / parentH
        return this
    }

    fun updateConfig(config: Configuration, helper: BasisHelper): Boolean {
        val isChangedScreen =
            if (config.screenWidthDp != screenW || config.screenHeightDp != screenH) {
                this.screenW = config.screenWidthDp
                this.screenH = config.screenHeightDp
                true
            } else {
                false
            }

        // check NavigationBar height and update
        val isChangedNavBar = if (helper is AppHelper) {
            val navigationBarHeight = helper.navigationBarHeight
            helper.updateNavigationBar(topActivity)
            navigationBarHeight != helper.navigationBarHeight
        } else {
            false
        }
        screenChanged = isChangedNavBar || isChangedScreen
        return screenChanged
    }

    fun setOnScreenChangedFlag(changed: Boolean) {
        this.screenChanged = changed
    }

    fun getPreX(parentW: Float): Float {
        return parentW * ratioX
    }

    fun getPreY(parentH: Float): Float {
        return parentH * ratioY
    }
}
