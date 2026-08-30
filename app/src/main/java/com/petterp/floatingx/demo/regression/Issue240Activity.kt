package com.petterp.floatingx.demo.regression

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxEdge
import com.petterp.floatingx.core.update
import com.petterp.floatingx.demo.DemoWindows
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
        demoPage("#240 越界不被裁剪") {
            note(
                "复现步骤：\n" +
                    "1. 点「显示」，再点「允许下方越界」并关掉吸附（否则松手会被吸回边上）；\n" +
                    "2. 把浮窗往屏幕最下方拖，压到导航栏区域；\n" +
                    "3. 期望：内容完整可见、不被裁掉一半，松手后也停在原地。",
            )
            button("显示 App 浮窗") { c.show() }
            button("允许下方越界（overflow(bottom = true)）") { c.update { overflow(bottom = true) } }
            button("恢复不越界（overflow()）") { c.update { overflow() } }
            button("关闭吸附（松手停在原地）") { c.update { adsorb(FxAdsorb.None) } }
            button("恢复左右吸附") { c.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END))) } }
            toggle("safeArea（避开系统栏 / 刘海）", true) { enabled -> c.update { safeArea = enabled } }
            note("safeArea 关掉后可用区扩到整个窗口，浮窗可以直接停在状态栏 / 导航栏上；overflow 则是允许再超出可用区之外。")
            note("挂载点是 DecorView：拖动范围是整个窗口，与状态栏 / 导航栏无关（挂在 android.R.id.content 上就会被内容区裁住）。")
        }
    }
}
