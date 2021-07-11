package com.petterp.floatingx.impl

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.widget.FrameLayout
import com.petterp.floatingx.listener.IFxAnimation

/**
 * @Author petterp
 * @Date 2021/7/11-4:07 下午
 * @Email ShiyihuiCloud@163.com
 * @Function Fx的动画示例
 */
class FxAnimationImpl(override val animatorTime: Long = 300L) : IFxAnimation {

    private var startAnimationJob: AnimatorSet? = null
    private var endAnimationJob: AnimatorSet? = null

    override fun startAnimation(view: FrameLayout?) {
        val scaleX = ObjectAnimator.ofFloat(
            view,
            "scaleX",
            0f,
            0.95f,
            0.95f,
            0.85f,
            0.85f,
            0.95f,
            0.95f,
            0.9f,
            0.9f,
            1f
        )
        val scaleY = ObjectAnimator.ofFloat(
            view,
            "scaleY",
            0f,
            0.95f,
            0.95f,
            0.85f,
            0.85f,
            0.95f,
            0.95f,
            0.9f,
            0.9f,
            1f
        )
        val alpha = ObjectAnimator.ofFloat(
            view,
            "alpha",
            0f,
            1f
        )
        AnimatorSet().apply {
            startAnimationJob = this
            duration = animatorTime
            play(scaleX).with(scaleY).with(alpha)
            start()
        }
    }

    override fun endAnimation(view: FrameLayout?) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0f)
        val alpha = ObjectAnimator.ofFloat(
            view,
            "alpha",
            1f,
            0f
        )
        AnimatorSet().apply {
            endAnimationJob = this
            duration = animatorTime
            play(scaleX).with(scaleY).with(alpha)
            start()
        }
    }

    override fun cancelAnimation() {
        startAnimationJob?.cancel()
        startAnimationJob = null
        endAnimationJob?.cancel()
        endAnimationJob = null
    }
}
