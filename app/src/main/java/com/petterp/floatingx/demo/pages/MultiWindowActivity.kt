package com.petterp.floatingx.demo.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage

/**
 * 多窗口页：全局浮窗按 tag 管理（#133）。
 * 注册表是 tag → control 的映射，同 tag 再 install 会先 cancel 旧的。
 */
class MultiWindowActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("多窗口（按 tag）") {
            section("安装 / 显示")
            note("两个窗口用不同 tag，配置、位置持久化、监听都各自独立。")
            button("显示 ${DemoWindows.TAG_APP}") { DemoWindows.ensureApp(application).show() }
            button("显示 ${DemoWindows.TAG_APP_2}") { DemoWindows.ensureApp(application, DemoWindows.TAG_APP_2).show() }

            section("注册表")
            button("遍历 controls()") {
                val controls = FloatingX.controls()
                val text = if (controls.isEmpty()) {
                    "当前没有全局浮窗"
                } else {
                    controls.joinToString("\n") { "${it.tag} → ${it.state}" }
                }
                DemoContent.toast(this@MultiWindowActivity, text)
            }
            button("isInstalled(...)") {
                DemoContent.toast(
                    this@MultiWindowActivity,
                    "${DemoWindows.TAG_APP}=${FloatingX.isInstalled(DemoWindows.TAG_APP)}\n" +
                        "${DemoWindows.TAG_APP_2}=${FloatingX.isInstalled(DemoWindows.TAG_APP_2)}\n" +
                        "${DemoWindows.TAG_SYSTEM}=${FloatingX.isInstalled(DemoWindows.TAG_SYSTEM)}",
                )
            }
            note("局部浮窗（fxScope / FloatingX.create）不进注册表，controls() 里看不到。")

            section("重复 install 同 tag")
            note("同一个 tag 再 install：注册表里替换成新的，旧 control 立刻被 cancel（终态，之后任何调用都会抛异常）。")
            button("重复 install ${DemoWindows.TAG_APP}") {
                val old = DemoWindows.ensureApp(application)
                val new = DemoWindows.installApp(application)
                new.show()
                DemoContent.toast(
                    this@MultiWindowActivity,
                    "旧 control=${old.state}，新 control=${new.state}，controls().size=${FloatingX.controls().size}",
                )
            }

            section("卸载")
            button("uninstall(${DemoWindows.TAG_APP_2})") { FloatingX.uninstall(DemoWindows.TAG_APP_2) }
            button("uninstallAll()") { FloatingX.uninstallAll() }
        }
    }
}
