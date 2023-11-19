package com.petterp.floatingx.view.default

import android.content.res.Configuration
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.util.coerceInFx

/**
 * Fx location restore helper，Used to restore the location of the floating window after the screen is rotated
 * @author petterp
 */
class FxLocationHelper {
    private lateinit var config: FxBasisHelper
    private var screenW = 0
    private var screenH = 0
    private var x: Float = 0f
    private var y: Float = 0f
    private var isNearestLeft = false
    private var screenChanged: Boolean = false
    private var isInitLocation = true

    fun initConfig(config: FxBasisHelper) {
        this.config = config
    }

    /**
     * Whether to restore the position
     * */
    fun isRestoreLocation() = screenChanged

    fun isInitLocation(): Boolean {
        if (isInitLocation) {
            isInitLocation = false
            return true
        }
        return false
    }

    /**
     * save location info
     * */
    fun saveLocation(
        x: Float,
        y: Float,
        configHelper: FxViewConfigHelper,
    ): FxLocationHelper {
        this.x = x
        this.y = y
        isNearestLeft = configHelper.isNearestLeft(x)
        return this
    }

    /**
     * update screen size config
     * @return Whether the screen rotation has occurred
     * */
    fun updateConfig(config: Configuration): Boolean {
        val isChangedScreen =
            if (config.screenWidthDp != screenW || config.screenHeightDp != screenH) {
                this.screenW = config.screenWidthDp
                this.screenH = config.screenHeightDp
                true
            } else {
                false
            }
        screenChanged = isChangedScreen
        return screenChanged
    }

    /** get location config  */
    fun getLocation(viewConfig: FxViewConfigHelper): Pair<Float, Float> {
        val newX = getX(viewConfig.minWBoundary, viewConfig.minWBoundary)
        val newY = getY(viewConfig.minHBoundary, viewConfig.maxHBoundary)
        this.screenChanged = false
        return newX to newY
    }

    private fun getX(min: Float, max: Float): Float {
        return if (config.enableEdgeAdsorption) {
            if (isNearestLeft) min else max
        } else {
            x.coerceInFx(min, max)
        }
    }

    private fun getY(min: Float, max: Float): Float {
        return y.coerceInFx(min, max)
    }
}
