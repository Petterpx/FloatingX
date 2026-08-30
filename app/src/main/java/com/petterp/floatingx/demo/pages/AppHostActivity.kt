package com.petterp.floatingx.demo.pages

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.app.attachedActivity
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.animation.FxAnimations
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxEdge
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxHalfHide
import com.petterp.floatingx.core.update
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.ui.demoPage

/** App 级全局浮窗的能力总览页：每个按钮对应一个公开 API */
class AppHostActivity : AppCompatActivity() {

    /** 每次取都保证浮窗存在：cancel 之后再点其它按钮会重新安装 */
    private val c: FxControl get() = DemoWindows.ensureApp(application)

    private lateinit var attachedView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attachedView = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.DKGRAY)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 4, pad, pad / 2)
        }
        demoPage(R.string.page_app_host_title) {
            section(R.string.section_show_hide)
            button(R.string.btn_show) { c.show() }
            button(R.string.btn_hide) { c.hide() }
            button(R.string.btn_cancel_uninstall) { c.cancel() }

            section(R.string.section_move)
            button(R.string.btn_move_to_100_300) { c.moveTo(100f, 300f) }
            button(R.string.btn_move_by) { c.moveBy(-40f, 0f) }
            button(R.string.btn_move_to_no_anim) { c.moveTo(0f, 0f, animate = false) }

            section(R.string.section_anchor_bounds)
            FxGravity.entries.forEach { gravity ->
                button(text(R.string.btn_anchor, gravity.name)) { c.update { anchor(gravity) } }
            }
            button(R.string.btn_margin_all_48) { c.update { margin(48f, 48f, 48f, 48f) } }
            button(R.string.btn_overflow_vertical) { c.update { overflow(top = true, bottom = true) } }
            toggle(R.string.toggle_safe_area, true) { enabled -> c.update { safeArea = enabled } }

            section(R.string.section_adsorb)
            button(R.string.btn_adsorb_all) {
                c.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END, FxEdge.TOP, FxEdge.BOTTOM))) }
            }
            button(R.string.btn_adsorb_sides_half) {
                c.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), halfHide = FxHalfHide(0.3f))) }
            }
            button(R.string.btn_adsorb_off) { c.update { adsorb(FxAdsorb.None) } }
            toggle(R.string.toggle_rebound, true) { enabled ->
                c.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), rebound = enabled)) }
            }

            section(R.string.section_content)
            button(R.string.btn_content_layout) { c.setContent(FxContent.layout(R.layout.fx_card)) }
            button(R.string.btn_content_title) { c.updateContent { it.setText(R.id.tvTitle, getString(R.string.fx_text_hi)) } }

            section(R.string.section_animation)
            button(R.string.btn_anim_scale) { c.update { animation(FxAnimations.scale()) } }
            button(R.string.btn_anim_fade) { c.update { animation(FxAnimations.fade()) } }
            button(R.string.btn_anim_none) { c.update { animation(null) } }

            section(R.string.section_navigation_filter)
            page(R.string.nav_second_observe, SecondActivity::class.java)
            page(R.string.nav_black_disappear, BlackActivity::class.java)
            page(R.string.nav_immersed_no_status_bar, ImmersedActivity::class.java)
            custom(attachedView)
        }
    }

    override fun onResume() {
        super.onResume()
        // AppHost 是在 postResumed（低版本是 resumed 之后的一个 post）里换页的，
        // 所以先刷一次，再 post 一次拿换页后的结果
        refreshAttached()
        attachedView.post(::refreshAttached)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refreshAttached()
    }

    private fun refreshAttached() {
        val name = c.attachedActivity?.javaClass?.simpleName ?: getString(R.string.label_not_attached)
        attachedView.text = getString(R.string.label_attached, name)
    }
}
