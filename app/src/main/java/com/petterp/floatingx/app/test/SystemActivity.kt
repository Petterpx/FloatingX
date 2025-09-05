package com.petterp.floatingx.app.test

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.MainActivity
import com.petterp.floatingx.app.R
import com.petterp.floatingx.app.addItemView
import com.petterp.floatingx.app.addLinearLayout
import com.petterp.floatingx.app.addNestedScrollView
import com.petterp.floatingx.app.createLinearLayoutToParent
import com.petterp.floatingx.app.dp
import com.petterp.floatingx.app.start

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
                    addItemView("移动到fx2"){
                        MainActivity::class.java.start(this@SystemActivity)
                    }
                    addItemView("移动到(0,0)") {
                        FloatingX.controlOrNull(MultipleFxActivity.TAG_1)?.move(0f, 0f)
                    }
                    addItemView("移动到(-100,0)") {
                        FloatingX.controlOrNull(MultipleFxActivity.TAG_1)?.move(-100f, 0f)
                    }
                    addItemView("移动到(300,0)") {
                        FloatingX.controlOrNull(MultipleFxActivity.TAG_1)?.move(300f, 0f)
                    }
                    addItemView("移动到(500,500)") {
                        FloatingX.controlOrNull(MultipleFxActivity.TAG_1)?.move(500f, 500f)
                    }
                    addItemView("updateView()") {
                        FloatingX.controlOrNull(MultipleFxActivity.TAG_1)?.updateView {
                            TextView(it).apply {
                                layoutParams = ViewGroup.LayoutParams(100.dp, 200.dp)
                                text = "App"
                                gravity = Gravity.CENTER
                                textSize = 15f
                                setBackgroundColor(Color.GRAY)
                            }
                        }
                    }
                    addItemView("updateView2()") {
                        FloatingX.controlOrNull(MultipleFxActivity.TAG_1)
                            ?.updateView(R.layout.item_floating_new)
                    }
                    
                    // Test the new getWindowManagerLayoutParams() functionality
                    addItemView("测试 WindowManager.LayoutParams 获取") {
                        val control = FloatingX.controlOrNull(MultipleFxActivity.TAG_1)
                        if (control != null) {
                            val layoutParams = control.getWindowManagerLayoutParams()
                            if (layoutParams != null) {
                                android.util.Log.i("SystemActivity", "系统悬浮窗 - 成功获取 WindowManager.LayoutParams")
                                android.util.Log.d("SystemActivity", "当前 flags: ${layoutParams.flags}")
                                android.util.Log.d("SystemActivity", "当前 type: ${layoutParams.type}")
                            } else {
                                android.util.Log.i("SystemActivity", "非系统悬浮窗 - WindowManager.LayoutParams 不可用")
                            }
                        }
                    }
                    
                    addItemView("请求悬浮窗焦点 (演示用例)") {
                        val control = FloatingX.controlOrNull(MultipleFxActivity.TAG_1)
                        if (control != null) {
                            com.petterp.floatingx.app.kotlin.TestWindowManagerLayoutParams
                                .requestFocusFloatingView(control, this@SystemActivity)
                        }
                    }
                    
                    addItemView("移除悬浮窗焦点 (演示用例)") {
                        val control = FloatingX.controlOrNull(MultipleFxActivity.TAG_1)
                        if (control != null) {
                            com.petterp.floatingx.app.kotlin.TestWindowManagerLayoutParams
                                .loseFocusFloatingView(control, this@SystemActivity)
                        }
                    }
                }
            }
        }
    }
}
