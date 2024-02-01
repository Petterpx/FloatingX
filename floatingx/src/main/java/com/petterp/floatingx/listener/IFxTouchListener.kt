package com.petterp.floatingx.listener

/**
 * Fx手势监听器
 * @author petterp
 */
import android.view.MotionEvent
import com.petterp.floatingx.view.IFxInternalView

/**
 * fx监听事件，用于监听浮窗上的一些手势事件
 *
 * 默认提供了一些常见的事件场景，比如按下、松开、浮窗位置移动,如需监听所有，请在eventIng中监听所有事件
 * */
interface IFxTouchListener {

    /** 按下 */
    fun down() {}

    /** 松开 */
    fun up() {}

    /** 实现此方法，可实现类似TouchListener的拦截效果
     * @param event 当前事件
     * @param control 浮窗控制器,便于自行实现一些调度
     * @return true:拦截当前事件处理，
     * @return false:未处理,走系统默认事件
     * */
    fun onTouch(event: MotionEvent, control: IFxInternalView?): Boolean = false

    /**
     * 监测当前移动浮窗的手指 move回调
     *
     * @param event 当前手势事件
     * @param x 当前浮窗相对于父View的x坐标
     * @param y 当前浮窗相对于父View的y坐标.
     * @since x,y代表了当前浮窗相对于父View的位置信息,即当前浮窗(左上角坐标)。
     * App与Activity级别时一般相当于其相对于屏幕的位置。
     * View级别相当于其相当于父ViewGroup
     *
     * 注意：[displayMode == FxDisplayMode.ClickOnly] 时，x,y不会变化
     *
     * 注意：这个方法仅会回调当前正在按压的手指事件
     *
     * */
    fun dragIng(event: MotionEvent, x: Float, y: Float) {}

    /** 接收所有event,用于自定义判断逻辑,会在onTouchEvent中被优先永远调用 */
    @Deprecated(
        "此方法不推荐使用，建议使用onTouch，后续版本将废弃",
        replaceWith = ReplaceWith("onTouch"),
    )
    fun eventIng(event: MotionEvent) {
    }
}
