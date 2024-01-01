package com.petterp.floatingx.view.basic

/**
 * FxView 边界配置
 * @author petterp
 */
class FxViewBoundaryConfig(
    var minW: Float = 0f,
    var maxW: Float = 0f,
    var minH: Float = 0f,
    var maxH: Float = 0f
) {
    fun copy(other: FxViewBoundaryConfig): FxViewBoundaryConfig {
        this.minW = other.minW
        this.maxW = other.maxW
        this.minH = other.minH
        this.maxH = other.maxH
        return this
    }
}
