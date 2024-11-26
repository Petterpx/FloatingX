package com.petterp.floatingx.assist

import android.animation.Animator
import android.widget.FrameLayout
import com.petterp.floatingx.assist.helper.FxBuilderDsl
import com.petterp.floatingx.util.SimpleAnimatorListener

/**
 * fx的动画辅助类
 * 默认实现示例 见simple - FxAnimationImpl
 */
@FxBuilderDsl
abstract class FxAnimation {

    private var startAnimatorJob: Animator? = null
    private var endAnimatorJob: Animator? = null
    private val startListener = SimpleAnimatorListener()
    private val endListener = SimpleAnimatorListener()

    /** 开始动画 */
    abstract fun fromAnimator(view: FrameLayout?): Animator

    /** 结束动画 */
    abstract fun toAnimator(view: FrameLayout?): Animator

    fun cancelAnimation() {
        startAnimatorJob?.cancel()
        endAnimatorJob?.cancel()
        startAnimatorJob = null
        endAnimatorJob = null
    }

    @JvmSynthetic
    internal fun fromJobIsRunning(): Boolean = startAnimatorJob?.isRunning ?: false

    @JvmSynthetic
    internal fun endJobIsRunning(): Boolean = endAnimatorJob?.isRunning ?: false

    @JvmSynthetic
    internal fun fromStartAnimator(view: FrameLayout?): Boolean {
        if (startAnimatorJob == null) {
            startAnimatorJob = fromAnimator(view)
            startAnimatorJob?.removeListener(startListener)
            startAnimatorJob?.addListener(startListener)
        }
        if (endJobIsRunning()) endAnimatorJob?.cancel()
        startAnimatorJob?.start()
        return true
    }

    @JvmSynthetic
    internal fun toEndAnimator(view: FrameLayout?): Boolean {
        if (endAnimatorJob == null) {
            endAnimatorJob = toAnimator(view)
            endAnimatorJob?.removeListener(endListener)
            endAnimatorJob?.addListener(endListener)
        }
        if (fromJobIsRunning()) startAnimatorJob?.cancel()
        endAnimatorJob?.start()
        return true
    }

    @JvmSynthetic
    internal fun setFromAnimatorListener(obj: (() -> Unit)?) {
        startListener.end = obj
    }

    @JvmSynthetic
    internal fun setEndAnimatorListener(obj: (() -> Unit)?) {
        endListener.end = obj
    }
}
