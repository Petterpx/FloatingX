package com.petterp.floatingx.demo

import android.app.Application
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petterp.floatingx.app.AppHost
import com.petterp.floatingx.app.appHost
import com.petterp.floatingx.compose.compose
import com.petterp.floatingx.compose.positionFlow
import com.petterp.floatingx.compose.stateFlow
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

    /** 系统窗口版的 compose 浮窗，另起一个 tag，可以和 [TAG_COMPOSE] 同时在屏幕上 */
    const val TAG_COMPOSE_SYS = "demo-compose-sys"

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

    /** 浮窗自己的 ViewModel：存活范围是 FxComposeOwner，即 install ~ cancel，跟页面无关 */
    class CounterViewModel : ViewModel() {
        var clicks = 0
    }

    /**
     * Compose 浮窗：整棵组合跑在浮窗自己的 [com.petterp.floatingx.compose.FxComposeOwner] 上——
     * `viewModel()` 落在 owner 的 ViewModelStore，`rememberSaveable` 走 FxComposeContent 的过桥仓库，
     * 所以换页、旋转、被黑名单卸下再回来都不丢状态（#210/#239）。
     *
     * [system] = true 时装成系统窗口（tag 换成 [TAG_COMPOSE_SYS]，权限被拒自动降级为 App 浮窗），
     * 内容与 App 版完全一样：compose 内容不关心 host。
     */
    @JvmOverloads
    fun installCompose(app: Application, system: Boolean = false): FxControl =
        FloatingX.install(if (system) TAG_COMPOSE_SYS else TAG_COMPOSE) {
            compose { control ->
                val vm: CounterViewModel = viewModel() // 归 FxComposeOwner 的 ViewModelStore
                var count by rememberSaveable { mutableIntStateOf(0) }
                val state by control.stateFlow().collectAsState()
                val pos by control.positionFlow().collectAsState()
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(120.dp)) {
                    Column(
                        Modifier.clickable { count++; vm.clicks++ },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("count $count", color = Color.White)
                        Text("vm ${vm.clicks}", color = Color.White, fontSize = 11.sp)
                        Text("${pos.x.toInt()},${pos.y.toInt()} $state", color = Color.White, fontSize = 9.sp)
                    }
                }
            }
            // 系统窗口版错开一点，两个一起显示时不重叠
            anchor(FxGravity.CENTER_START, dy = if (system) 60f else -100f)
            enableLog("Fx-demo")
            if (system) {
                systemHost(app) {
                    permission(FxPermissionStrategy.auto())
                    fallback(AppHost.builder(app).theme(R.style.Theme_FloatingX).build())
                }
            } else {
                appHost(app) { blacklist(BaseBlackActivity::class.java) }
            }
        }

    @JvmOverloads
    fun ensureCompose(app: Application, system: Boolean = false): FxControl =
        FloatingX.controlOrNull(if (system) TAG_COMPOSE_SYS else TAG_COMPOSE) ?: installCompose(app, system)
}
