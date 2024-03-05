package com.petterp.floatingx.app.simple

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.widget.FrameLayout
import com.petterp.floatingx.assist.FxAnimation

/**
 * Fx的动画示例
 * */
class FxAnimationImpl(private val defaultTime: Long = 1000L) : FxAnimation() {
    private val values = floatArrayOf(
        0f,
        0.95f,
        0.95f,
        0.85f,
        0.85f,
        0.95f,
        0.95f,
        0.9f,
        0.9f,
        1f,
    )

    override fun fromAnimator(view: FrameLayout?): Animator {
        val scaleX = ObjectAnimator.ofFloat(
            view,
            "scaleX",
            *values,
        )
        val scaleY = ObjectAnimator.ofFloat(
            view,
            "scaleY",
            *values,
        )
        val alpha = ObjectAnimator.ofFloat(
            view,
            "alpha",
            0f,
            1f,
        )
        return AnimatorSet().apply {
            duration = defaultTime
            play(scaleX).with(scaleY).with(alpha)
        }
    }

    override fun toAnimator(view: FrameLayout?): Animator {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0f)
        val alpha = ObjectAnimator.ofFloat(
            view,
            "alpha",
            1f,
            0f,
        )
        return AnimatorSet().apply {
            duration = defaultTime
            play(scaleX).with(scaleY).with(alpha)
        }
    }
}
