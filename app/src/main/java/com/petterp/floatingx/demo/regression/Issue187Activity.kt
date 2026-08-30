package com.petterp.floatingx.demo.regression

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.app.appHost
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.update
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage

/**
 * #187 回归：内容尺寸变化后浮窗位置乱跑（2.x 按左上角定位，变大就往右下溢出）。
 * 3.0 存的是「锚点」而不是左上角坐标，尺寸变化时按锚点重算，贴着的那条边不动。
 *
 * 用 App 级浮窗演示（系统浮窗要权限）：本页装一个自己的 tag，离开页面即卸载，不影响别的示例页。
 */
class Issue187Activity : AppCompatActivity() {

    /** 四个角轮着切，方便验证每个方向都不动 */
    private val gravities = listOf(FxGravity.BOTTOM_END, FxGravity.BOTTOM_START, FxGravity.TOP_START, FxGravity.TOP_END)
    private var gravityIndex = 0

    private val c: FxControl
        get() = FloatingX.controlOrNull(TAG) ?: FloatingX.install(TAG) {
            view { ctx -> DemoContent.resizable(ctx) }
            anchor(gravities[gravityIndex])
            margin(top = 24f, bottom = 24f, left = 24f, right = 24f)
            enableLog("Fx-demo")
            appHost(application) { theme(R.style.Theme_FloatingX) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage(R.string.page_issue187_title) {
            note(R.string.note_issue187_steps)
            button(R.string.btn_show) { c.show() }
            button(R.string.btn_hide) { c.hide() }
            button(R.string.btn_grow_40) { resize(40) }
            button(R.string.btn_shrink_40) { resize(-40) }
            button(R.string.btn_switch_anchor) { switchGravity() }
            note(R.string.note_issue187_drag)
            note(R.string.note_issue187_system)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 本页专用的全局浮窗，离开就卸载
        FloatingX.uninstall(TAG)
    }

    private fun resize(deltaDp: Int) {
        val content = c.contentView ?: return
        DemoContent.resize(content, deltaDp)
    }

    private fun switchGravity() {
        gravityIndex = (gravityIndex + 1) % gravities.size
        val gravity = gravities[gravityIndex]
        c.update { anchor(gravity) }
        DemoContent.toast(this, R.string.toast_anchor, gravity.name)
    }

    private companion object {
        const val TAG = "issue187"
    }
}
