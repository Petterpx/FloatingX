package com.petterp.floatingx.util

import android.animation.Animator
import android.animation.Animator.AnimatorListener

/**
 *
 * @author petterp
 */
class SimpleAnimatorListener(
    var start: (() -> Unit)? = null,
    var end: (() -> Unit)? = null
) : AnimatorListener {
    override fun onAnimationStart(animation: Animator) {
        start?.invoke()
    }

    override fun onAnimationEnd(animation: Animator) {
        end?.invoke()
    }

    override fun onAnimationCancel(animation: Animator) {
        end?.invoke()
    }

    override fun onAnimationRepeat(animation: Animator) {
    }
}
