package com.petterp.floatingx.core.config

import android.content.Context
import android.view.View
import androidx.annotation.LayoutRes
import com.petterp.floatingx.core.animation.FxAnimation
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.ModalScrimFeature
import com.petterp.floatingx.core.gesture.FxChildPriority
import com.petterp.floatingx.core.gesture.FxDrag
import com.petterp.floatingx.core.gesture.FxGesture
import com.petterp.floatingx.core.gesture.FxRegion
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxMargin
import com.petterp.floatingx.core.layout.FxOverflow
import com.petterp.floatingx.core.storage.FxStorage

@DslMarker
public annotation class FxDsl

/**
 * Kotlin DSL。`base` 非空时（control.update {}）未显式设置的项沿用旧值。
 * build() 是 public 因为被 inline 的 FxControl.update 调用。
 */
@FxDsl
public open class FxConfigScope(base: FxConfig?) {
    private var content: FxContent? = base?.content
    private var anchor: FxAnchor = base?.anchor ?: FxAnchor.DEFAULT
    private var margin: FxMargin = base?.margin ?: FxMargin.NONE
    private var overflow: FxOverflow = base?.overflow ?: FxOverflow.NONE
    private var adsorb: FxAdsorb = base?.adsorb ?: FxAdsorb.None
    private var gesture: FxGesture = base?.gesture ?: FxGesture.Normal
    private var animation: FxAnimation? = base?.animation
    private var storage: FxStorage? = base?.storage
    private val features: MutableList<FxFeature> = base?.features?.toMutableList() ?: mutableListOf()
    private var logTag: String? = null
    private val baseLogger = base?.logger

    public var safeArea: Boolean = base?.safeArea ?: true

    public fun layout(@LayoutRes id: Int) {
        content = FxContent.layout(id)
    }

    public fun view(view: View) {
        content = FxContent.view(view)
    }

    public fun view(provider: (Context) -> View) {
        content = FxContent.provider(provider)
    }

    public fun content(content: FxContent) {
        this.content = content
    }

    public fun anchor(gravity: FxGravity, dx: Float = 0f, dy: Float = 0f) {
        anchor = FxAnchor(gravity, dx, dy)
    }

    public fun margin(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f) {
        margin = FxMargin(left, top, right, bottom)
    }

    public fun overflow(top: Boolean = false, bottom: Boolean = false, left: Boolean = false, right: Boolean = false) {
        overflow = FxOverflow(top, bottom, left, right)
    }

    public fun adsorb(adsorb: FxAdsorb) {
        this.adsorb = adsorb
    }

    public fun gesture(gesture: FxGesture) {
        this.gesture = gesture
    }

    public fun gesture(block: FxGestureScope.() -> Unit) {
        gesture = FxGestureScope(gesture).apply(block).build()
    }

    public fun animation(animation: FxAnimation?) {
        this.animation = animation
    }

    public fun persist(storage: FxStorage?) {
        this.storage = storage
    }

    public fun addFeature(feature: FxFeature) {
        features += feature
    }

    /**
     * 拦截内容之外的触摸（#212），dismissOnOutsideTouch=true 时点击外部自动 hide（#151）。
     * 只对 Layer 容器（app / scope）生效；重复调用只保留最后一次。
     */
    public fun modal(enabled: Boolean = true, dismissOnOutsideTouch: Boolean = false) {
        features.removeAll { it is ModalScrimFeature }
        if (enabled) features += ModalScrimFeature(dismissOnOutsideTouch)
    }

    public fun enableLog(tag: String = "Fx") {
        logTag = tag
    }

    public fun build(): FxConfig {
        val c = requireNotNull(content) { "必须通过 layout()/view()/content() 指定浮窗内容" }
        val builder = FxConfig.builder(c)
            .anchor(anchor).margin(margin).overflow(overflow).safeArea(safeArea)
            .adsorb(adsorb).gesture(gesture).animation(animation).storage(storage)
        features.forEach(builder::addFeature)
        val tag = logTag
        if (tag != null) builder.enableLog(tag) else builder.logger(baseLogger)
        return builder.build()
    }
}

/** FloatingX.install / create 用：额外指定 host */
@FxDsl
public class FxInstallScope : FxConfigScope(null) {
    public var host: FxHost? = null
}

@FxDsl
public class FxGestureScope internal constructor(base: FxGesture) {
    public var click: Boolean = base.click
    public var longPress: Boolean = base.longPress
    public var drag: FxDrag = base.drag
    public var dragRegion: FxRegion? = base.dragRegion
    public var childPriority: FxChildPriority = base.childPriority
    public var touchable: Boolean = base.touchable
    public var longPressTimeout: Long = base.longPressTimeout

    internal fun build(): FxGesture = FxGesture(click, longPress, drag, dragRegion, childPriority, touchable, longPressTimeout)
}
