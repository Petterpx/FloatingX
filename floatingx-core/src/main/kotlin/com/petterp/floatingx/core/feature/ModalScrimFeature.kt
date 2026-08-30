package com.petterp.floatingx.core.feature

import com.petterp.floatingx.core.container.FxLayerContainer

/**
 * 拦截内容之外的触摸（#212），可选点击外部自动 hide（#151）。仅对 Layer 容器生效。
 * 只在浮窗**显示中**才拦截：hide 之后触摸照常透传（否则 dismissOnOutsideTouch 一藏，整页就全点不动了）。
 */
public class ModalScrimFeature @JvmOverloads constructor(
    private val dismissOnOutsideTouch: Boolean = false,
) : FxFeature {

    private var container: FxLayerContainer? = null

    override fun onAttach(scope: FxFeatureScope) {
        if (!scope.container.isLayer) {
            scope.logger?.e("ModalScrimFeature 仅支持 Layer 容器（app/scope），当前容器为 ${scope.container::class.java.simpleName}")
            return
        }
        val c = scope.container as FxLayerContainer
        c.modal = true
        c.onOutsideTouch = { if (dismissOnOutsideTouch) scope.control.hide() }
        container = c
    }

    override fun onDetach() {
        container?.let {
            it.modal = false
            it.onOutsideTouch = null
        }
        container = null
    }
}
