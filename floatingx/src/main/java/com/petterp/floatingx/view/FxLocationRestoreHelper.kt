package com.petterp.floatingx.view

import android.content.res.Configuration
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.util.coerceInFx
import com.petterp.floatingx.util.topActivity

/**
 * Fx位置恢复助手，用于屏幕旋转后的逻辑延续
 * @author petterp
 */
class FxLocationRestoreHelper {
    // 当前屏幕的大小
    private var screenW = 0
    private var screenH = 0

    // 缓存浮窗位置相对于屏幕的比例
    private var x: Float = 0f
    private var y: Float = 0f
    private var isNearestLeft = false
    private var enableEdgeAdsorption = false

    // 屏幕大小是否已更新
    private var screenChanged: Boolean = false

    fun isScreenChanged() = screenChanged

    fun saveLocation(
        x: Float,
        y: Float,
        parentW: Float,
        config: BasisHelper
    ): FxLocationRestoreHelper {
        this.x = x
        this.y = y
        val middle = parentW / 2
        isNearestLeft = x < middle
        this.enableEdgeAdsorption = config.enableEdgeAdsorption
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

    fun getXY(minW: Float, maxW: Float, minH: Float, maxH: Float): Pair<Float, Float> {
        val newX = getX(minW, maxW)
        val newY = getY(minH, maxH)
        this.screenChanged = false
        return newX to newY
    }

    private fun getX(min: Float, max: Float): Float {
        return if (enableEdgeAdsorption) {
            if (isNearestLeft) min else max
        } else {
            x.coerceInFx(min, max)
        }
    }

    private fun getY(min: Float, max: Float): Float {
        return y.coerceInFx(min, max)
    }
}
