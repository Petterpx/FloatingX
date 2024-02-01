package com.petterp.floatingx.view.helper

import android.content.res.Configuration
import android.view.View
import com.petterp.floatingx.assist.FxAdsorbDirection
import com.petterp.floatingx.assist.FxBoundaryConfig
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.util.coerceInFx
import com.petterp.floatingx.util.shr
import com.petterp.floatingx.view.FxBasicContainerView

/**
 * 浮窗坐标的配置助手，用于处理坐标相关的处理
 * @author petterp
 */
class FxViewLocationHelper : FxViewBasicHelper(), View.OnLayoutChangeListener {
    private var parentW = 0f
    private var parentH = 0f
    private var viewW = 0f
    private var viewH = 0f

    private var screenWidthDp = 0
    private var screenHeightDp = 0
    private var restoreLeftStandard = false
    private var restoreTopStandard = false
    private var needUpdateLocation: Boolean = false
    private var orientation = Configuration.ORIENTATION_PORTRAIT

    private val moveIngBoundary = FxBoundaryConfig()
    private val moveBoundary = FxBoundaryConfig()

    private val x: Float
        get() = basicView?.currentX() ?: 0f
    private val y: Float
        get() = basicView?.currentY() ?: 0f

    override fun initConfig(parentView: FxBasicContainerView) {
        super.initConfig(parentView)
        parentView.addOnLayoutChangeListener(this)
        parentView.resources.configuration.apply {
            this@FxViewLocationHelper.orientation = orientation
            this@FxViewLocationHelper.screenWidthDp = screenWidthDp
            this@FxViewLocationHelper.screenHeightDp = screenHeightDp
        }
    }

    override fun onInit() {
        val hasHistory = config.enableSaveDirection && config.iFxConfigStorage?.hasConfig() == true
        val (defaultX, defaultY) = if (hasHistory) {
            getHistoryXY()
        } else {
            getDefaultXY(parentW, parentH, viewW, viewH)
        }
        basicView?.updateXY(safeX(defaultX), safeY(defaultY))
        val from = if (hasHistory) "history_location" else "default_location"
        config.fxLog.d("fxView -> initLocation: x:$defaultX,y:$defaultY,way:[$from]")
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        updateViewSize()
        checkOrRestoreLocation()
    }

    override fun onConfigurationChanged(config: Configuration) {
        if (config.orientation != orientation || config.screenWidthDp != screenWidthDp || config.screenHeightDp != screenHeightDp) {
            orientation = config.orientation
            screenWidthDp = config.screenWidthDp
            screenHeightDp = config.screenHeightDp
            restoreLeftStandard = isNearestLeft(x)
            restoreTopStandard = isNearestTop(y)
            this.needUpdateLocation = true
            this.config.fxLog.d("fxView -> onConfigurationChanged:[screenChanged:${this.needUpdateLocation}]")
        }
    }

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        checkOrRestoreLocation()
    }

    fun needUpdateLocation() {
        this.needUpdateLocation = true
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

    fun safeX(x: Float, isMoveIng: Boolean = false): Float {
        // 是否考虑边界
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
        config.fxLog.d("fxView -> updateSize: parentW:$parentW,parentH:$parentH,viewW:$viewW,viewH:$viewH")
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

    private fun checkOrRestoreLocation() {
        if (!needUpdateLocation) return
        config.fxLog.d("fxView -> restoreLocation,start")
        updateViewSize()
        val restoreX: Float
        val restoreY: Float
        if (config.enableEdgeAdsorption) {
            restoreX = if (restoreLeftStandard) moveBoundary.minW else moveBoundary.maxW
            restoreY = if (restoreTopStandard) moveBoundary.minH else moveBoundary.maxH
        } else {
            restoreX = safeX(x)
            restoreY = safeY(y)
        }
        restoreLeftStandard = false
        restoreTopStandard = false
        needUpdateLocation = false
        basicView?.moveLocation(restoreX, restoreY, false)
        config.fxLog.d("fxView -> restoreLocation,success")
    }
}
