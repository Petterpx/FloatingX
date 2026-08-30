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
                override fun onAnimationEnd(animation: Animator) {
                    running = null
                    onEnd()
                }
            })
            start()
        }
    }
}
