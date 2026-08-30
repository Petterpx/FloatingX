package com.petterp.floatingx.core.animation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View

/** 内置动画 */
public object FxAnimations {

    @JvmStatic
    @JvmOverloads
    public fun fade(duration: Long = 200L): FxAnimation = object : FxAnimation() {
        override fun showAnimator(view: View): Animator = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).setDuration(duration)
        override fun hideAnimator(view: View): Animator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).setDuration(duration)
    }

    @JvmStatic
    @JvmOverloads
    public fun scale(duration: Long = 200L): FxAnimation = object : FxAnimation() {
        override fun showAnimator(view: View): Animator = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
        ).setDuration(duration)

        override fun hideAnimator(view: View): Animator = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f),
        ).setDuration(duration)
    }
}
