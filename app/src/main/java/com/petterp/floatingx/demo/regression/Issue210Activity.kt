package com.petterp.floatingx.demo.regression

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.pages.BlackActivity
import com.petterp.floatingx.demo.pages.ComposeSecondActivity
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage

/**
 * #210 回归：2.x 的 compose 浮窗跨页就没了——内容 view 上挂的是**宿主 Activity** 的
 * ViewTree owner，换页时旧 Activity 走到 DESTROYED，组合被判死刑，ViewModel 也跟着清掉。
 *
 * 3.0 里每个浮窗自带 FxComposeOwner（spec §6），归 control 所有：
 * attach→STARTED、show→RESUMED、detach→CREATED，只有 cancel 才 DESTROYED。
 */
class Issue210Activity : AppCompatActivity() {

    private val fx: FxControl get() = DemoWindows.ensureCompose(application)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage(R.string.page_issue210_title) {
            note(R.string.note_issue210_steps)
            button(R.string.btn_show_compose) { fx.show() }
            page(R.string.nav_issue210_second, ComposeSecondActivity::class.java)
            page(R.string.nav_issue210_black, BlackActivity::class.java)
            note(R.string.note_issue210_black)
            button(R.string.btn_print_state) {
                DemoContent.toast(this@Issue210Activity, "${fx.tag} → ${fx.state} @ ${fx.position.x.toInt()},${fx.position.y.toInt()}")
            }
            note(R.string.note_issue210_cancel)
            button(R.string.btn_cancel_reset_count) { FloatingX.uninstall(DemoWindows.TAG_COMPOSE) }
        }
    }
}
