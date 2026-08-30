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

    /** 容器从 host 卸下之前调用；必须释放持有的 scope。host 丢失/重新挂载会成对出现多次 */
    public fun onDetach()

    /**
     * control.cancel() 时调用，在最后一次 onDetach 之后、host.release() 之前，整个生命周期只有一次。
     * 用于释放跨 attach 周期的资源（如 compose 的 owner destroy），普通 detach 不该释放的东西放这里。
     */
    public fun onCancel() {}
    public fun onConfigChanged(old: FxConfig, new: FxConfig) {}
    public fun onContentSizeChanged(size: FxSize) {}
    public fun onBoundsChanged() {}
    public fun onShow() {}
    public fun onHide() {}
}
