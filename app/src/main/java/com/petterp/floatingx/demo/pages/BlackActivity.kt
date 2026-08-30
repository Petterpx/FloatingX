package com.petterp.floatingx.demo.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.demo.ui.demoPage

/**
 * 黑名单基类：`DemoWindows.installApp` 里登记的是本类，
 * AppHost 的黑名单按 `isInstance` 匹配，所以所有子类一起命中（#221）。
 */
open class BaseBlackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("黑名单页") {
            note("本页在黑名单（按父类 BaseBlackActivity 命中，#221），浮窗不显示；返回后恢复。")
            button("返回") { finish() }
        }
    }
}

/** 真正被跳转的页面，本身没登记进黑名单，靠父类命中 */
class BlackActivity : BaseBlackActivity()
