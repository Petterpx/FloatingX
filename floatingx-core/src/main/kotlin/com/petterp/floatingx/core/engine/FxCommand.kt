package com.petterp.floatingx.core.engine

/** 需要容器已挂载才能执行的命令；未 ready 时由 FxEngine 排队 */
internal sealed class FxCommand {
    /** 移动到容器坐标系下的绝对位置 */
    data class MoveTo(val x: Float, val y: Float, val animate: Boolean) : FxCommand()

    /** 相对当前位置偏移 */
    data class MoveBy(val dx: Float, val dy: Float, val animate: Boolean) : FxCommand()
}
