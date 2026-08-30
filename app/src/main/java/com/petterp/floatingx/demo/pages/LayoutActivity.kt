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
import com.petterp.floatingx.demo.R
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
        demoPage(R.string.page_layout_title) {
            section(R.string.section_window)
            button(R.string.btn_show) { fx.show() }
            button(R.string.btn_hide) { fx.hide() }

            section(R.string.section_anchor)
            note(R.string.note_anchor)
            FxGravity.entries.forEach { gravity ->
                button(text(R.string.btn_anchor, gravity.name)) { fx.update { anchor(gravity) } }
            }

            section(R.string.section_overflow)
            note(R.string.note_overflow)
            toggle(R.string.toggle_overflow_top, false) { enabled ->
                overflowTop = enabled
                applyOverflow()
            }
            toggle(R.string.toggle_overflow_bottom, false) { enabled ->
                overflowBottom = enabled
                applyOverflow()
            }
            toggle(R.string.toggle_overflow_left, false) { enabled ->
                overflowLeft = enabled
                applyOverflow()
            }
            toggle(R.string.toggle_overflow_right, false) { enabled ->
                overflowRight = enabled
                applyOverflow()
            }

            section(R.string.section_adsorb)
            button(R.string.btn_adsorb_all) {
                fx.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END, FxEdge.TOP, FxEdge.BOTTOM))) }
            }
            button(R.string.btn_adsorb_sides_half) {
                fx.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), halfHide = FxHalfHide(0.3f))) }
            }
            button(R.string.btn_adsorb_off) { fx.update { adsorb(FxAdsorb.None) } }
            toggle(R.string.toggle_rebound, true) { enabled ->
                fx.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), rebound = enabled)) }
            }

            section(R.string.section_content_size)
            note(R.string.note_content_size)
            button(R.string.btn_grow_40) { resize(40) }
            button(R.string.btn_shrink_40) { resize(-40) }

            section(R.string.section_persist)
            toggle(R.string.toggle_persist, false) { enabled ->
                fx.update { persist(if (enabled) FxSpStorage(this@LayoutActivity) else null) }
            }
            note(R.string.note_persist)
            button(R.string.btn_clear_storage) {
                FxSpStorage(this@LayoutActivity).apply {
                    clear("$TAG:1")
                    clear("$TAG:2")
                }
                DemoContent.toast(this@LayoutActivity, R.string.toast_storage_cleared, TAG)
            }
            note(R.string.note_persist_orientation)
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
