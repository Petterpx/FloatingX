package com.petterp.floatingx.assist

/**
 * Fx浮窗的显示模式
 * @author petterp
 */
enum class FxDisplayMode {
    // 默认模式: 可正常移动和响应点击事件
    Normal,

    // 固定模式：不能移动，只能响应点击事件
    ClickOnly,

    // 展示模式：只用于展示，不响应任何事件不能移动
    DisplayOnly,

    // 长按移动模式：长按后可移动，解决内容滑动冲突
    LongPressMove;

    val canMove: Boolean
        get() = this == Normal

    val canLongPressMove: Boolean
        get() = this == LongPressMove
}
