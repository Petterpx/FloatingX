package com.petterp.floatingx.core.layout

/** 锚点 ↔ 左上角坐标 的纯函数集合，无任何 Android 依赖 */
public object FxLayoutResolver {

    /** 锚点 → 内容左上角坐标（已 clamp） */
    @JvmStatic
    public fun resolve(anchor: FxAnchor, input: FxLayoutInput): FxPoint {
        val a = input.area
        val w = input.size.width
        val h = input.size.height
        val x = when (physical(anchor.gravity.horizontal, input.ltr)) {
            FxHorizontal.START -> a.left + anchor.dx
            FxHorizontal.END -> a.right - w - anchor.dx
            FxHorizontal.CENTER -> a.centerX - w / 2f + anchor.dx
        }
        val y = when (anchor.gravity.vertical) {
            FxVertical.TOP -> a.top + anchor.dy
            FxVertical.BOTTOM -> a.bottom - h - anchor.dy
            FxVertical.CENTER -> a.centerY - h / 2f + anchor.dy
        }
        return clamp(FxPoint(x, y), input)
    }

    /** 钳制到可用区；overflow 打开的边不设限 */
    @JvmStatic
    public fun clamp(point: FxPoint, input: FxLayoutInput): FxPoint {
        val a = input.area
        val w = input.size.width
        val h = input.size.height
        val minX = if (input.overflow.left) Float.NEGATIVE_INFINITY else a.left
        val maxX = if (input.overflow.right) Float.POSITIVE_INFINITY else a.right - w
        val minY = if (input.overflow.top) Float.NEGATIVE_INFINITY else a.top
        val maxY = if (input.overflow.bottom) Float.POSITIVE_INFINITY else a.bottom - h
        return FxPoint(clampAxis(point.x, minX, maxX, a.left), clampAxis(point.y, minY, maxY, a.top))
    }

    /** 左上角坐标 → 最近的物理边组合（左/右 × 上/下）对应的逻辑锚点；拖动结束后调用 */
    @JvmStatic
    public fun toAnchor(point: FxPoint, input: FxLayoutInput): FxAnchor {
        val a = input.area
        val w = input.size.width
        val h = input.size.height
        val distLeft = point.x - a.left
        val distRight = (a.right - w) - point.x
        val nearLeft = distLeft <= distRight
        val dx = if (nearLeft) distLeft else distRight
        val distTop = point.y - a.top
        val distBottom = (a.bottom - h) - point.y
        val nearTop = distTop <= distBottom
        val dy = if (nearTop) distTop else distBottom
        val horizontal = physical(if (nearLeft) FxHorizontal.START else FxHorizontal.END, input.ltr)
        val vertical = if (nearTop) FxVertical.TOP else FxVertical.BOTTOM
        return FxAnchor(FxGravity.of(horizontal, vertical), dx, dy)
    }

    /** 逻辑 START/END ↔ 物理左/右；LTR 恒等，RTL 互换。该映射自反，两个方向都用它 */
    private fun physical(h: FxHorizontal, ltr: Boolean): FxHorizontal = if (ltr) {
        h
    } else {
        when (h) {
            FxHorizontal.START -> FxHorizontal.END
            FxHorizontal.END -> FxHorizontal.START
            FxHorizontal.CENTER -> FxHorizontal.CENTER
        }
    }

    /** 内容比可用区还大（max < min）时靠 start 对齐，避免 coerceIn 抛异常 */
    private fun clampAxis(value: Float, min: Float, max: Float, fallback: Float): Float =
        if (max < min) fallback else value.coerceIn(min, max)
}
