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
        demoPage("#187 尺寸变化锚点不动") {
            note(
                "复现步骤：\n" +
                    "1. 点「显示」，浮窗默认贴右下（BOTTOM_END）；\n" +
                    "2. 反复点「变大 / 变小」（也可以点卡片里的按钮）；\n" +
                    "3. 期望：右下角始终不动，尺寸只往左上方向长——2.x 会越变越往右下溢出。",
            )
            button("显示") { c.show() }
            button("隐藏") { c.hide() }
            button("变大（+40dp）") { resize(40) }
            button("变小（-40dp）") { resize(-40) }
            button("切换锚点（四角轮换）") { switchGravity() }
            note("拖到任意位置再变尺寸同样成立：拖动结束时 core 会把落点换算成最近的锚点。")
            note("有悬浮窗权限时，同一个用例在「系统级浮窗」页也成立：系统窗口改的是 WindowManager 的 x/y，尺寸变化一样按锚点重算。")
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
        DemoContent.toast(this, "锚点：${gravity.name}")
    }

    private companion object {
        const val TAG = "issue187"
    }
}
