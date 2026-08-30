package com.petterp.floatingx.core.feature

import android.animation.Animator
import android.animation.AnimatorListenerAdapter

/** 播放 config.animation 的显示/隐藏动画；无动画时立即回调 */
internal class AnimationFeature : FxFeature {

    private var scope: FxFeatureScope? = null
    private var running: Animator? = null

    override fun onAttach(scope: FxFeatureScope) { this.scope = scope }

    override fun onDetach() {
        running?.cancel()
        running = null
        scope = null
    }

    override fun onShow() {
        val s = scope ?: return
        val anim = s.config.animation ?: return
        val v = s.container.contentView ?: return
        running?.cancel()
        running = anim.showAnimator(v).also { it.start() }
    }

    fun playHide(onEnd: () -> Unit) {
        val s = scope
        val anim = s?.config?.animation
        val v = s?.container?.contentView
        if (anim == null || v == null) {
            onEnd()
            return
        }
        running?.cancel()
        running = anim.hideAnimator(v).apply {
            addListener(object : AnimatorListenerAdapter() {
                /**
                 * cancel() 会先后触发 onAnimationCancel + onAnimationEnd。
                 * 隐藏动画被中途取消（多半是紧接着又 show 了）时必须跳过 onEnd，
                 * 否则会把刚显示出来的内容重新置为 INVISIBLE，导致浮窗永久看不见。
                 */
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) {
                    running = null
                    if (cancelled) return
                    onEnd()
                }
            })
            start()
        }
    }
}
