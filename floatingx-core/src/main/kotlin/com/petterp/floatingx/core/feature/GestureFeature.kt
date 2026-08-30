package com.petterp.floatingx.core.feature

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.container.FxContainerTouchHandler
import com.petterp.floatingx.core.gesture.FxGestureDetector

/** 把容器触摸交给 FxGestureDetector，把识别结果转成 LocationFeature 动作与监听器事件 */
internal class GestureFeature(private val location: LocationFeature) : FxFeature, FxContainerTouchHandler, FxGestureDetector.Callback {

    private var scope: FxFeatureScope? = null
    private var detector: FxGestureDetector? = null

    override fun onAttach(scope: FxFeatureScope) {
        this.scope = scope
        val vc = ViewConfiguration.get(scope.container.view.context)
        detector = FxGestureDetector(vc.scaledTouchSlop.toFloat(), ViewConfiguration.getLongPressTimeout().toLong(), this)
            .also { it.config = scope.config.gesture }
        scope.container.touchHandler = this
    }

    override fun onDetach() {
        detector?.cancel()
        scope?.container?.touchHandler = null
        detector = null
        scope = null
    }

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        if (old.gesture != new.gesture) {
            detector?.cancel()
            detector?.config = new.gesture
        }
    }

    override fun onIntercept(ev: MotionEvent): Boolean = detector?.onIntercept(ev) ?: false

    override fun onTouch(ev: MotionEvent): Boolean = detector?.onTouch(ev) ?: false

    override fun onClick() {
        val s = scope ?: return
        val v = s.container.contentView ?: return
        s.dispatch { it.onClick(s.control, v) }
    }

    override fun onLongPress() {
        val s = scope ?: return
        val v = s.container.contentView ?: return
        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        s.dispatch { it.onLongClick(s.control, v) }
    }

    override fun onDragStart() {
        val s = scope ?: return
        location.onDragStart()
        s.dispatch { it.onDragStart(s.control) }
    }

    override fun onDrag(dx: Float, dy: Float) {
        val s = scope ?: return
        location.onDrag(dx, dy)
        val p = s.container.contentPosition()
        s.dispatch { it.onDrag(s.control, p.x, p.y) }
    }

    override fun onDragEnd() {
        val s = scope ?: return
        location.onDragEnd()
        val p = s.container.contentPosition()
        s.dispatch { it.onDragEnd(s.control, p.x, p.y) }
    }

    override fun canDragFrom(x: Float, y: Float): Boolean {
        val s = scope ?: return false
        val region = s.config.gesture.dragRegion ?: return true
        val c = s.container.contentView ?: return false
        return region.contains(x - c.translationX, y - c.translationY, c)
    }

    override fun hasScrollableChildAt(x: Float, y: Float): Boolean {
        val c = scope?.container?.contentView ?: return false
        return findScrollable(c, x - c.translationX, y - c.translationY)
    }

    private fun findScrollable(v: View, x: Float, y: Float): Boolean {
        if (x < 0f || y < 0f || x > v.width || y > v.height) return false
        if (v.canScrollVertically(1) || v.canScrollVertically(-1) || v.canScrollHorizontally(1) || v.canScrollHorizontally(-1)) return true
        if (v is ViewGroup) {
            for (i in v.childCount - 1 downTo 0) {
                val child = v.getChildAt(i)
                if (findScrollable(child, x - child.left - child.translationX, y - child.top - child.translationY)) return true
            }
        }
        return false
    }
}
