package com.petterp.floatingx.core

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.host.FxLayoutSpec
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxInsets
import com.petterp.floatingx.core.layout.FxRect

/** 把容器挂到给定 FrameLayout 上的最小 host，等价于 Plan 2 的 ViewGroupHost */
class TestHost(
    private val parent: FrameLayout,
    private val readyOnBind: Boolean = true,
    private val insets: FxInsets = FxInsets.NONE,
) : FxHost {
    override val context: Context get() = parent.context
    var session: FxHostSession? = null
    var attachCount = 0
    var detachCount = 0
    var released = false

    /** 收到的每一次布局提交，用于断言 spec 的内容与顺序 */
    val layoutSpecs = mutableListOf<FxLayoutSpec>()

    override fun bind(session: FxHostSession) {
        this.session = session
        if (readyOnBind) session.onHostReady()
    }
    override fun createContainer(): FxContainer = FxLayerContainer(parent.context)
    override fun attach(container: FxContainer) {
        parent.addView(container.view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        attachCount++
    }
    override fun detach(container: FxContainer) { parent.removeView(container.view); detachCount++ }
    override fun updateLayout(container: FxContainer, spec: FxLayoutSpec) {
        layoutSpecs += spec
        super<FxHost>.updateLayout(container, spec)
    }
    override fun bounds(): FxBounds = FxBounds(FxRect(0f, 0f, parent.width.toFloat(), parent.height.toFloat()), insets)
    override fun release() { released = true }

    fun ready() = session?.onHostReady()
    fun lose() = session?.onHostLost()
}
