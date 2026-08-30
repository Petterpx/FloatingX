package com.petterp.floatingx.app

import android.app.Activity
import android.app.Application
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.config.FxInstallScope

/** DSL：`FloatingX.install("tag") { layout(...); appHost(app) { blacklist(SplashActivity::class.java) } }` */
@JvmSynthetic
public fun FxInstallScope.appHost(application: Application, block: AppHost.Builder.() -> Unit = {}): AppHost =
    AppHost.Builder(application).apply(block).build().also { host = it }

/** 浮窗当前挂在哪个 Activity 上；host 不是 AppHost 或尚未挂载时为 null（spec §3） */
public val FxControl.attachedActivity: Activity?
    get() = (host as? AppHost)?.attachedActivity
