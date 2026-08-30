package com.petterp.floatingx.compose

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxRect

/** 挂在 FrameLayout 上的最小 host；parent 应已在 Activity 窗口里，Compose 才会真正组合 */
class TestHost(parent: FrameLayout, private val readyOnBind: Boolean = true) : FxHost {

    /** 可变：[moveSilently] 会换成另一个窗口里的父容器 */
    var parent: FrameLayout = parent
        private set

    override val context: Context get() = parent.context
    var session: FxHostSession? = null
    private var container: FxContainer? = null

    override fun bind(session: FxHostSession) { this.session = session; if (readyOnBind) session.onHostReady() }
    override fun createContainer(): FxContainer = FxLayerContainer(parent.context)
    override fun attach(container: FxContainer) {
        this.container = container
        parent.addView(container.view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
    override fun detach(container: FxContainer) { parent.removeView(container.view); this.container = null }
    override fun bounds(): FxBounds = FxBounds(FxRect(0f, 0f, parent.width.toFloat(), parent.height.toFloat()))
    override fun release() {}
    fun lose() = session?.onHostLost()
    fun ready() = session?.onHostReady()

    /**
     * 复刻 AppHost.moveTo 的**静默换父**：removeView + addView，不发任何 session 事件、
     * 不走 feature 的 detach/attach。换到另一个 Activity 的窗口时也一样。
     */
    fun moveSilently(newParent: FrameLayout) {
        val v = container?.view ?: return
        parent.removeView(v)
        parent = newParent
        newParent.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
}

/** 容器本身就是窗口根 view 的 host（系统浮窗形态：容器直接加到 WindowManager 上） */
class WindowTestHost(private val activity: Activity) : FxHost {
    override val context: Context get() = activity
    var session: FxHostSession? = null

    override fun bind(session: FxHostSession) { this.session = session; session.onHostReady() }
    override fun createContainer(): FxContainer = FxLayerContainer(activity)
    override fun attach(container: FxContainer) {
        val lp = WindowManager.LayoutParams(1080, 1920, WindowManager.LayoutParams.TYPE_APPLICATION, 0, android.graphics.PixelFormat.TRANSLUCENT)
        activity.windowManager.addView(container.view, lp)
    }
    override fun detach(container: FxContainer) {
        runCatching { activity.windowManager.removeViewImmediate(container.view) }
    }
    override fun bounds(): FxBounds = FxBounds(FxRect(0f, 0f, 1080f, 1920f))
    override fun release() {}
}
