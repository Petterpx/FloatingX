package com.petterp.floatingx.core.container

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.petterp.floatingx.core.layout.FxPoint
import com.petterp.floatingx.core.layout.FxSize

/** 容器把触摸事件交给它（GestureFeature 实现） */
public interface FxContainerTouchHandler {
    public fun onIntercept(ev: MotionEvent): Boolean
    public fun onTouch(ev: MotionEvent): Boolean
}

/**
 * 承载用户内容 view 的容器抽象。两种实现：
 * - FxLayerContainer：match_parent 覆盖层，内容用 translation 定位（app / scope）
 * - Window 容器（system 模块）：wrap_content 窗口，坐标写回 LayoutParams
 */
public interface FxContainer {
    /** 真正被加到 host 上的 View */
    public val view: ViewGroup
    public val contentView: View?
    public val isLtr: Boolean

    /** 是否是覆盖层容器（FxLayerContainer）。只对 Layer 生效的 feature 用它判断，避免各自 as? 强转 */
    public val isLayer: Boolean

    public fun setContent(view: View)

    /**
     * 解除与内容 view 的绑定：移除监听、把内容从容器里摘下来。
     * 换 host（容器换新）与 cancel 时调用，防止旧容器的监听留在内容 view 上（内存泄漏）。
     */
    public fun releaseContent()

    public fun contentSize(): FxSize
    public fun setContentPosition(x: Float, y: Float)

    /** 内容相对容器的坐标 */
    public fun contentPosition(): FxPoint

    /** 内容的屏幕坐标 */
    public fun contentPositionOnScreen(): FxPoint
    public fun setContentVisible(visible: Boolean)

    /** 容器坐标系下的点是否落在内容上 */
    public fun hitTest(x: Float, y: Float): Boolean

    public var touchHandler: FxContainerTouchHandler?

    /** 内容 view 宽高变化（仅在尺寸真的变了时回调） */
    public var onContentSizeChanged: ((FxSize) -> Unit)?

    /** 容器自身尺寸变化 */
    public var onBoundsChanged: (() -> Unit)?
}
