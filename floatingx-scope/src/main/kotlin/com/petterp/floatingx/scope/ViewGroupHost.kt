package com.petterp.floatingx.scope

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxRect

/**
 * 把浮窗挂到任意 ViewGroup 上的 host（spec §5）。
 * - bind 后立即 ready：父容器已存在，尺寸为 0 时 core 不会定位，等容器 onSizeChanged 再算。
 * - viewGroup 从 window 卸下即 lost（Activity 销毁、RecyclerView 回收…），重新挂上再 ready。
 * - 不进 FloatingX 注册表，生命周期归调用方：不再需要时调用 control.cancel()。
 * - viewGroup 应是 FrameLayout 一类可叠放子 view 的容器（FrameLayout / ConstraintLayout / CoordinatorLayout / android.R.id.content）。
 */
public class ViewGroupHost(public val viewGroup: ViewGroup) : FxHost {

    override val context: Context get() = viewGroup.context

    private var session: FxHostSession? = null

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) { session?.onHostReady() }
        override fun onViewDetachedFromWindow(v: View) { session?.onHostLost() }
    }

    override fun bind(session: FxHostSession) {
        this.session = session
        viewGroup.addOnAttachStateChangeListener(attachListener)
        session.onHostReady()
    }

    override fun createContainer(): FxContainer = FxLayerContainer(context)

    override fun attach(container: FxContainer) {
        viewGroup.addView(container.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    override fun detach(container: FxContainer) {
        viewGroup.removeView(container.view)
    }

    override fun bounds(): FxBounds = FxBounds(FxRect(0f, 0f, viewGroup.width.toFloat(), viewGroup.height.toFloat()))

    override fun release() {
        viewGroup.removeOnAttachStateChangeListener(attachListener)
        session = null
    }

    public companion object {
        /** Java 入口：FloatingX.create(config, ViewGroupHost.of(viewGroup)) */
        @JvmStatic
        public fun of(viewGroup: ViewGroup): ViewGroupHost = ViewGroupHost(viewGroup)
    }
}
