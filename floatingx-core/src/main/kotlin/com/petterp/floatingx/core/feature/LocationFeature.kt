package com.petterp.floatingx.core.feature

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.host.FxLayoutSpec
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxAdsorbResolver
import com.petterp.floatingx.core.layout.FxLayoutInput
import com.petterp.floatingx.core.layout.FxLayoutResolver
import com.petterp.floatingx.core.layout.FxPoint
import com.petterp.floatingx.core.layout.FxSize

/**
 * 定位（spec §2.3）：位置真值是 control.anchor，这里只负责把它投影到容器，
 * 以及在拖动/moveTo 结束后把坐标反算回锚点提交。
 * 内容尺寸无效（0）时什么都不做，等有效布局——这是 2.x 跳变的根源。
 */
internal class LocationFeature : FxFeature {

    private var scope: FxFeatureScope? = null
    private var animator: ValueAnimator? = null

    /** 内容尺寸无效时收到的 moveTo，首次有效布局后应用 */
    private var pending: FxPoint? = null

    override fun onAttach(scope: FxFeatureScope) {
        this.scope = scope
        relayout()
    }

    override fun onDetach() {
        cancelAnimator()
        scope = null
    }

    override fun onContentSizeChanged(size: FxSize) = relayout()

    override fun onBoundsChanged() = relayout()

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        if (old.anchor != new.anchor || old.margin != new.margin || old.overflow != new.overflow || old.safeArea != new.safeArea) relayout()
    }

    fun relayout() {
        val s = scope ?: return
        val input = s.layoutInput() ?: return
        val p = pending
        if (p != null) {
            pending = null
            settle(s, FxLayoutResolver.clamp(p, input), input, animate = false)
            return
        }
        apply(s, FxLayoutResolver.resolve(s.control.anchor, input))
    }

    fun moveTo(x: Float, y: Float, animate: Boolean) {
        val s = scope ?: return
        val input = s.layoutInput()
        if (input == null) {
            pending = FxPoint(x, y)
            return
        }
        settle(s, FxLayoutResolver.clamp(FxPoint(x, y), input), input, animate)
    }

    fun moveBy(dx: Float, dy: Float, animate: Boolean) {
        val s = scope ?: return
        val cur = s.container.contentPosition()
        moveTo(cur.x + dx, cur.y + dy, animate)
    }

    fun onDragStart() = cancelAnimator()

    /** 拖动中：rebound 时允许暂时出界，否则实时 clamp */
    fun onDrag(dx: Float, dy: Float) {
        val s = scope ?: return
        val input = s.layoutInput() ?: return
        val cur = s.container.contentPosition()
        val next = FxPoint(cur.x + dx, cur.y + dy)
        val rebound = (s.config.adsorb as? FxAdsorb.Edges)?.rebound ?: false
        apply(s, if (rebound) next else FxLayoutResolver.clamp(next, input))
    }

    fun onDragEnd() {
        val s = scope ?: return
        val input = s.layoutInput() ?: return
        val target = FxAdsorbResolver.target(s.container.contentPosition(), input, s.config.adsorb)
        settle(s, target, input, animate = true)
    }

    private fun settle(s: FxFeatureScope, target: FxPoint, input: FxLayoutInput, animate: Boolean) {
        cancelAnimator()
        val from = s.container.contentPosition()
        if (!animate || from == target) {
            apply(s, target)
            commit(s, target, input)
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SETTLE_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val f = va.animatedValue as Float
                apply(s, FxPoint(from.x + (target.x - from.x) * f, from.y + (target.y - from.y) * f))
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) {
                    animator = null
                    if (cancelled) return
                    apply(s, target)
                    commit(s, target, input)
                }
            })
            start()
        }
    }

    private fun apply(s: FxFeatureScope, p: FxPoint) {
        s.host.updateLayout(s.container, FxLayoutSpec(p.x, p.y, s.control.anchor.gravity, s.container.isLtr))
    }

    private fun commit(s: FxFeatureScope, p: FxPoint, input: FxLayoutInput) {
        s.commitAnchor(FxLayoutResolver.toAnchor(p, input))
    }

    private fun cancelAnimator() {
        animator?.cancel()
        animator = null
    }

    private companion object {
        const val SETTLE_DURATION = 200L
    }
}
