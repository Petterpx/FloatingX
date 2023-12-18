package com.petterp.floatingx.app.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.addItemView
import com.petterp.floatingx.app.addLinearLayout
import com.petterp.floatingx.app.addNestedScrollView
import com.petterp.floatingx.app.createLinearLayoutToParent

/**
 *
 * @author petterp
 */
class SystemActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            addNestedScrollView {
                addLinearLayout {
                    addItemView("移动到(0,0)") {
                        FloatingX.controlOrNull(MultipleFxActivity.TAG_1)?.move(0f, 0f)
                    }
                    addItemView("移动到(-100,0)") {
                        FloatingX.controlOrNull(MultipleFxActivity.TAG_1)?.move(-100f, 0f)
                    }
                    addItemView("移动到(500,500)") {
                        FloatingX.controlOrNull(MultipleFxActivity.TAG_1)?.move(500f, 500f)
                    }
                }
            }
        }
    }
}
