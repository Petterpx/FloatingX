package com.petterp.floatingx.view.basic

import com.petterp.floatingx.assist.FxAdsorbDirection
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.util.coerceInFx
import com.petterp.floatingx.util.shr

/**
 * Fx location restore helper，Used to restore the location of the floating window after the screen is rotated
 * @author petterp
 */
class FxViewLocationHelper : FxBasicViewHelper() {
    private var screenChanged: Boolean = false
    private var isInitLocation = true

    private var needUpdateLocation = false
    private var parentW = 0f
    private var parentH = 0f
    private var viewW = 0f
    private var viewH = 0f

    private val moveIngBoundary = FxViewBoundaryConfig()
    private val moveBoundary = FxViewBoundaryConfig()

    private val x: Float
        get() = basicView?.currentX() ?: 0f
    private val y: Float
        get() = basicView?.currentY() ?: 0f

    fun isRestoreLocation() = screenChanged

    fun isInitLocation(): Boolean {
        if (isInitLocation) {
            isInitLocation = false
            return true
        }
        return false
    }

    fun updateLocationStatus() {
        needUpdateLocation = true
    }

    fun onSizeChanged() {
        updateViewSize()
        if (needUpdateLocation) {
            basicView?.moveToEdge()
            needUpdateLocation = false
            config.fxLog.d("fxView -> updateLocation")
        }
    }

    fun initLayout(view: FxBasicParentView) {
        val hasHistory = config.enableSaveDirection && config.iFxConfigStorage?.hasConfig() == true
        val (defaultX, defaultY) = if (hasHistory) {
            getHistoryXY()
        } else {
            getDefaultXY(parentW, parentH, viewW, viewH)
        }
        view.updateXY(safeX(defaultX), safeY(defaultY))
        val from = if (hasHistory) "history_location" else "default_location"
        config.fxLog.d("fxView -> initLocation: x:$defaultX,y:$defaultY,from:$from")
    }

    fun getDefaultEdgeXY(): Pair<Float, Float>? {
        return if (config.enableEdgeAdsorption) {
            if (config.adsorbDirection == FxAdsorbDirection.LEFT_OR_RIGHT) {
                val moveX = if (isNearestLeft(x)) moveBoundary.minW else moveBoundary.maxW
                val moveY = y
                moveX to moveY
            } else {
                val moveX = x
                val moveY = if (isNearestTop(y)) moveBoundary.minH else moveBoundary.maxH
                moveX to moveY
            }
        } else if (config.enableEdgeRebound) {
            x to y
        } else {
            null
        }
    }

    private fun updateViewSize() {
        val view = basicView ?: return
        val (pW, pH) = view.parentSize() ?: return
        val viewH = view.height.toFloat()
        val viewW = view.width.toFloat()
        this.parentW = pW.toFloat()
        this.parentH = pH.toFloat()
        this.viewW = viewW
        this.viewH = viewH
        updateBoundary()
        config.fxLog.d("fxView -> updateViewSize: parentW:$parentW,parentH:$parentH,viewW:$viewW,viewH:$viewH")
    }

    fun safeX(x: Float, isMoveIng: Boolean = false): Float {
        val enableBound = isMoveIng && config.enableEdgeRebound
        val minW = if (enableBound) moveIngBoundary.minW else moveBoundary.minW
        val maxW = if (enableBound) moveIngBoundary.maxW else moveBoundary.maxW
        return x.coerceInFx(minW, maxW)
    }

    fun safeY(y: Float, isMoveIng: Boolean = false): Float {
        val enableBound = isMoveIng && config.enableEdgeRebound
        val minH = if (enableBound) moveIngBoundary.minH else moveBoundary.minH
        val maxH = if (enableBound) moveIngBoundary.maxH else moveBoundary.maxH
        return y.coerceInFx(minH, maxH)
    }

    fun checkOrSaveLocation(x: Float, y: Float) {
        if (config.iFxConfigStorage == null || !config.enableSaveDirection) return
        config.iFxConfigStorage!!.update(x, y)
        config.fxLog.d("saveLocation -> x:$x,y:$y")
    }

    private fun isNearestLeft(x: Float): Boolean {
        val middle = parentW / 2
        return x < middle
    }

    private fun isNearestTop(y: Float): Boolean {
        val middle = parentH / 2
        return y < middle
    }

    private fun getHistoryXY(): Pair<Float, Float> {
        return config.run {
            val configX = iFxConfigStorage?.getX() ?: 0f
            val configY = iFxConfigStorage?.getY() ?: 0f
            configX to configY
        }
    }

    private fun getDefaultXY(
        width: Float,
        height: Float,
        viewW: Float,
        viewH: Float
    ): Pair<Float, Float> {
        return config.run {
            val offX = offsetX + safeEdgeOffSet
            val offY = offsetY + safeEdgeOffSet
            // 为历史方法做兼容
            val l = offX + fxBorderMargin.l + (assistLocation?.l ?: offsetX)
            val r = offX + fxBorderMargin.r + (assistLocation?.r ?: offsetX)
            val b = offY + fxBorderMargin.b + (assistLocation?.b ?: offsetY)
            val t = offY + fxBorderMargin.t + (assistLocation?.t ?: offsetY)
            when (gravity) {
                FxGravity.DEFAULT,
                FxGravity.LEFT_OR_TOP -> l to t

                FxGravity.LEFT_OR_CENTER -> l to (height - viewH).shr(1)

                FxGravity.LEFT_OR_BOTTOM -> 0f to height - viewH - b

                FxGravity.RIGHT_OR_TOP -> width - viewW - r to t

                FxGravity.RIGHT_OR_CENTER -> width - viewW - r to (height - viewH).shr(1)

                FxGravity.RIGHT_OR_BOTTOM -> width - viewW - r to height - viewH - b

                FxGravity.TOP_OR_CENTER -> (width - viewW).shr(1) to t

                FxGravity.BOTTOM_OR_CENTER -> (width - viewW).shr(1) to height - viewH - b

                else -> (width - viewW).shr(1) to (height - viewH).shr(1)
            }
        }
    }

    private fun updateBoundary() {
        config.apply {
            moveIngBoundary.apply {
                minW = 0f
                maxW = parentW - viewW
                minH = statsBarHeight.toFloat()
                maxH = parentH - viewH - navigationBarHeight
            }
            moveBoundary.copy(moveIngBoundary).apply {
                minW += fxBorderMargin.l + edgeOffset
                maxW -= fxBorderMargin.r + edgeOffset
                minH += fxBorderMargin.t + edgeOffset
                maxH -= fxBorderMargin.b + edgeOffset
            }
        }
    }
}
