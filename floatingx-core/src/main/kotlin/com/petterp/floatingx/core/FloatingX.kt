package com.petterp.floatingx.core

import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxInstallScope
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.internal.FxControlImpl
import java.util.concurrent.ConcurrentHashMap

/**
 * 全局注册表（spec §2.7）。按 tag 管理全局浮窗；局部浮窗用 create() 不注册。
 * 注册表按进程隔离，不做跨进程（#129）。
 */
public object FloatingX {

    public const val DEFAULT_TAG: String = "FloatingX"

    private val controls = ConcurrentHashMap<String, FxControlImpl>()

    /** 安装并注册；同 tag 已存在则先 cancel 旧的 */
    @JvmStatic
    public fun install(tag: String, config: FxConfig, host: FxHost): FxControl {
        controls.remove(tag)?.cancel()
        val control = FxControlImpl(tag, config, host) { c -> controls.remove(c.tag, c) }
        controls[tag] = control
        return control
    }

    @JvmSynthetic
    public fun install(tag: String = DEFAULT_TAG, block: FxInstallScope.() -> Unit): FxControl {
        val scope = FxInstallScope().apply(block)
        val host = requireNotNull(scope.host) { "install 必须指定 host（appHost / systemHost / viewGroupHost）" }
        return install(tag, scope.build(), host)
    }

    /**
     * 创建但不注册，生命周期由调用方管理（局部浮窗）。
     * tag 只用于日志与位置持久化的存储键：留空则不做持久化（多个局部浮窗会共用同一个键）。
     */
    @JvmStatic
    @JvmOverloads
    public fun create(config: FxConfig, host: FxHost, tag: String = ""): FxControl = FxControlImpl(tag, config, host)

    @JvmSynthetic
    public fun create(tag: String = "", block: FxInstallScope.() -> Unit): FxControl {
        val scope = FxInstallScope().apply(block)
        val host = requireNotNull(scope.host) { "create 必须指定 host" }
        return create(scope.build(), host, tag)
    }

    @JvmStatic
    @JvmOverloads
    public fun control(tag: String = DEFAULT_TAG): FxControl =
        controls[tag] ?: throw IllegalStateException("未安装 tag=$tag 的浮窗，请先 FloatingX.install")

    @JvmStatic
    @JvmOverloads
    public fun controlOrNull(tag: String = DEFAULT_TAG): FxControl? = controls[tag]

    /** 当前所有全局浮窗的快照（#133） */
    @JvmStatic
    public fun controls(): List<FxControl> = controls.values.toList()

    @JvmStatic
    @JvmOverloads
    public fun isInstalled(tag: String = DEFAULT_TAG): Boolean = controls.containsKey(tag)

    @JvmStatic
    @JvmOverloads
    public fun uninstall(tag: String = DEFAULT_TAG) {
        controls.remove(tag)?.cancel()
    }

    @JvmStatic
    public fun uninstallAll() {
        val all = controls.values.toList()
        controls.clear()
        all.forEach { it.cancel() }
    }
}
