package com.petterp.floatingx.core.feature

import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.layout.FxSize

/**
 * 容器行为插件（spec §2.5）。core 内置 Location/Gesture/Animation 三个，
 * 用户与其他模块通过 config.addFeature / control.addFeature 注册。
 * feature 之间不可互相引用，需要共享的数据走 FxFeatureScope。
 */
public interface FxFeature {
    /** 容器挂载到 host 之后调用 */
    public fun onAttach(scope: FxFeatureScope)

    /** 容器从 host 卸下之前调用；必须释放持有的 scope */
    public fun onDetach()
    public fun onConfigChanged(old: FxConfig, new: FxConfig) {}
    public fun onContentSizeChanged(size: FxSize) {}
    public fun onBoundsChanged() {}
    public fun onShow() {}
    public fun onHide() {}
}
