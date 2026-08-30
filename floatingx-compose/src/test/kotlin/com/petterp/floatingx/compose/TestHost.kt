package com.petterp.floatingx.compose

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxRect

/** 挂在 FrameLayout 上的最小 host；parent 应已在 Activity 窗口里，Compose 才会真正组合 */
class TestHost(private val parent: FrameLayout) : FxHost {
    override val context: Context get() = parent.context
    var session: FxHostSession? = null
    override fun bind(session: FxHostSession) { this.session = session; session.onHostReady() }
    override fun createContainer(): FxContainer = FxLayerContainer(parent.context)
    override fun attach(container: FxContainer) {
        parent.addView(container.view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
    override fun detach(container: FxContainer) { parent.removeView(container.view) }
    override fun bounds(): FxBounds = FxBounds(FxRect(0f, 0f, parent.width.toFloat(), parent.height.toFloat()))
    override fun release() {}
    fun lose() = session?.onHostLost()
    fun ready() = session?.onHostReady()
}
