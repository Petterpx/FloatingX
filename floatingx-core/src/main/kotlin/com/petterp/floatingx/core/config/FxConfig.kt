package com.petterp.floatingx.core.config

import com.petterp.floatingx.core.FxLogcatLogger
import com.petterp.floatingx.core.FxLogger
import com.petterp.floatingx.core.animation.FxAnimation
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.ModalScrimFeature
import com.petterp.floatingx.core.gesture.FxGesture
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxMargin
import com.petterp.floatingx.core.layout.FxOverflow
import com.petterp.floatingx.core.storage.FxStorage

/** 不可变配置（spec §2.6）。Java 用 Builder，Kotlin 用 FxConfigScope DSL */
public class FxConfig private constructor(
    public val content: FxContent,
    public val anchor: FxAnchor,
    public val margin: FxMargin,
    public val overflow: FxOverflow,
    public val safeArea: Boolean,
    public val adsorb: FxAdsorb,
    public val gesture: FxGesture,
    public val animation: FxAnimation?,
    public val storage: FxStorage?,
    public val features: List<FxFeature>,
    public val logger: FxLogger?,
) {

    public fun toBuilder(): Builder = Builder(content)
        .anchor(anchor).margin(margin).overflow(overflow).safeArea(safeArea)
        .adsorb(adsorb).gesture(gesture).animation(animation).storage(storage).logger(logger)
        .also { b -> features.forEach(b::addFeature) }

    public class Builder(private var content: FxContent) {
        private var anchor: FxAnchor = FxAnchor.DEFAULT
        private var margin: FxMargin = FxMargin.NONE
        private var overflow: FxOverflow = FxOverflow.NONE
        private var safeArea: Boolean = true
        private var adsorb: FxAdsorb = FxAdsorb.None
        private var gesture: FxGesture = FxGesture.Normal
        private var animation: FxAnimation? = null
        private var storage: FxStorage? = null
        private val features = mutableListOf<FxFeature>()
        private var logger: FxLogger? = null

        public fun content(content: FxContent): Builder = apply { this.content = content }
        public fun anchor(anchor: FxAnchor): Builder = apply { this.anchor = anchor }

        @JvmOverloads
        public fun anchor(gravity: FxGravity, dx: Float = 0f, dy: Float = 0f): Builder = anchor(FxAnchor(gravity, dx, dy))
        public fun margin(margin: FxMargin): Builder = apply { this.margin = margin }
        public fun margin(left: Float, top: Float, right: Float, bottom: Float): Builder = margin(FxMargin(left, top, right, bottom))
        public fun overflow(overflow: FxOverflow): Builder = apply { this.overflow = overflow }
        public fun safeArea(enabled: Boolean): Builder = apply { this.safeArea = enabled }
        public fun adsorb(adsorb: FxAdsorb): Builder = apply { this.adsorb = adsorb }
        public fun gesture(gesture: FxGesture): Builder = apply { this.gesture = gesture }
        public fun animation(animation: FxAnimation?): Builder = apply { this.animation = animation }
        public fun storage(storage: FxStorage?): Builder = apply { this.storage = storage }
        public fun addFeature(feature: FxFeature): Builder = apply { features += feature }

        /** 见 FxConfigScope.modal */
        @JvmOverloads
        public fun modal(enabled: Boolean = true, dismissOnOutsideTouch: Boolean = false): Builder = apply {
            features.removeAll { it is ModalScrimFeature }
            if (enabled) features += ModalScrimFeature(dismissOnOutsideTouch)
        }

        public fun logger(logger: FxLogger?): Builder = apply { this.logger = logger }

        @JvmOverloads
        public fun enableLog(tag: String = "Fx"): Builder = logger(FxLogcatLogger(tag))

        public fun build(): FxConfig = FxConfig(content, anchor, margin, overflow, safeArea, adsorb, gesture, animation, storage, features.toList(), logger)
    }

    public companion object {
        @JvmStatic public fun builder(content: FxContent): Builder = Builder(content)
    }
}
