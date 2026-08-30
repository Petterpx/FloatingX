package com.petterp.floatingx.core.feature

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.host.FxLayoutSpec
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxAdsorbResolver
import com.petterp.floatingx.core.layout.FxAnchor
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

    /** 动画中的 settle 目标；锚点尚未提交时非空，relayout 会拿最新 input 替它收尾 */
    private var settleTarget: FxPoint? = null

    /** 内容尺寸无效时收到的 moveTo，首次有效布局后应用 */
    private var pending: FxPoint? = null

    /** 一次拖动期间复用的布局输入：MOVE 路径不再每帧构造 FxLayoutInput/FxRect */
    private var dragInput: FxLayoutInput? = null

    override fun onAttach(scope: FxFeatureScope) {
        this.scope = scope
        relayout()
    }

    override fun onDetach() {
        discardSettle()
        dragInput = null
        scope = null
    }

    override fun onContentSizeChanged(size: FxSize) {
        dragInput = null
        relayout()
    }

    override fun onBoundsChanged() {
        dragInput = null
        relayout()
    }

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        // 未挂载时也会收到（detach 期间 update {}）：没有 scope 就没有可投影的容器，
        // 新配置会在下一次 onAttach → relayout 里生效
        scope ?: return
        if (old.anchor != new.anchor) {
            // 用户显式改锚点：优先级高于在飞的 settle，直接丢弃，别让它把新锚点覆盖回去
            discardSettle()
            relayout()
            return
        }
        if (old.margin != new.margin || old.overflow != new.overflow || old.safeArea != new.safeArea) relayout()
    }

    fun relayout() {
        val s = scope ?: return
        val input = s.layoutInput() ?: return
        // 动画中的 settle 还没提交锚点：用最新 input 立刻收尾。
        // 否则动画结束时会拿旋转/改尺寸前的旧 input 反算锚点，并把 relayout 的结果覆盖掉。
        finishSettle(s, input)
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

    fun onDragStart() {
        val s = scope ?: return
        discardSettle()
        dragInput = s.layoutInput()
    }

    /** 拖动中：rebound 时允许暂时出界，否则实时 clamp */
    fun onDrag(dx: Float, dy: Float) {
        val s = scope ?: return
        val input = dragInput ?: return
        val cur = s.container.contentPosition()
        val next = FxPoint(cur.x + dx, cur.y + dy)
        val rebound = (s.config.adsorb as? FxAdsorb.Edges)?.rebound ?: false
        apply(s, if (rebound) next else FxLayoutResolver.clamp(next, input))
    }

    fun onDragEnd() {
        val s = scope ?: return
        val input = dragInput ?: s.layoutInput() ?: return
        dragInput = null
        val target = FxAdsorbResolver.target(s.container.contentPosition(), input, s.config.adsorb)
        settle(s, target, input, animate = true)
    }

    private fun settle(s: FxFeatureScope, target: FxPoint, input: FxLayoutInput, animate: Boolean) {
        discardSettle()
        val from = s.container.contentPosition()
        if (!animate || from == target) {
            commitAndApply(s, target, input)
            return
        }
        settleTarget = target
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
                    commitAndApply(s, target, input)
                }
            })
            start()
        }
    }

    /** 用最新 input 给在飞的 settle 收尾；target 先按新可用区 clamp，避免反算出负偏移 */
    private fun finishSettle(s: FxFeatureScope, input: FxLayoutInput) {
        val target = settleTarget ?: return
        cancelAnimator()
        commitAndApply(s, FxLayoutResolver.clamp(target, input), input)
    }

    /**
     * 先投影再提交锚点：commitAnchor 会广播 onPositionChanged，监听器在回调里读 control.position
     * 必须已经是新坐标（spec §2.3）。投影显式带上即将提交的新锚点，所以最后一帧不会带旧 gravity。
     */
    private fun commitAndApply(s: FxFeatureScope, target: FxPoint, input: FxLayoutInput) {
        settleTarget = null
        val anchor = FxLayoutResolver.toAnchor(target, input)
        apply(s, target, anchor)
        s.commitAnchor(anchor)
    }

    /** anchor 默认取 control 上已提交的那个；提交前的最后一帧要显式传新锚点 */
    private fun apply(s: FxFeatureScope, p: FxPoint, anchor: FxAnchor = s.control.anchor) {
        s.host.updateLayout(s.container, FxLayoutSpec(p.x, p.y, anchor, s.container.isLtr))
    }

    private fun discardSettle() {
        cancelAnimator()
        settleTarget = null
    }

    private fun cancelAnimator() {
        animator?.cancel()
        animator = null
    }

    private companion object {
        const val SETTLE_DURATION = 200L
    }
}
