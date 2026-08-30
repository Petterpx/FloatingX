package com.petterp.floatingx.core

import android.view.View
import com.petterp.floatingx.core.layout.FxAnchor

/** 浮窗事件监听；全部有默认空实现，Java 端得益于 jvmDefault=enable 也只需覆写用到的方法 */
public interface FxListener {
    public fun onAttach(control: FxControl) {}
    public fun onDetach(control: FxControl) {}
    public fun onShow(control: FxControl) {}
    public fun onHide(control: FxControl) {}
    public fun onClick(control: FxControl, view: View) {}
    public fun onLongClick(control: FxControl, view: View) {}
    public fun onDragStart(control: FxControl) {}

    /** 每个 MOVE 都回调（#199），x/y 为内容相对容器坐标 */
    public fun onDrag(control: FxControl, x: Float, y: Float) {}
    public fun onDragEnd(control: FxControl, x: Float, y: Float) {}

    /** 锚点提交（拖动/吸附结束、moveTo 完成、update{anchor}）后回调，可用于判断贴在哪边（#148） */
    public fun onPositionChanged(control: FxControl, anchor: FxAnchor) {}
    public fun onCancel(control: FxControl) {}
}
