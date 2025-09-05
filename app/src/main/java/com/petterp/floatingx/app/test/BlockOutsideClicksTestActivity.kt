package com.petterp.floatingx.app.test

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.app.R
import com.petterp.floatingx.app.addItemView
import com.petterp.floatingx.app.addLinearLayout
import com.petterp.floatingx.app.addNestedScrollView
import com.petterp.floatingx.app.createLinearLayoutToParent
import com.petterp.floatingx.app.simple.FxAnimationImpl
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.util.createFx

/** 
 * Test activity for block outside clicks functionality
 * @author petterp 
 */
class BlockOutsideClicksTestActivity : AppCompatActivity() {

    private var isModalBlocking = true  // Track the current state

    // Create a floating window with block outside clicks enabled by default
    private val modalFx by createFx {
        setLayout(R.layout.item_floating)
        setBlockOutsideClicks(true)
        setGravity(FxGravity.CENTER)
        setAnimationImpl(FxAnimationImpl())
        setEnableAnimation(true)
        setEnableLog(true, "modal_fx")
        build().toControl(this@BlockOutsideClicksTestActivity)
    }

    // Create a normal floating window for comparison
    private val normalFx by createFx {
        setLayout(R.layout.item_floating)
        setGravity(FxGravity.TOP_OR_LEFT)
        setOffsetXY(50f, 100f)
        setEnableLog(true, "normal_fx")
        build().toControl(this@BlockOutsideClicksTestActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            setBackgroundColor(Color.LTGRAY)
            addNestedScrollView {
                addLinearLayout {
                    addItemView("这是一个可点击的按钮") {
                        Toast.makeText(
                            this@BlockOutsideClicksTestActivity,
                            "背景按钮被点击了！",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    addItemView("显示模态浮窗(阻挡外部点击)") {
                        modalFx.show()
                        modalFx.updateViewContent { holder ->
                            holder.setText(R.id.tvItemFx, "模态")
                        }
                    }
                    addItemView("显示普通浮窗") {
                        normalFx.show()
                        normalFx.updateViewContent { holder ->
                            holder.setText(R.id.tvItemFx, "普通")
                        }
                    }
                    addItemView("隐藏模态浮窗") {
                        modalFx.hide()
                    }
                    addItemView("隐藏普通浮窗") {
                        normalFx.hide()
                    }
                    addItemView("切换模态浮窗的阻挡模式") {
                        // Toggle the block outside clicks setting
                        isModalBlocking = !isModalBlocking
                        modalFx.configControl.setBlockOutsideClicks(isModalBlocking)
                        val message = if (isModalBlocking) {
                            "模态浮窗现在阻挡外部点击"
                        } else {
                            "模态浮窗现在允许外部点击"
                        }
                        Toast.makeText(this@BlockOutsideClicksTestActivity, message, Toast.LENGTH_SHORT).show()
                    }
                    addItemView("另一个背景可点击按钮") {
                        Toast.makeText(
                            this@BlockOutsideClicksTestActivity,
                            "第二个背景按钮被点击了！",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}