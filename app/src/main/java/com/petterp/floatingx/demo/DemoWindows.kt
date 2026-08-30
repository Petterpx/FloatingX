package com.petterp.floatingx.demo

import android.app.Application
import android.view.View
import com.petterp.floatingx.app.AppHost
import com.petterp.floatingx.app.appHost
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxEdge
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxHalfHide
import com.petterp.floatingx.core.storage.FxSpStorage
import com.petterp.floatingx.demo.pages.BaseBlackActivity
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.system.permission.FxPermissionStrategy
import com.petterp.floatingx.system.systemHost

/** 全局浮窗集中在这里安装；页面按钮只操作 FxControl */
object DemoWindows {
    const val TAG_APP = "demo-app"
    const val TAG_SYSTEM = "demo-system"
    const val TAG_COMPOSE = "demo-compose"

    private val clickToast = object : FxListener {
        override fun onClick(control: FxControl, view: View) = DemoContent.toast(view.context, "点击了 ${control.tag}")

        override fun onLongClick(control: FxControl, view: View) = DemoContent.toast(view.context, "长按了 ${control.tag}")
    }

    /** App 级全局浮窗：黑名单页不显示、贴边 + 半隐、位置持久化 */
    fun installApp(app: Application): FxControl = FloatingX.install(TAG_APP) {
        view { ctx -> DemoContent.card(ctx, "App") }
        anchor(FxGravity.CENTER_END, dy = 120f)
        margin(top = 24f, bottom = 24f)
        adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), halfHide = FxHalfHide(0.3f)))
        persist(FxSpStorage(app))
        enableLog("Fx-demo")
        appHost(app) {
            // 传 Class 而非类名字符串：按 isInstance 匹配，子类一起命中（#221）
            blacklist(BaseBlackActivity::class.java)
            theme(R.style.Theme_FloatingX)
        }
    }.also { it.addListener(clickToast) }

    fun ensureApp(app: Application): FxControl = FloatingX.controlOrNull(TAG_APP) ?: installApp(app)

    /** 系统浮窗：默认自动申请权限，被拒降级为 App 浮窗 */
    fun installSystem(
        app: Application,
        strategy: FxPermissionStrategy = FxPermissionStrategy.auto(),
        fallback: Boolean = true,
    ): FxControl = FloatingX.install(TAG_SYSTEM) {
        view { ctx -> DemoContent.card(ctx, "Sys") }
        anchor(FxGravity.TOP_START, dx = 24f, dy = 200f)
        adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END)))
        persist(FxSpStorage(app))
        enableLog("Fx-demo")
        systemHost(app) {
            theme(R.style.Theme_FloatingX)
            permission(strategy)
            // this. 消歧：这里的 fallback 既是 Builder 方法名也是本函数的参数名
            if (fallback) this.fallback(AppHost.builder(app).theme(R.style.Theme_FloatingX).build())
        }
    }.also { it.addListener(clickToast) }

    fun ensureSystem(app: Application): FxControl = FloatingX.controlOrNull(TAG_SYSTEM) ?: installSystem(app)
}
