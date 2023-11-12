package com.petterp.floatingx.view

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.util.FxAdsorbDirection
import com.petterp.floatingx.util.INVALID_TOUCH_ID
import com.petterp.floatingx.util.coerceInFx
import com.petterp.floatingx.util.pointerId

class FxViewConfigHelper {

    private var downTouchX = 0f
    private var downTouchY = 0f
    private var mParentWidth = 0f
    private var mParentHeight = 0f
    private lateinit var helper: BasisHelper
    private var scaledTouchSlop = 0

    var minHBoundary = 0f
    var maxHBoundary = 0f
    var minWBoundary = 0f
    var maxWBoundary = 0f

    var touchDownId = INVALID_TOUCH_ID

    fun initConfig(context: Context, helper: BasisHelper) {
        this.helper = helper
        scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    fun safeX(x: Float) = x.coerceInFx(minWBoundary, maxWBoundary)
    fun safeY(y: Float) = y.coerceInFx(minHBoundary, maxHBoundary)
    fun safeX(x: Float, ev: MotionEvent) =
        x.plus(ev.x).minus(downTouchX).coerceInFx(minWBoundary, maxWBoundary)

    fun safeY(y: Float, ev: MotionEvent) =
        y.plus(ev.y).minus(downTouchY).coerceInFx(minHBoundary, maxHBoundary)

    fun hasMainPointerId() = touchDownId != INVALID_TOUCH_ID

    fun isCurrentPointerId(ev: MotionEvent): Boolean {
        if (touchDownId == INVALID_TOUCH_ID) return false
        return ev.pointerId == touchDownId
    }

    fun initTouchDown(ev: MotionEvent) {
        touchDownId = ev.pointerId
        downTouchX = ev.x
        downTouchY = ev.y
        helper.fxLog?.d("fxView---newTouchDown:$touchDownId")
    }

    fun updateWidgetSize(view: ViewGroup): Boolean {
        // 如果此时浮窗被父布局移除,parent将为null,此时就别更新位置了,没意义
        val parentGroup = (view.parent as? ViewGroup) ?: return false
        return updateWidgetSize(parentGroup.width, parentGroup.height, view)
    }

    fun updateWidgetSize(parentW: Int, parentH: Int, view: ViewGroup): Boolean {
        val parentWidth = (parentW - view.width).toFloat()
        val parentHeight = (parentH - view.height).toFloat()
        if (mParentHeight != parentHeight || mParentWidth != parentWidth) {
            helper.fxLog?.d("fxView->updateContainerSize: oldW-($mParentWidth),oldH-($mParentHeight),newW-($parentWidth),newH-($parentHeight)")
            mParentWidth = parentWidth
            mParentHeight = parentHeight
            updateBoundary(false)
            return true
        }
        return false
    }

    fun isNearestLeft(x: Float): Boolean {
        val middle = mParentWidth / 2
        return x < middle
    }

    fun isNearestTop(y: Float): Boolean {
        val middle = mParentHeight / 2
        return y < middle
    }

    fun checkInterceptedEvent(event: MotionEvent): Boolean {
        if (!isCurrentPointerId(event)) return false
        return kotlin.math.abs(event.x - downTouchX) >= scaledTouchSlop ||
            kotlin.math.abs(event.y - downTouchY) >= scaledTouchSlop
    }

    fun getAdsorbDirectionLocation(x: Float, y: Float): Pair<Float, Float>? {
        // 允许边缘吸附
        return if (helper.enableEdgeAdsorption) {
            if (helper.adsorbDirection == FxAdsorbDirection.LEFT_OR_RIGHT) {
                val moveX = if (isNearestLeft(x)) minWBoundary else maxWBoundary
                val moveY = safeY(y)
                moveX to moveY
            } else {
                val moveX = safeX(x)
                val moveY = if (isNearestTop(y)) minHBoundary else maxHBoundary
                moveX to moveY
            }
        } else if (helper.enableEdgeRebound) {
            safeX(x) to safeY(y)
        } else {
            null
        }
    }

    fun updateBoundary(isDownTouchInit: Boolean) {
        // 开启边缘回弹时,浮窗允许移动到边界外
        if (helper.enableEdgeRebound) {
            val edgeOffset = if (isDownTouchInit) 0f else helper.edgeOffset
            val marginTop = if (isDownTouchInit) 0f else helper.fxBorderMargin.t + edgeOffset
            val marginBto = if (isDownTouchInit) 0f else helper.fxBorderMargin.b + edgeOffset
            val marginLef = if (isDownTouchInit) 0f else helper.fxBorderMargin.l + edgeOffset
            val marginRig = if (isDownTouchInit) 0f else helper.fxBorderMargin.r + edgeOffset
            minWBoundary = marginLef
            maxWBoundary = mParentWidth - marginRig
            minHBoundary = helper.statsBarHeight.toFloat() + marginTop
            maxHBoundary = mParentHeight - helper.navigationBarHeight - marginBto
        } else {
            minWBoundary = helper.fxBorderMargin.l
            maxWBoundary = mParentWidth - helper.fxBorderMargin.r
            minHBoundary = helper.statsBarHeight + helper.fxBorderMargin.t
            maxHBoundary = mParentHeight - helper.navigationBarHeight - helper.fxBorderMargin.b
        }
    }
}
