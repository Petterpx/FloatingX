package com.petterp.floatingx.core.layout

/** 可吸附的逻辑边 */
public enum class FxEdge { START, END, TOP, BOTTOM }

/** 半隐比例：贴 START 边时隐藏 start 比例，贴 END 边时隐藏 end 比例（#204 左右可不同） */
public data class FxHalfHide(val start: Float, val end: Float = start) {
    init {
        require(start in 0f..1f && end in 0f..1f) { "halfHide 比例必须在 0..1 之间: start=$start end=$end" }
    }
}

/** 拖动结束后的吸附策略 */
public sealed class FxAdsorb {

    public object None : FxAdsorb()

    /**
     * @param edges 允许吸附的边
     * @param halfHide 贴左右边时的半隐比例，null 表示不半隐
     * @param rebound 拖动过程中允许暂时超出可用区、松手后回弹
     */
    public data class Edges(
        val edges: Set<FxEdge>,
        val halfHide: FxHalfHide? = null,
        val rebound: Boolean = true,
    ) : FxAdsorb()

    public companion object {
        @JvmStatic
        public fun none(): FxAdsorb = None

        @JvmStatic
        @JvmOverloads
        public fun horizontal(halfHide: FxHalfHide? = null, rebound: Boolean = true): FxAdsorb =
            Edges(setOf(FxEdge.START, FxEdge.END), halfHide, rebound)

        @JvmStatic
        @JvmOverloads
        public fun vertical(rebound: Boolean = true): FxAdsorb =
            Edges(setOf(FxEdge.TOP, FxEdge.BOTTOM), null, rebound)

        @JvmStatic
        @JvmOverloads
        public fun all(halfHide: FxHalfHide? = null, rebound: Boolean = true): FxAdsorb =
            Edges(FxEdge.entries.toSet(), halfHide, rebound)
    }
}
