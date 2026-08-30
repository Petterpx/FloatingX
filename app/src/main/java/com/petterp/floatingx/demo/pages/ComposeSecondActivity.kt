package com.petterp.floatingx.demo.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.ui.demoPage

/** Compose 浮窗的换页观察页：换页只是把容器挪到本页，组合状态不重来 */
class ComposeSecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage(R.string.page_compose_second_title) {
            note(R.string.note_compose_second_1)
            note(R.string.note_compose_second_2)
            button(R.string.btn_back) { finish() }
        }
    }
}
