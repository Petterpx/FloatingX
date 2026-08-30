package com.petterp.floatingx.core.host

import android.content.Context
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.layout.FxBounds

/**
 * 谁来承载浮窗（spec §2.1）。app / system / scope 模块各自实现。
 * 实现约定：bind() 之后一旦具备挂载条件必须回调 session.onHostReady()，
 * 失去条件回调 onHostLost()；release() 后不得再回调 session。
 */
public interface FxHost {
    /** 创建容器与内容 view 所用的 context */
    public val context: Context

    public fun bind(session: FxHostSession)

    /** 由 host 决定容器形态：Layer（覆盖层）或 Window（WindowManager 窗口） */
    public fun createContainer(): FxContainer

    public fun attach(container: FxContainer)

    public fun detach(container: FxContainer)

    /** 应用一次布局。Layer 容器默认只改内容坐标；Window 容器覆写为写 LayoutParams */
    public fun updateLayout(container: FxContainer, spec: FxLayoutSpec) {
        container.setContentPosition(spec.x, spec.y)
    }

    /** 当前可用区域与 safe area insets */
    public fun bounds(): FxBounds

    /**
     * host 自带的行为插件（如系统窗口的键盘适配、touchable→窗口 flag 映射）。
     * control 创建时与 config.features 一起挂入，换 host 时随之替换。必须每次返回同一批实例。
     */
    public fun hostFeatures(): List<FxFeature> = emptyList()

    /** control cancel 或 swap 时调用，释放监听/回调 */
    public fun release()
}
