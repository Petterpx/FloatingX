package com.petterp.floatingx.demo.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.update
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.ui.demoPage

/** 沉浸页：edge-to-edge + 隐藏状态栏，用来验证 safeArea 对刘海/系统栏的影响 */
class ImmersedActivity : AppCompatActivity() {

    private val c: FxControl get() = DemoWindows.ensureApp(application)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 内容铺满窗口（含状态栏/导航栏区域），再把状态栏藏掉
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.statusBars())
        demoPage(R.string.page_immersed_title) {
            note(R.string.note_immersed)
            toggle(R.string.toggle_safe_area_short, true) { enabled -> c.update { safeArea = enabled } }
            button(R.string.btn_back) { finish() }
        }
    }
}
