package com.petterp.floatingx.compose

import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxLayoutResolver
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
 * 把 core 的回调桥接到 flow，无 core 改动。同时是 listener（状态/位置事件）与 feature
 * （只为拿到 FxFeatureScope.layoutInput()，本身不带任何行为）。
 * 由 control 持有：cancel 后 core 清空 listeners，feature 随 control 一起回收。
 */
private class FxFlowsBridge(private val flows: FxControlFlows) : FxListener, FxFeature {

    private var scope: FxFeatureScope? = null

    // ---------- FxFeature ----------

    override fun onAttach(scope: FxFeatureScope) { this.scope = scope }

    override fun onDetach() { scope = null }

    // ---------- FxListener ----------

    // FxEngine.transition 是「先回调、后改 state」，回调里读 control.state 拿到的还是旧值，
    // 所以这里直接写这次转移的目标状态，而不是读 control.state。
    override fun onAttach(control: FxControl) { flows.state.value = FxState.ATTACHED }

    /** host 丢失时转 INSTALLED；cancel 也会走一次 detach，紧跟着的 onCancel 会同步改写成 CANCELLED */
    override fun onDetach(control: FxControl) { flows.state.value = FxState.INSTALLED }

    override fun onShow(control: FxControl) { flows.state.value = FxState.SHOWN }

    override fun onHide(control: FxControl) { flows.state.value = FxState.ATTACHED }

    override fun onCancel(control: FxControl) { flows.state.value = FxState.CANCELLED }

    // 拖动是投影完成之后才广播的（GestureFeature），此刻 position 已是新坐标
    override fun onDrag(control: FxControl, x: Float, y: Float) { flows.position.value = control.position }

    override fun onDragEnd(control: FxControl, x: Float, y: Float) { flows.position.value = control.position }

    /**
     * 锚点提交发生在投影之前（LocationFeature.commitAndApply 先 commitAnchor 再 apply），
     * 此刻 control.position 还是旧坐标；所以按锚点算出即将应用的容器内坐标，
     * 再用「新屏幕坐标 = 旧屏幕坐标 + 容器内位移」换算——位移在两种容器里都等于屏幕位移，三种 host 通用。
     */
    override fun onPositionChanged(control: FxControl, anchor: FxAnchor) {
        flows.position.value = resolvePending(anchor) ?: control.position
    }

    private fun resolvePending(anchor: FxAnchor): FxPoint? {
        val s = scope ?: return null
        val input = s.layoutInput() ?: return null
        val target = FxLayoutResolver.resolve(anchor, input)
        val local = s.container.contentPosition()
        val screen = s.container.contentPositionOnScreen()
        return FxPoint(screen.x + (target.x - local.x), screen.y + (target.y - local.y))
    }
}

/** key 用 control 身份（FxControl 实现没有覆写 equals/hashCode），只在主线程访问 */
private val flows = WeakHashMap<FxControl, FxControlFlows>()

private fun FxControl.flows(): FxControlFlows = flows.getOrPut(this) {
    FxControlFlows(state, position).also { f ->
        val bridge = FxFlowsBridge(f)
        addListener(bridge)
        addFeature(bridge)
    }
}

/** 浮窗状态；主线程更新。同一 control 多次调用返回同一条 flow */
public fun FxControl.stateFlow(): StateFlow<FxState> = flows().state

/** 内容左上角屏幕坐标（拖动中每帧更新、moveTo/吸附结束更新）；主线程更新 */
public fun FxControl.positionFlow(): StateFlow<FxPoint> = flows().position
