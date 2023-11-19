package com.petterp.floatingx.app.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.addItemView
import com.petterp.floatingx.app.addTextView
import com.petterp.floatingx.app.createLinearLayoutToParent

/** @author petterp */
class BlackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            addTextView {
                text = "我是黑名单页面"
                textSize = 50f
            }
            addItemView("显示全局悬浮窗") {
                FloatingX.control(MultipleFxActivity.TAG_1).show()
            }
        }
    }
}
