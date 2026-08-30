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
        val c = scope.container as? FxWindowContainer ?: return
        container = c
        c.setWindowTouchable(scope.config.gesture.touchable)
    }

    override fun onDetach() {
        container = null
    }

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        if (old.gesture.touchable != new.gesture.touchable) container?.setWindowTouchable(new.gesture.touchable)
    }
}
