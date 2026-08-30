package com.petterp.floatingx.demo.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.regression.Issue210Activity
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage

/**
 * Compose 浮窗页：内容是 `compose { control -> … }`，跑在浮窗自己的 FxComposeOwner 上。
 *
 * 卡片里三行分别演示三种状态来源：
 * - `count`：`rememberSaveable`，组合被 dispose（容器 detach）时存进 FxComposeContent 的过桥仓库；
 * - `vm`：`viewModel()`，实例存在 owner 的 ViewModelStore 里，只有 cancel 才清；
 * - 坐标 + 状态：`positionFlow()` / `stateFlow()`，core 的回调桥成 StateFlow。
 */
class ComposeActivity : AppCompatActivity() {

    private val fx: FxControl get() = DemoWindows.ensureCompose(application)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("Compose 浮窗") {
            section("App 级 compose 浮窗")
            note("点浮窗计数：count 走 rememberSaveable，vm 走 viewModel()，第三行是 positionFlow() 的实时坐标与 stateFlow() 的状态。")
            button("显示 Compose 浮窗") { fx.show() }
            button("隐藏") { fx.hide() }
            button("cancel") {
                // cancel 是终态：owner destroy、ViewModel 清空、过桥仓库清空，
                // 再点「显示」会重新 install 一个全新的（count / vm 都归零）
                FloatingX.uninstall(DemoWindows.TAG_COMPOSE)
                DemoContent.toast(this@ComposeActivity, "已 cancel，再点显示会重建（计数归零）")
            }

            section("状态存活")
            note("旋转后 count 保持（rememberSaveable 走 FxComposeOwner）")
            page("进入第二页：count 与 vm 应保持", ComposeSecondActivity::class.java)
            page("进入黑名单页：浮窗消失，返回后 count 保持", BlackActivity::class.java)
            page("#210 回归页", Issue210Activity::class.java)

            section("系统窗口版")
            note("同一份 compose 内容装进系统窗口（tag=${DemoWindows.TAG_COMPOSE_SYS}）：自动申请权限，被拒降级为 App 浮窗。可与上面的 App 版同时显示。")
            button("显示系统窗口版 compose") { DemoWindows.ensureCompose(application, system = true).show() }
            button("卸载系统窗口版") { FloatingX.uninstall(DemoWindows.TAG_COMPOSE_SYS) }
        }
    }
}
