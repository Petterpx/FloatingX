package com.petterp.floatingx.core.animation

import android.animation.Animator
import android.view.View

/** 显示 / 隐藏动画。作用对象是内容 view，不是容器 */
public abstract class FxAnimation {
    public abstract fun showAnimator(view: View): Animator
    public abstract fun hideAnimator(view: View): Animator
}
