package com.petterp.floatingx.assist

import android.animation.Animator
import android.widget.FrameLayout

/**
 * fx的动画辅助类
 * 默认实现示例 见simple - FxAnimationImpl
 */
abstract class FxAnimation {

    private var startAnimatorJob: Animator? = null
    private var endAnimatorJob: Animator? = null

    internal val fromJobRunning: Boolean
        get() = startAnimatorJob?.isRunning ?: false
    internal val endJobRunning: Boolean
        get() = endAnimatorJob?.isRunning ?: false

    private val Animator.animatorDuration: Long
        get() = duration + startDelay

    /** 开始动画 */
    abstract fun fromAnimator(view: FrameLayout?): Animator

    /** 结束动画 */
    abstract fun toAnimator(view: FrameLayout?): Animator

    @JvmName(" fromStartAnimator")
    internal fun fromStartAnimator(view: FrameLayout?): Long {
        startAnimatorJob?.cancel()
        startAnimatorJob = fromAnimator(view)
        if (startAnimatorJob?.isRunning == true) {
            return startAnimatorJob?.duration ?: 0
        }
        startAnimatorJob?.start()
        return startAnimatorJob?.animatorDuration ?: 0
    }

    @JvmName(" toEndAnimator")
    internal fun toEndAnimator(view: FrameLayout?): Long {
        endAnimatorJob?.cancel()
        endAnimatorJob = toAnimator(view)
        if (endAnimatorJob?.isRunning == true) {
            return endAnimatorJob?.duration ?: 0
        }
        endAnimatorJob?.start()
        return endAnimatorJob?.animatorDuration ?: 0
    }

    fun cancelAnimation() {
        startAnimatorJob?.cancel()
        endAnimatorJob?.cancel()
        startAnimatorJob = null
        endAnimatorJob = null
    }
}
