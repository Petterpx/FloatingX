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

    /** 第二个 App 级浮窗，用来演示按 tag 管理多窗口 */
    const val TAG_APP_2 = "demo-app-2"
    const val TAG_SYSTEM = "demo-system"
    const val TAG_COMPOSE = "demo-compose"

    private val clickToast = object : FxListener {
        override fun onClick(control: FxControl, view: View) = DemoContent.toast(view.context, "点击了 ${control.tag}")

        override fun onLongClick(control: FxControl, view: View) = DemoContent.toast(view.context, "长按了 ${control.tag}")
    }

    /**
     * App 级全局浮窗：黑名单页不显示、贴边 + 半隐、位置持久化。
     *
     * [tag] 默认是 [TAG_APP]；多窗口页用 [TAG_APP_2] 再装一个（注册表按 tag 隔离，
     * 持久化的存储键也是按 tag 分的，两个窗口各记各的位置）。
     */
    @JvmOverloads
    fun installApp(app: Application, tag: String = TAG_APP): FxControl = FloatingX.install(tag) {
        val label = if (tag == TAG_APP) "App" else "App2"
        view { ctx -> DemoContent.card(ctx, label) }
        // 第二个窗口错开一点，否则默认位置完全重叠，看不出是两个
        anchor(FxGravity.CENTER_END, dy = if (tag == TAG_APP) 120f else 260f)
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

    @JvmOverloads
    fun ensureApp(app: Application, tag: String = TAG_APP): FxControl =
        FloatingX.controlOrNull(tag) ?: installApp(app, tag)

    /**
     * 系统浮窗：默认自动申请权限，被拒降级为 App 浮窗。
     *
     * [keyboard] = true 时换成带 EditText 的内容，并登记 keyboard/onBackPressed，
     * 用来验证系统窗口里唤起软键盘（窗口默认 FLAG_NOT_FOCUSABLE，触到 EditText 才临时可聚焦）。
     */
    fun installSystem(
        app: Application,
        strategy: FxPermissionStrategy = FxPermissionStrategy.auto(),
        fallback: Boolean = true,
        keyboard: Boolean = false,
    ): FxControl = FloatingX.install(TAG_SYSTEM) {
        if (keyboard) layout(R.layout.fx_input) else view { ctx -> DemoContent.card(ctx, "Sys") }
        anchor(FxGravity.TOP_START, dx = 24f, dy = 200f)
        adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END)))
        persist(FxSpStorage(app))
        enableLog("Fx-demo")
        systemHost(app) {
            theme(R.style.Theme_FloatingX)
            permission(strategy)
            // this. 消歧：这里的 fallback / keyboard 既是 Builder 方法名也是本函数的参数名
            if (fallback) this.fallback(AppHost.builder(app).theme(R.style.Theme_FloatingX).build())
            if (keyboard) {
                this.keyboard(R.id.etInput)
                // 键盘弹起期间窗口才可聚焦，也才收得到返回键；而收键盘的那次返回会被吞掉，不会走到这里
                onBackPressed {
                    DemoContent.toast(app, "系统浮窗收到返回键")
                    true
                }
            }
        }
    }.also { it.addListener(clickToast) }

    fun ensureSystem(app: Application): FxControl = FloatingX.controlOrNull(TAG_SYSTEM) ?: installSystem(app)
}
