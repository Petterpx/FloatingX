package com.petterp.floatingx.impl.simple

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.widget.FrameLayout
import com.petterp.floatingx.assist.FxAnimation

/**
 * @Author petterp
 * @Date 2021/7/11-4:07 下午
 * @Email ShiyihuiCloud@163.com
 * @Function Fx的动画示例
 */
class FxAnimationImpl(private val defaultTime: Long = 1000L) : FxAnimation() {

    override fun fromAnimator(view: FrameLayout?): Animator {
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
            0f
        )
        return AnimatorSet().apply {
            duration = defaultTime
            play(scaleX).with(scaleY).with(alpha)
        }
    }
}
