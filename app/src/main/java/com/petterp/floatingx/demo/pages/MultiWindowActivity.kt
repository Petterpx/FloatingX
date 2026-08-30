package com.petterp.floatingx.demo.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage

/**
 * 多窗口页：全局浮窗按 tag 管理（#133）。
 * 注册表是 tag → control 的映射，同 tag 再 install 会先 cancel 旧的。
 */
class MultiWindowActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage(R.string.page_multi_window_title) {
            section(R.string.section_install_show)
            note(R.string.note_multi_window)
            button(text(R.string.btn_show_tag, DemoWindows.TAG_APP)) { DemoWindows.ensureApp(application).show() }
            button(text(R.string.btn_show_tag, DemoWindows.TAG_APP_2)) { DemoWindows.ensureApp(application, DemoWindows.TAG_APP_2).show() }

            section(R.string.section_registry)
            button(R.string.btn_iterate_controls) {
                val controls = FloatingX.controls()
                val text = if (controls.isEmpty()) {
                    getString(R.string.toast_no_global_windows)
                } else {
                    controls.joinToString("\n") { "${it.tag} → ${it.state}" }
                }
                DemoContent.toast(this@MultiWindowActivity, text)
            }
            button(R.string.btn_is_installed) {
                DemoContent.toast(
                    this@MultiWindowActivity,
                    "${DemoWindows.TAG_APP}=${FloatingX.isInstalled(DemoWindows.TAG_APP)}\n" +
                        "${DemoWindows.TAG_APP_2}=${FloatingX.isInstalled(DemoWindows.TAG_APP_2)}\n" +
                        "${DemoWindows.TAG_SYSTEM}=${FloatingX.isInstalled(DemoWindows.TAG_SYSTEM)}",
                )
            }
            note(R.string.note_scope_not_in_registry)

            section(R.string.section_reinstall)
            note(R.string.note_reinstall)
            button(text(R.string.btn_reinstall_tag, DemoWindows.TAG_APP)) {
                val old = DemoWindows.ensureApp(application)
                val new = DemoWindows.installApp(application)
                new.show()
                DemoContent.toast(
                    this@MultiWindowActivity,
                    getString(R.string.toast_reinstall_result, old.state, new.state, FloatingX.controls().size),
                )
            }

            section(R.string.section_uninstall)
            button(text(R.string.btn_uninstall_tag, DemoWindows.TAG_APP_2)) { FloatingX.uninstall(DemoWindows.TAG_APP_2) }
            button(R.string.btn_uninstall_all_api) { FloatingX.uninstallAll() }
        }
    }
}
