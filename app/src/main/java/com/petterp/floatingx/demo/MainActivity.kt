package com.petterp.floatingx.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.demo.pages.AppHostActivity
import com.petterp.floatingx.demo.pages.BlackActivity
import com.petterp.floatingx.demo.pages.ImmersedActivity
import com.petterp.floatingx.demo.pages.ScopeHostActivity
import com.petterp.floatingx.demo.pages.SecondActivity
import com.petterp.floatingx.demo.pages.SystemHostActivity
import com.petterp.floatingx.demo.regression.Issue244Activity
import com.petterp.floatingx.demo.ui.demoPage

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("FloatingX 3.0") {
            section("快捷操作")
            button("显示 App 全局浮窗") { DemoWindows.ensureApp(application).show() }
            button("显示系统浮窗（自动申请权限，拒绝则降级）") { DemoWindows.ensureSystem(application).show() }
            button("隐藏全部全局浮窗") { FloatingX.controls().forEach { it.hide() } }
            button("卸载全部全局浮窗") { FloatingX.uninstallAll() }
            section("能力页")
            page("App 级全局浮窗", AppHostActivity::class.java)
            page("系统级浮窗（权限 / 键盘 / Service）", SystemHostActivity::class.java)
            page("局部浮窗（Activity / ViewGroup / Fragment）", ScopeHostActivity::class.java)
            page("换页观察（attachedActivity）", SecondActivity::class.java)
            page("黑名单页（浮窗消失）", BlackActivity::class.java)
            page("沉浸页（无状态栏）", ImmersedActivity::class.java)
            // 后续 Task 逐个追加 page(...)
            section("回归页")
            page("#244 Fragment 内浮窗", Issue244Activity::class.java)
        }
    }
}
