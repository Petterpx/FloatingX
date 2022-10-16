package com.petterp.floatingx.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** @author petterp */
class BlackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            addTextView {
                text = "我是黑名单页面"
                textSize = 50f
            }
        }
    }
}
