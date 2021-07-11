package com.petterp.floatingx.listener

import android.widget.FrameLayout

/**
 * @Author petterp
 * @Date 2021/7/11-3:27 下午
 * @Email ShiyihuiCloud@163.com
 * @Function fx的动画接口,实现此接口,实现自己的动画效果
 * 默认实现示例 FxAnimationImpl
 */
interface IFxAnimation {

    /** 返回动画执行时间 */
    val animatorTime: Long

    /** 开始动画 */
    fun startAnimation(view: FrameLayout?)

    /** 结束动画 */
    fun endAnimation(view: FrameLayout?)

    /** 停止动画 */
    fun cancelAnimation()
}
