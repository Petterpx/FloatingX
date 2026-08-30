package com.petterp.floatingx.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
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
            // 后续 Task 逐个追加 page(...)
        }
    }
}
