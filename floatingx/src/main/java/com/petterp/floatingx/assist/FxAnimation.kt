package com.petterp.floatingx.assist

import android.animation.Animator
import android.widget.FrameLayout

/**
 * @Author petterp
 * @Date 2021/7/11-3:27 下午
 * @Email ShiyihuiCloud@163.com
 * @Function fx的动画辅助类
 * 默认实现示例 FxAnimationImpl
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

    internal fun fromStartAnimator(view: FrameLayout?): Long {
        startAnimatorJob?.cancel()
        startAnimatorJob = fromAnimator(view)
        if (startAnimatorJob?.isRunning == true) {
            return startAnimatorJob?.duration ?: 0
        }
        startAnimatorJob?.start()
        return startAnimatorJob?.animatorDuration ?: 0
    }

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
