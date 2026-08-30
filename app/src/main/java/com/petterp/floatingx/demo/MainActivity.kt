package com.petterp.floatingx.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.demo.java.JavaDemo
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
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage

class MainActivity : AppCompatActivity() {

    /**
     * [JavaDemo.createScope] 建出来的局部浮窗：不进注册表，生命周期归本页——
     * 重复点按钮要先 cancel 上一个（否则会一层层叠上去），离开本页也要 cancel。
     */
    private var javaScope: FxControl? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage(R.string.page_main_title) {
            section(R.string.section_quick_actions)
            button(R.string.btn_show_app_window) { DemoWindows.ensureApp(application).show() }
            button(R.string.btn_show_system_window) { DemoWindows.ensureSystem(application).show() }
            button(R.string.btn_hide_all) { FloatingX.controls().forEach { it.hide() } }
            button(R.string.btn_uninstall_all) { FloatingX.uninstallAll() }

            section(R.string.section_features)
            page(R.string.page_app_host_title, AppHostActivity::class.java)
            page(R.string.nav_system_host, SystemHostActivity::class.java)
            page(R.string.nav_scope_host, ScopeHostActivity::class.java)
            page(R.string.nav_gesture, GestureActivity::class.java)
            page(R.string.nav_layout, LayoutActivity::class.java)
            page(R.string.nav_multi_window, MultiWindowActivity::class.java)
            page(R.string.nav_modal, ModalActivity::class.java)
            page(R.string.nav_compose, ComposeActivity::class.java)
            note(R.string.note_main_extra_pages)
            page(R.string.nav_second, SecondActivity::class.java)
            page(R.string.nav_black, BlackActivity::class.java)
            page(R.string.nav_immersed, ImmersedActivity::class.java)

            section(R.string.section_regressions)
            page(R.string.page_issue187_title, Issue187Activity::class.java)
            page(R.string.page_issue210_title, Issue210Activity::class.java)
            page(R.string.page_issue221_title, Issue221Activity::class.java)
            page(R.string.page_issue240_title, Issue240Activity::class.java)
            page(R.string.page_issue244_title, Issue244Activity::class.java)

            section(R.string.section_java)
            note(R.string.note_java)
            button(text(R.string.btn_java_install_app, JavaDemo.TAG_APP)) {
                JavaDemo.installApp(application)
                DemoContent.toast(this@MainActivity, R.string.toast_installed, JavaDemo.TAG_APP)
            }
            button(text(R.string.btn_java_install_system, JavaDemo.TAG_SYSTEM)) {
                JavaDemo.installSystem(application)
                DemoContent.toast(this@MainActivity, R.string.toast_installed, JavaDemo.TAG_SYSTEM)
            }
            button(text(R.string.btn_java_scope, JavaDemo.TAG_SCOPE)) {
                cancelJavaScope()
                javaScope = JavaDemo.createScope(this@MainActivity)
                DemoContent.toast(this@MainActivity, R.string.toast_java_scope_created, JavaDemo.TAG_SCOPE)
            }
        }
    }

    override fun onDestroy() {
        cancelJavaScope()
        super.onDestroy()
    }

    /** cancel() 本身幂等（重复调用直接返回），这里看一眼状态只是不做无谓的调用；cancel 之后的 show/hide/moveTo 才会抛 IllegalStateException */
    private fun cancelJavaScope() {
        javaScope?.takeIf { it.state != FxState.CANCELLED }?.cancel()
        javaScope = null
    }
}
