package com.petterp.floatingx.core.layout

/** 吸附目标点的纯函数 */
public object FxAdsorbResolver {

    /** 拖动结束后的停靠点。None → 仅 clamp */
    @JvmStatic
    public fun target(point: FxPoint, input: FxLayoutInput, adsorb: FxAdsorb): FxPoint {
        val p = FxLayoutResolver.clamp(point, input)
        val edges = adsorb as? FxAdsorb.Edges ?: return p
        val edge = nearestEdge(p, input, edges.edges) ?: return p
        val a = input.area
        val w = input.size.width
        val h = input.size.height
        val hide = edges.halfHide
        return when (edge) {
            FxEdge.START -> p.copy(
                x = if (input.ltr) a.left - w * (hide?.start ?: 0f) else a.right - w + w * (hide?.start ?: 0f),
            )
            FxEdge.END -> p.copy(
                x = if (input.ltr) a.right - w + w * (hide?.end ?: 0f) else a.left - w * (hide?.end ?: 0f),
            )
            FxEdge.TOP -> p.copy(y = a.top)
            FxEdge.BOTTOM -> p.copy(y = a.bottom - h)
        }
    }

    /** 距离最近的启用边（逻辑边，已考虑 ltr）；edges 为空返回 null */
    @JvmStatic
    public fun nearestEdge(point: FxPoint, input: FxLayoutInput, edges: Set<FxEdge>): FxEdge? {
        if (edges.isEmpty()) return null
        val a = input.area
        val distLeft = point.x - a.left
        val distRight = (a.right - input.size.width) - point.x
        var best: FxEdge? = null
        var bestDist = Float.MAX_VALUE
        for (edge in edges) {
            val d = when (edge) {
                FxEdge.START -> if (input.ltr) distLeft else distRight
                FxEdge.END -> if (input.ltr) distRight else distLeft
                FxEdge.TOP -> point.y - a.top
                FxEdge.BOTTOM -> (a.bottom - input.size.height) - point.y
            }
            if (d < bestDist) {
                bestDist = d
                best = edge
            }
        }
        return best
    }
}
