package com.petterp.floatingx.system.feature

import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope
import com.petterp.floatingx.system.container.FxWindowContainer

/**
 * 把 config.gesture.touchable 映射成窗口的 FLAG_NOT_TOUCHABLE（spec §4）。
 * Layer 容器里 GestureFeature 自己会透传触摸，这里只管系统窗口，容器不是 [FxWindowContainer] 时忽略。
 */
internal class SystemWindowFeature : FxFeature {

    private var container: FxWindowContainer? = null

    override fun onAttach(scope: FxFeatureScope) {
        // 先无条件赋值：降级到别的容器后重新 attach 时，必须把上一轮的旧容器清掉
        container = scope.container as? FxWindowContainer
        val c = container ?: return
        c.setWindowTouchable(scope.config.gesture.touchable)
    }

    override fun onDetach() {
        container = null
    }

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        if (old.gesture.touchable != new.gesture.touchable) container?.setWindowTouchable(new.gesture.touchable)
    }
}
