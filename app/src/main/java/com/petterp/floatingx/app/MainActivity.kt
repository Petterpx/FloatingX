package com.petterp.floatingx.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.simple.FxAnimationImpl
import com.petterp.floatingx.app.test.MultipleFxActivity
import com.petterp.floatingx.app.test.SystemActivity
import com.petterp.floatingx.util.createFx

class MainActivity : AppCompatActivity() {

    private lateinit var viewGroup: FrameLayout

    private val activityFx by createFx {
        setLayout(R.layout.item_floating)
        setEnableLog(true, "activityFx")
        build().toControl(this@MainActivity)
    }

    private val viewFx by createFx {
        setLayout(R.layout.item_floating)
        setEnableScrollOutsideScreen(false)
        setAnimationImpl(FxAnimationImpl())
        setEnableLog(true, "main_fx")
        build().toControl(viewGroup)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            viewGroup = addScopeFrameViewGroup()
            addNestedScrollView {
                addLinearLayout {
                    addItemView("显示全局悬浮窗") {
                        FloatingX.control(MultipleFxActivity.TAG_1).apply {
                            updateViewContent { holder ->
                                val tv = holder.getViewOrNull<TextView>(R.id.tvItemFx)
                                tv?.setOnClickListener {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "文字被点击",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        }.show()
                    }
                    addItemView("开启半贴边"){
                        FloatingX.control(MultipleFxActivity.TAG_1).configControl.setEnableHalfHide(true)
                    }
                    addItemView("隐藏半贴边"){
                        FloatingX.control(MultipleFxActivity.TAG_1).configControl.setEnableHalfHide(false)
                    }
                    addItemView("隐藏全局悬浮窗") {
                        FloatingX.control(MultipleFxActivity.TAG_1).hide()
                    }
                    addItemView("更新当前[全局浮窗]内容-(layoutId方式)") {
                        FloatingX.control(MultipleFxActivity.TAG_1).apply {
                            updateView(R.layout.item_floating)
                            this.updateViewContent {
                                it.setText(R.id.tvItemFx, "App")
                            }
                        }.show()
                    }
                    addItemView("更新当前[全局浮窗]内容-(传递view方式)") {
                        FloatingX.control(MultipleFxActivity.TAG_1).apply {
                            updateView {
                                TextView(it).apply {
                                    layoutParams = ViewGroup.LayoutParams(50.dp, 50.dp)
                                    text = "App"
                                    gravity = Gravity.CENTER
                                    textSize = 15f
                                    setBackgroundColor(Color.GRAY)
                                    id = R.id.tvItemFx
                                }
                            }
                            show()
                        }
                    }
                    addItemView("显示一个Activity悬浮窗-(展示与多指触摸)") {
                        activityFx.show()
                        activityFx.updateViewContent {
                            it.setText(R.id.tvItemFx, "Act")
                            it.getView<CardView>(R.id.cardItemFx).setCardBackgroundColor(Color.BLUE)
                        }
                    }
                    addItemView("显示一个View级别悬浮窗-(展示与多指触摸)") {
                        viewFx.show()
                        viewFx.updateViewContent {
                            it.setText(R.id.tvItemFx, "view")
                            it.getView<CardView>(R.id.cardItemFx)
                                .setCardBackgroundColor(Color.GREEN)
                        }
                    }
                    addItemView("进入测试页面") {
                        TestActivity::class.java.start(this@MainActivity)
                    }
                    addItemView("进入system浮窗测试页面") {
                        SystemActivity::class.java.start(this@MainActivity)
                    }
                }
            }
        }
    }

    private fun ViewGroup.addScopeFrameViewGroup(): FrameLayout {
        val viewGroup = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                300,
            ).apply {
                leftMargin = 50
                topMargin = 50
                rightMargin = 50
                bottomMargin = 50
            }
            setBackgroundColor(Color.YELLOW)
        }
        addView(viewGroup)
        return viewGroup
    }
}