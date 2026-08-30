package com.petterp.floatingx.system

import android.content.Context
import com.petterp.floatingx.core.config.FxInstallScope

/** DSL：`FloatingX.install("tag") { layout(...); systemHost(app) { fallback(appHost(app)) } }` */
@JvmSynthetic
public fun FxInstallScope.systemHost(context: Context, block: SystemHost.Builder.() -> Unit = {}): SystemHost =
    SystemHost.Builder(context).apply(block).build().also { host = it }
