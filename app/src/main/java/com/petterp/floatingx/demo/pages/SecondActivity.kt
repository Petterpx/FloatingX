package com.petterp.floatingx.demo.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.app.attachedActivity
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage

/** 换页观察页：同一个容器被静默挪到本页，浮窗状态/位置都不重来 */
class SecondActivity : AppCompatActivity() {

    private val c: FxControl get() = DemoWindows.ensureApp(application)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage(R.string.page_second_title) {
            note(R.string.note_second_page)
            button(R.string.btn_attached_activity) {
                val name = c.attachedActivity?.javaClass?.simpleName ?: getString(R.string.label_not_attached)
                DemoContent.toast(this@SecondActivity, name)
            }
            button(R.string.btn_back) { finish() }
        }
    }
}
