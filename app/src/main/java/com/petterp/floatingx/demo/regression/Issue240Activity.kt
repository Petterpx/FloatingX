package com.petterp.floatingx.demo.regression

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxEdge
import com.petterp.floatingx.core.update
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.ui.demoPage

/**
 * #240 回归：浮窗拖到底部导航栏区域时被裁剪 / 位置错乱。
 * 3.0 里 AppHost 默认挂在 DecorView 上（不是 android.R.id.content），
 * 而且 Layer 容器 clipChildren=false、移动只改 translation（父层不 re-layout），所以拖到哪都不会被裁。
 */
class Issue240Activity : AppCompatActivity() {

    private val c: FxControl get() = DemoWindows.ensureApp(application)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage(R.string.page_issue240_title) {
            note(R.string.note_issue240_steps)
            button(R.string.btn_show_app) { c.show() }
            button(R.string.btn_overflow_bottom_on) { c.update { overflow(bottom = true) } }
            button(R.string.btn_overflow_reset) { c.update { overflow() } }
            button(R.string.btn_adsorb_off_stay) { c.update { adsorb(FxAdsorb.None) } }
            button(R.string.btn_adsorb_sides) { c.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END))) } }
            toggle(R.string.toggle_safe_area, true) { enabled -> c.update { safeArea = enabled } }
            note(R.string.note_issue240_safe_area)
            note(R.string.note_issue240_decor)
        }
    }
}
