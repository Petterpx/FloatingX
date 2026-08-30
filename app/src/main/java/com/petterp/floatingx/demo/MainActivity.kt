package com.petterp.floatingx.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.demo.pages.AppHostActivity
import com.petterp.floatingx.demo.pages.BlackActivity
import com.petterp.floatingx.demo.pages.ComposeActivity
import com.petterp.floatingx.demo.pages.GestureActivity
import com.petterp.floatingx.demo.pages.ImmersedActivity
import com.petterp.floatingx.demo.pages.LayoutActivity
import com.petterp.floatingx.demo.pages.ModalActivity
import com.petterp.floatingx.demo.pages.MultiWindowActivity
import com.petterp.floatingx.demo.pages.ScopeHostActivity
import com.petterp.floatingx.demo.pages.SecondActivity
import com.petterp.floatingx.demo.pages.SystemHostActivity
import com.petterp.floatingx.demo.regression.Issue187Activity
import com.petterp.floatingx.demo.regression.Issue210Activity
import com.petterp.floatingx.demo.regression.Issue221Activity
import com.petterp.floatingx.demo.regression.Issue240Activity
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
            page("手势（拖动模式 / 区域 / 子 view 优先级）", GestureActivity::class.java)
            page("布局（锚点 / 越界 / 吸附 / 持久化）", LayoutActivity::class.java)
            page("多窗口（按 tag 管理）", MultiWindowActivity::class.java)
            page("Compose 浮窗（ViewModel / rememberSaveable / 状态流）", ComposeActivity::class.java)
            page("Modal（拦截外部触摸 / Dialog 之上）", ModalActivity::class.java)
            page("换页观察（attachedActivity）", SecondActivity::class.java)
            page("黑名单页（浮窗消失）", BlackActivity::class.java)
            page("沉浸页（无状态栏）", ImmersedActivity::class.java)
            // 后续 Task 逐个追加 page(...)
            section("回归页")
            page("#187 尺寸变化锚点不动", Issue187Activity::class.java)
            page("#210 Compose 跨页存活", Issue210Activity::class.java)
            page("#221 黑名单命中子类", Issue221Activity::class.java)
            page("#240 越界不被裁剪", Issue240Activity::class.java)
            page("#244 Fragment 内浮窗", Issue244Activity::class.java)
        }
    }
}
