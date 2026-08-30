package com.petterp.floatingx.compose

import android.os.Looper
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.WeakHashMap

/**
 * 一个 control 的两条 flow。只装数据，不持有 control——它是 WeakHashMap 的 value，
 * 反向强引用会让弱 key 永远回收不掉（value 由 map 强持有）。
 */
private class FxControlFlows(state: FxState, position: FxPoint) {
    val state: MutableStateFlow<FxState> = MutableStateFlow(state)
    val position: MutableStateFlow<FxPoint> = MutableStateFlow(position)
}

/**
 * 把 core 的回调桥接到 flow，无 core 改动。
 * 由 control 持有：cancel 后 core 清空 listeners，桥随 control 一起回收。
 */
private class FxFlowsBridge(private val flows: FxControlFlows) : FxListener {

    // FxEngine.transition 是「先回调、后改 state」，回调里读 control.state 拿到的还是旧值，
    // 所以这里直接写这次转移的目标状态，而不是读 control.state。
    override fun onAttach(control: FxControl) { flows.state.value = FxState.ATTACHED }

    /** host 丢失时转 INSTALLED；cancel 也会走一次 detach，紧跟着的 onCancel 会同步改写成 CANCELLED */
    override fun onDetach(control: FxControl) { flows.state.value = FxState.INSTALLED }

    override fun onShow(control: FxControl) { flows.state.value = FxState.SHOWN }

    override fun onHide(control: FxControl) { flows.state.value = FxState.ATTACHED }

    override fun onCancel(control: FxControl) { flows.state.value = FxState.CANCELLED }

    // 三个位置回调都是投影完成之后才广播的（GestureFeature 的拖动、
    // LocationFeature.commitAndApply 的先 apply 再 commitAnchor），此刻 control.position 已是新坐标
    override fun onDrag(control: FxControl, x: Float, y: Float) { flows.position.value = control.position }

    override fun onDragEnd(control: FxControl, x: Float, y: Float) { flows.position.value = control.position }

    override fun onPositionChanged(control: FxControl, anchor: FxAnchor) { flows.position.value = control.position }
}

/** key 用 control 身份（FxControl 实现没有覆写 equals/hashCode），只在主线程访问 */
private val flows = WeakHashMap<FxControl, FxControlFlows>()

private fun FxControl.flows(): FxControlFlows {
    // 检查放在最前：注册完 listener 再抛线程异常会留下一个野监听器，初始化必须要么全成要么全不成
    check(Looper.myLooper() == Looper.getMainLooper()) { "stateFlow()/positionFlow() 必须在主线程调用" }
    return flows.getOrPut(this) {
        FxControlFlows(state, position).also { addListener(FxFlowsBridge(it)) }
    }
}

/**
 * 浮窗状态。必须在主线程调用；flow 也只在主线程更新。同一 control 多次调用返回同一条 flow。
 * cancel() 之后 core 会清空 listeners，此时（或之后）拿到的 flow 停在 CANCELLED 上，不再更新。
 */
public fun FxControl.stateFlow(): StateFlow<FxState> = flows().state

/**
 * 内容左上角的**屏幕坐标**（等同 `control.position`，拖动中每帧更新、moveTo/吸附结束更新）。
 * 注意与 `FxListener.onDrag/onDragEnd` 回调里的 x/y 不同——那两个是相对容器的坐标。
 *
 * 必须在主线程调用；flow 也只在主线程更新。同一 control 多次调用返回同一条 flow。
 * cancel() 之后 core 会清空 listeners，此时（或之后）拿到的 flow 停在最后一次坐标上，不再更新。
 */
public fun FxControl.positionFlow(): StateFlow<FxPoint> = flows().position
