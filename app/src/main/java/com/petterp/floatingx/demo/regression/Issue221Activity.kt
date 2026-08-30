package com.petterp.floatingx.demo.regression

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.pages.BlackActivity
import com.petterp.floatingx.demo.ui.demoPage

/**
 * #221 回归：黑名单只能写类名字符串，父类 / 子类、按包名过滤都做不到。
 * 3.0 的 AppHost.Builder 提供三种写法，本 demo 用的是 Class 版（按 isInstance 匹配，子类一起命中）。
 */
class Issue221Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("#221 黑名单命中子类") {
            note(
                "DemoWindows.installApp 里登记的是父类：\n" +
                    "    blacklist(BaseBlackActivity::class.java)\n" +
                    "BlackActivity 自己没登记，只是继承了 BaseBlackActivity——按 isInstance 匹配，一样被拦下。",
            )
            note(
                "AppHost.Builder 的三种过滤写法：\n" +
                    "· blacklist(Class...) / whitelist(Class...)：含子类；\n" +
                    "· blacklist(String...) / whitelist(String...)：类全名精确匹配；\n" +
                    "· filter { activity -> ... }：任意规则（按包名、按 Intent 参数……），可多次调用，全部通过才显示。",
            )
            button("显示 App 浮窗") { DemoWindows.ensureApp(application).show() }
            page("跳到 BlackActivity（子类，浮窗应消失）", BlackActivity::class.java)
            note("期望：进入 BlackActivity 后浮窗消失（容器被卸下），返回本页自动恢复。")
        }
    }
}
