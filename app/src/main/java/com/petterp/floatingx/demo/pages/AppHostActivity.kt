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
        demoPage("App 级全局浮窗") {
            section("显示/隐藏")
            button("显示") { c.show() }
            button("隐藏") { c.hide() }
            button("cancel（卸载）") { c.cancel() }

            section("移动")
            button("moveTo(100,300)") { c.moveTo(100f, 300f) }
            button("moveBy(-40,0)") { c.moveBy(-40f, 0f) }
            button("moveTo 不带动画") { c.moveTo(0f, 0f, animate = false) }

            section("锚点 / 边界")
            FxGravity.entries.forEach { gravity ->
                button("anchor(${gravity.name})") { c.update { anchor(gravity) } }
            }
            button("margin 全 48") { c.update { margin(48f, 48f, 48f, 48f) } }
            button("允许上下越界") { c.update { overflow(top = true, bottom = true) } }
            toggle("safeArea（避开系统栏/刘海）", true) { enabled -> c.update { safeArea = enabled } }

            section("吸附")
            button("四向吸附") {
                c.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END, FxEdge.TOP, FxEdge.BOTTOM))) }
            }
            button("左右吸附 + 半隐 30%") {
                c.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), halfHide = FxHalfHide(0.3f))) }
            }
            button("关闭吸附") { c.update { adsorb(FxAdsorb.None) } }
            toggle("越界回弹（rebound）", true) { enabled ->
                c.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), rebound = enabled)) }
            }

            section("内容")
            button("换成 layout 内容") { c.setContent(FxContent.layout(R.layout.fx_card)) }
            button("改标题文字") { c.updateContent { it.setText(R.id.tvTitle, "Hi") } }

            section("动画")
            button("scale 动画") { c.update { animation(FxAnimations.scale()) } }
            button("fade 动画") { c.update { animation(FxAnimations.fade()) } }
            button("无动画") { c.update { animation(null) } }

            section("换页 / 过滤")
            page("进入第二页（观察 attachedActivity）", SecondActivity::class.java)
            page("进入黑名单页（浮窗消失）", BlackActivity::class.java)
            page("进入沉浸页（无状态栏）", ImmersedActivity::class.java)
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
        attachedView.text = "当前挂载：" + (c.attachedActivity?.javaClass?.simpleName ?: "未挂载")
    }
}
