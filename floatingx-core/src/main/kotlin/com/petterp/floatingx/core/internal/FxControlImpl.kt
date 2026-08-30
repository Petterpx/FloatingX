package com.petterp.floatingx.core.internal

import android.os.Looper
import android.view.View
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.FxLogger
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxViewHolder
import com.petterp.floatingx.core.engine.FxCommand
import com.petterp.floatingx.core.engine.FxEngine
import com.petterp.floatingx.core.engine.FxEngineDelegate
import com.petterp.floatingx.core.feature.AnimationFeature
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope
import com.petterp.floatingx.core.feature.GestureFeature
import com.petterp.floatingx.core.feature.LocationFeature
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxLayoutInput
import com.petterp.floatingx.core.layout.FxPoint
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 把 engine / host / container / features / listeners 装配在一起。
 * 构造顺序很重要：内容 view 先创建，host.bind() 最后调用（host 可能同步 onHostReady）。
 */
internal class FxControlImpl(
    override val tag: String,
    initialConfig: FxConfig,
    initialHost: FxHost,
    private val onCancelled: ((FxControlImpl) -> Unit)? = null,
) : FxControl, FxEngineDelegate, FxFeatureScope {

    override var config: FxConfig = initialConfig
        private set
    override var host: FxHost = initialHost
        private set
    override var container: FxContainer = initialHost.createContainer()
        private set
    override var contentView: View? = null
        private set
    override var holder: FxViewHolder? = null
        private set
    override var anchor: FxAnchor = initialConfig.anchor
        private set
    override val control: FxControl get() = this
    override val logger: FxLogger? get() = config.logger

    private val engine = FxEngine(this)
    private val listeners = CopyOnWriteArrayList<FxListener>()
    private val location = LocationFeature()
    private val gesture = GestureFeature(location)
    private val animation = AnimationFeature()
    /** 用 COW 列表：六处 delegate 回调都在遍历它，而 feature 的回调里可能再 add/removeFeature */
    private val features = CopyOnWriteArrayList<FxFeature>(listOf(location, gesture, animation))
    private var featuresAttached = false

    /** cancel 重入闩：engine.cancel() 派发 onDetach 时 state 尚未变成 CANCELLED */
    private var cancelling = false
    private var lastOrientation = initialHost.context.resources.configuration.orientation
    private val mainLooper = Looper.getMainLooper()

    init {
        features += initialConfig.features
        createContent()
        loadStoredAnchor()?.let { anchor = it }
        host.bind(engine)
    }

    // ---------- FxControl ----------

    override val state: FxState get() = engine.state
    override val isShowing: Boolean get() = engine.state == FxState.SHOWN
    override val position: FxPoint get() = container.contentPositionOnScreen()

    override fun show() { main(); engine.show() }

    override fun hide() { main(); engine.hide() }

    override fun cancel() {
        main()
        if (cancelling || engine.state == FxState.CANCELLED) return
        cancelling = true
        engine.cancel()
        host.release()
        dispatch { it.onCancel(this) }
        listeners.clear()
        onCancelled?.invoke(this)
    }

    override fun moveTo(x: Float, y: Float) = moveTo(x, y, animate = true)
    override fun moveTo(x: Float, y: Float, animate: Boolean) { main(); engine.dispatch(FxCommand.MoveTo(x, y, animate)) }
    override fun moveBy(dx: Float, dy: Float) = moveBy(dx, dy, animate = true)
    override fun moveBy(dx: Float, dy: Float, animate: Boolean) { main(); engine.dispatch(FxCommand.MoveBy(dx, dy, animate)) }

    override fun update(config: FxConfig) {
        main()
        val old = this.config
        this.config = config
        // update { anchor } 是 spec §2.3 的三个持久化写入点之一：要提交而不是裸赋值。
        // 必须先于 onConfigChanged 广播，好让 relayout 看到新锚点。
        if (old.anchor != config.anchor) commitAnchor(config.anchor)
        if (old.content !== config.content) createContent()
        val removed = old.features - config.features.toSet()
        val added = config.features - old.features.toSet()
        removed.forEach { removeFeature(it) }
        added.forEach { addFeature(it) }
        if (featuresAttached) features.forEach { it.onConfigChanged(old, config) }
    }

    override fun updateContent(block: (FxViewHolder) -> Unit) { main(); holder?.let(block) }

    override fun setContent(content: FxContent) {
        main()
        update(config.toBuilder().content(content).build())
    }

    override fun addListener(listener: FxListener) { listeners.addIfAbsent(listener) }
    override fun removeListener(listener: FxListener) { listeners.remove(listener) }

    override fun addFeature(feature: FxFeature) {
        main()
        if (feature in features) return
        features += feature
        if (featuresAttached) feature.onAttach(this)
    }

    override fun removeFeature(feature: FxFeature) {
        main()
        if (features.remove(feature) && featuresAttached) feature.onDetach()
    }

    // ---------- FxEngineDelegate ----------

    override fun performAttach() {
        container.onContentSizeChanged = { size -> if (featuresAttached) features.forEach { it.onContentSizeChanged(size) } }
        container.onBoundsChanged = { engine.onBoundsChanged() }
        host.attach(container)
        container.setContentVisible(false)
        featuresAttached = true
        features.forEach { it.onAttach(this) }
        dispatch { it.onAttach(this) }
    }

    override fun performDetach() {
        features.forEach { it.onDetach() }
        featuresAttached = false
        container.onContentSizeChanged = null
        container.onBoundsChanged = null
        host.detach(container)
        dispatch { it.onDetach(this) }
    }

    override fun performShow() {
        container.setContentVisible(true)
        features.forEach { it.onShow() }
        dispatch { it.onShow(this) }
    }

    override fun performHide() {
        features.forEach { it.onHide() }
        animation.playHide { container.setContentVisible(false) }
        dispatch { it.onHide(this) }
    }

    override fun perform(command: FxCommand) {
        when (command) {
            is FxCommand.MoveTo -> location.moveTo(command.x, command.y, command.animate)
            is FxCommand.MoveBy -> location.moveBy(command.dx, command.dy, command.animate)
        }
    }

    override fun onBoundsChanged() {
        val orientation = host.context.resources.configuration.orientation
        if (orientation != lastOrientation) {
            lastOrientation = orientation
            loadStoredAnchor()?.let { anchor = it }
        }
        features.forEach { it.onBoundsChanged() }
    }

    override fun swapHost(fallback: FxHost) {
        logger?.d { "[$tag] 切换 host: ${host::class.java.simpleName} -> ${fallback::class.java.simpleName}" }
        host.release()
        host = fallback
        container = fallback.createContainer()
        contentView?.let { container.setContent(it) }
        fallback.bind(engine)
    }

    override fun onStateChanged(old: FxState, new: FxState) {
        logger?.d { "[$tag] $old -> $new" }
    }

    // ---------- FxFeatureScope ----------

    override fun layoutInput(): FxLayoutInput? {
        val size = container.contentSize()
        if (!size.isValid) return null
        return FxLayoutInput(host.bounds(), size, container.isLtr, config.margin, config.overflow, config.safeArea)
    }

    override fun commitAnchor(anchor: FxAnchor) {
        this.anchor = anchor
        config.storage?.save(storageKey(), anchor)
        dispatch { it.onPositionChanged(this, anchor) }
    }

    override fun dispatch(block: (FxListener) -> Unit) {
        for (l in listeners) block(l)
    }

    override fun requestRelayout() = location.relayout()

    // ---------- internal ----------

    private fun createContent() {
        val view = config.content.create(host.context, container.view)
        container.setContent(view)
        contentView = view
        holder = FxViewHolder(view)
        // 新 view 默认是 VISIBLE，换内容时要沿用当前可见性，否则隐藏中的浮窗会自己冒出来
        container.setContentVisible(engine.state == FxState.SHOWN)
    }

    private fun storageKey(): String = "$tag:${host.context.resources.configuration.orientation}"

    private fun loadStoredAnchor(): FxAnchor? = config.storage?.load(storageKey())

    private fun main() {
        check(Looper.myLooper() == mainLooper) { "FloatingX[$tag] 的 API 必须在主线程调用" }
    }
}
