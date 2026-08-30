package com.petterp.floatingx.demo.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxEdge
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxHalfHide
import com.petterp.floatingx.core.storage.FxSpStorage
import com.petterp.floatingx.core.update
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage
import com.petterp.floatingx.scope.fxScope

/** 布局页：锚点 / 越界 / 吸附 / 内容尺寸 / 位置持久化，全部用局部浮窗演示 */
class LayoutActivity : AppCompatActivity() {

    // 四个方向的越界开关要一起提交，所以在页面里存一份当前值
    private var overflowTop = false
    private var overflowBottom = false
    private var overflowLeft = false
    private var overflowRight = false

    private var fxOrNull: FxControl? = null

    private val fx: FxControl
        get() = fxOrNull?.takeIf { it.state != FxState.CANCELLED } ?: fxScope(TAG) {
            view { ctx -> DemoContent.resizable(ctx) }
            anchor(FxGravity.CENTER)
            enableLog("Fx-demo")
        }.also { fxOrNull = it }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("布局") {
            section("浮窗")
            button("显示") { fx.show() }
            button("隐藏") { fx.hide() }

            section("锚点")
            note("锚点 = 内容贴哪个角/边；拖动结束后 core 会把落点换算回锚点（这也是持久化存的东西）。")
            FxGravity.entries.forEach { gravity ->
                button("anchor(${gravity.name})") { fx.update { anchor(gravity) } }
            }

            section("越界")
            note("默认不允许越界：内容被约束在可用区内。打开某个方向后，那一侧可以拖出去。")
            toggle("上方越界", false) { enabled ->
                overflowTop = enabled
                applyOverflow()
            }
            toggle("下方越界", false) { enabled ->
                overflowBottom = enabled
                applyOverflow()
            }
            toggle("左侧越界", false) { enabled ->
                overflowLeft = enabled
                applyOverflow()
            }
            toggle("右侧越界", false) { enabled ->
                overflowRight = enabled
                applyOverflow()
            }

            section("吸附")
            button("四向吸附") {
                fx.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END, FxEdge.TOP, FxEdge.BOTTOM))) }
            }
            button("左右吸附 + 半隐 30%") {
                fx.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), halfHide = FxHalfHide(0.3f))) }
            }
            button("关闭吸附") { fx.update { adsorb(FxAdsorb.None) } }
            toggle("越界回弹（rebound）", true) { enabled ->
                fx.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), rebound = enabled)) }
            }

            section("内容尺寸")
            note("尺寸变化时锚点不动（#187）：先把浮窗拖到某个角，再变大/变小，贴着的那条边不会挪。")
            button("变大（+40dp）") { resize(40) }
            button("变小（-40dp）") { resize(-40) }

            section("持久化")
            toggle("persist(FxSpStorage)", false) { enabled ->
                fx.update { persist(if (enabled) FxSpStorage(this@LayoutActivity) else null) }
            }
            note("开启后每次锚点提交（拖动结束 / moveTo / update{anchor}）都会写入；读取只发生在浮窗创建时，所以要退出本页再进来才看得到恢复效果。")
            button("清除记忆") {
                FxSpStorage(this@LayoutActivity).apply {
                    clear("$TAG:1")
                    clear("$TAG:2")
                }
                DemoContent.toast(this@LayoutActivity, "已清除 $TAG 的横竖屏记忆")
            }
            note("旋转屏幕后位置按方向分别恢复：存储键是「tag:orientation」（1=竖屏、2=横屏），横竖屏各记一份。")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fxOrNull?.takeIf { it.state != FxState.CANCELLED }?.cancel()
    }

    private fun applyOverflow() {
        fx.update { overflow(top = overflowTop, bottom = overflowBottom, left = overflowLeft, right = overflowRight) }
    }

    /** 直接改内容 view 的 layoutParams，不动浮窗配置——正是 #187 的场景 */
    private fun resize(deltaDp: Int) {
        val content = fx.contentView ?: return
        DemoContent.resize(content, deltaDp)
    }

    private companion object {
        const val TAG = "layout"
    }
}
