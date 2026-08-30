package com.petterp.floatingx.demo.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.R
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
        demoPage(R.string.page_compose_title) {
            section(R.string.section_compose_app)
            note(R.string.note_compose_counters)
            button(R.string.btn_show_compose) { fx.show() }
            button(R.string.btn_hide) { fx.hide() }
            button(R.string.btn_cancel) {
                // cancel 是终态：owner destroy、ViewModel 清空、过桥仓库清空，
                // 再点「显示」会重新 install 一个全新的（count / vm 都归零）
                FloatingX.uninstall(DemoWindows.TAG_COMPOSE)
                DemoContent.toast(this@ComposeActivity, R.string.toast_compose_cancelled)
            }

            section(R.string.section_state_survival)
            note(R.string.note_compose_rotation)
            page(R.string.nav_compose_second, ComposeSecondActivity::class.java)
            page(R.string.nav_compose_black, BlackActivity::class.java)
            page(R.string.nav_issue210, Issue210Activity::class.java)

            section(R.string.section_compose_system)
            note(text(R.string.note_compose_system, DemoWindows.TAG_COMPOSE_SYS))
            button(R.string.btn_show_compose_system) { DemoWindows.ensureCompose(application, system = true).show() }
            button(R.string.btn_uninstall_compose_system) { FloatingX.uninstall(DemoWindows.TAG_COMPOSE_SYS) }
        }
    }
}
