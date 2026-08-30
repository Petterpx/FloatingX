package com.petterp.floatingx.demo.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.app.attachedActivity
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage

/** 换页观察页：同一个容器被静默挪到本页，浮窗状态/位置都不重来 */
class SecondActivity : AppCompatActivity() {

    private val c: FxControl get() = DemoWindows.ensureApp(application)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("第二页") {
            note("浮窗应无动画地跟到本页：容器只是换了父 View，engine 状态、feature、动画都不重来，位置由 translation 保留。")
            button("attachedActivity") {
                DemoContent.toast(this@SecondActivity, c.attachedActivity?.javaClass?.simpleName ?: "未挂载")
            }
            button("返回") { finish() }
        }
    }
}
