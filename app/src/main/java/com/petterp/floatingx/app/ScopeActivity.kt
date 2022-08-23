package com.petterp.floatingx.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.simple.FxAnimationImpl
import com.petterp.floatingx.util.createFx

/** @author petterp */
class ScopeActivity : AppCompatActivity() {

    private lateinit var viewGroup: ViewGroup

    private val scopeFx by createFx {
        setLayout(R.layout.item_floating)
        setEnableScrollOutsideScreen(false)
        setEnableEdgeAdsorption(false)
        setEdgeOffset(40f)
        setAnimationImpl(FxAnimationImpl())
        setEnableAnimation(false)
        setEnableLog(true)
        build().toControl(viewGroup)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            viewGroup = addScopeViewGroup()
            addTextView {
                text = "api列表可拖动"
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 20)
                textSize = 19f
            }
            addNestedScrollView {
                addLinearLayout {
                    addItemView("显示悬浮窗") {
                        scopeFx.show()
                    }
                    addItemView("禁止触摸事件(禁止拖动)") {
                        scopeFx.helperControl.setEnableTouch(false)
                    }
                    addItemView("允许触摸事件(允许拖动)-默认允许") {
                        scopeFx.helperControl.setEnableTouch(true)
                    }
                    addItemView("隐藏悬浮窗") {
                        scopeFx.hide()
                    }
                    addItemView("更换layout(通过布局更换)") {
                        scopeFx.updateManagerView(R.layout.item_floating_new)
                    }
                    addItemView("更换layoutView(通过传递View)") {
                        scopeFx.updateManagerView {
                            TextView(it).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                text = "scope"
                                textSize = 15f
                                setBackgroundColor(Color.GRAY)
                                setPadding(10, 10, 10, 10)
                            }
                        }
                    }
                    addItemView("增加点击事件") {
                        scopeFx.setClickListener {
                            Toast.makeText(
                                this@ScopeActivity,
                                "被点击",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                    addItemView("关闭点击事件响应") {
                        scopeFx.helperControl.setEnableClick(false)
                    }
                    addItemView("打开点击事件响应") {
                        scopeFx.helperControl.setEnableClick(true)
                    }
                    addItemView("当前是否显示") {
                        Toast.makeText(
                            this@ScopeActivity,
                            "当前是否显示-${scopeFx.isShow()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    addItemView("允许边缘吸附,立即生效") {
                        scopeFx.helperControl.setEnableEdgeAdsorption(true)
                    }
                    addItemView("允许边缘回弹") {
                        scopeFx.helperControl.setEnableEdgeRebound(true)
                    }
                    addItemView("开启动画") {
                        scopeFx.helperControl.setEnableAnimation(true)
                    }
                    addItemView("边距调整为100f") {
                        scopeFx.helperControl.setBorderMargin(100f, 100f, 100f, 100f)
                    }
                    addItemView("设置浮窗子view点击事件") {
                        scopeFx.updateView {
                            it.getView<View>(R.id.cardItemFx)?.setOnClickListener {
                                Toast.makeText(
                                    this@ScopeActivity,
                                    "点击了内部cardView",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ViewGroup.addScopeViewGroup(): ViewGroup {
        val viewGroup = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                300
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
