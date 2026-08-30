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
            page("Modal（拦截外部触摸 / Dialog 之上）", ModalActivity::class.java)
            page("Compose 浮窗（ViewModel / rememberSaveable / 状态流）", ComposeActivity::class.java)
            note("下面三页是 App 级浮窗的配套场景，也可以从「App 级全局浮窗」页进入。")
            page("换页观察（attachedActivity）", SecondActivity::class.java)
            page("黑名单页（浮窗消失）", BlackActivity::class.java)
            page("沉浸页（无状态栏）", ImmersedActivity::class.java)

            section("回归页")
            page("#187 尺寸变化锚点不动", Issue187Activity::class.java)
            page("#210 Compose 跨页存活", Issue210Activity::class.java)
            page("#221 黑名单命中子类", Issue221Activity::class.java)
            page("#240 越界不被裁剪", Issue240Activity::class.java)
            page("#244 Fragment 内浮窗", Issue244Activity::class.java)

            section("Java")
            note("同样的三种浮窗用 Java Builder 写一遍（JavaDemo.java），验证公开 API 的 Java 友好度。")
            button("Java：安装 App 级浮窗（tag=${JavaDemo.TAG_APP}）") {
                JavaDemo.installApp(application)
                DemoContent.toast(this@MainActivity, "已安装 ${JavaDemo.TAG_APP}")
            }
            button("Java：安装系统浮窗（tag=${JavaDemo.TAG_SYSTEM}）") {
                JavaDemo.installSystem(application)
                DemoContent.toast(this@MainActivity, "已安装 ${JavaDemo.TAG_SYSTEM}")
            }
            button("Java：本页局部浮窗（tag=${JavaDemo.TAG_SCOPE}）") {
                cancelJavaScope()
                javaScope = JavaDemo.createScope(this@MainActivity)
                DemoContent.toast(this@MainActivity, "已创建 ${JavaDemo.TAG_SCOPE}（离开本页自动销毁）")
            }
        }
    }

    override fun onDestroy() {
        cancelJavaScope()
        super.onDestroy()
    }

    /** cancel 之后的 FxControl 不可复用，再调用会抛 IllegalStateException，所以先看状态 */
    private fun cancelJavaScope() {
        javaScope?.takeIf { it.state != FxState.CANCELLED }?.cancel()
        javaScope = null
    }
}
