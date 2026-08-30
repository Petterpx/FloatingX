package com.petterp.floatingx.demo.regression

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.pages.BlackActivity
import com.petterp.floatingx.demo.pages.ComposeSecondActivity
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage

/**
 * #210 回归：2.x 的 compose 浮窗跨页就没了——内容 view 上挂的是**宿主 Activity** 的
 * ViewTree owner，换页时旧 Activity 走到 DESTROYED，组合被判死刑，ViewModel 也跟着清掉。
 *
 * 3.0 里每个浮窗自带 FxComposeOwner（spec §6），归 control 所有：
 * attach→STARTED、show→RESUMED、detach→CREATED，只有 cancel 才 DESTROYED。
 */
class Issue210Activity : AppCompatActivity() {

    private val fx: FxControl get() = DemoWindows.ensureCompose(application)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("#210 Compose 跨页存活") {
            note(
                "复现步骤：\n" +
                    "1. 点「显示 Compose 浮窗」，再点浮窗几下把 count / vm 加上去；\n" +
                    "2. 跳到第二页 / 黑名单页，或者旋转屏幕；\n" +
                    "3. 期望：浮窗一直在（黑名单页除外，那是配置要求它消失），回来后 count 与 vm 都是原值，浮窗不会白掉或崩溃。",
            )
            button("显示 Compose 浮窗") { fx.show() }
            page("跨页：进第二页再返回", ComposeSecondActivity::class.java)
            page("卸下再挂上：进黑名单页再返回", BlackActivity::class.java)
            note("黑名单页里容器被卸下（owner 降到 CREATED，组合 dispose），返回时重新组合——count 由过桥仓库带回、vm 是同一个实例。")
            button("打印当前状态") {
                DemoContent.toast(this@Issue210Activity, "${fx.tag} → ${fx.state} @ ${fx.position.x.toInt()},${fx.position.y.toInt()}")
            }
            note("对照组：cancel 之后 owner 才 destroy，此时 ViewModel 被清空，再显示就是一个全新的浮窗。")
            button("cancel（计数应归零）") { FloatingX.uninstall(DemoWindows.TAG_COMPOSE) }
        }
    }
}
