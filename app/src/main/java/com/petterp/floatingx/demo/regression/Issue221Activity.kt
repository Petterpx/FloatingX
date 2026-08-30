package com.petterp.floatingx.demo.regression

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.pages.BlackActivity
import com.petterp.floatingx.demo.ui.demoPage

/**
 * #221 回归：黑名单只能写类名字符串，父类 / 子类、按包名过滤都做不到。
 * 3.0 的 AppHost.Builder 提供三种写法，本 demo 用的是 Class 版（按 isInstance 匹配，子类一起命中）。
 */
class Issue221Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage(R.string.page_issue221_title) {
            note(R.string.note_issue221_blacklist)
            note(R.string.note_issue221_filters)
            button(R.string.btn_show_app) { DemoWindows.ensureApp(application).show() }
            page(R.string.nav_issue221_black, BlackActivity::class.java)
            note(R.string.note_issue221_expect)
        }
    }
}
