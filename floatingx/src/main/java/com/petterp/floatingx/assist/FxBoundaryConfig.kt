package com.petterp.floatingx.assist

/**
 * FxView 边界配置
 * @author petterp
 */
data class FxBoundaryConfig(
    var minW: Float = 0f,
    var maxW: Float = 0f,
    var minH: Float = 0f,
    var maxH: Float = 0f
) {
    fun copy(other: FxBoundaryConfig): FxBoundaryConfig {
        this.minW = other.minW
        this.maxW = other.maxW
        this.minH = other.minH
        this.maxH = other.maxH
        return this
    }
}
