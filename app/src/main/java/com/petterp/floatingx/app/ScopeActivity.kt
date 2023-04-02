package com.petterp.floatingx.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.app.simple.FxAnimationImpl
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.util.createFx

/** @author petterp */
class ScopeActivity : AppCompatActivity() {

    private val viewGroup by lazy(LazyThreadSafetyMode.NONE) {
        FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                600
            ).apply {
                leftMargin = 50
                topMargin = 50
                rightMargin = 50
                bottomMargin = 50
            }
            setBackgroundColor(Color.YELLOW)
        }
    }

    private val scopeFx by createFx {
        setLayout(R.layout.item_floating)
        setEnableScrollOutsideScreen(false)
        setEnableEdgeAdsorption(false)
        setGravity(FxGravity.RIGHT_OR_TOP)
        setEnableAssistDirection(t = 10f, r = 10f)
        setEdgeOffset(40f)
        setBottomBorderMargin(40f)
        setAnimationImpl(FxAnimationImpl())
        setEnableAnimation(false)
        setEnableLog(true)
        build().toControl(viewGroup)
    }

    private val activityFx by createFx {
        setLayout(R.layout.item_floating)
        setEnableScrollOutsideScreen(false)
        setEnableEdgeAdsorption(false)
        setEdgeOffset(40f)
        setBottomBorderMargin(40f)
        setAnimationImpl(FxAnimationImpl())
        setEnableAnimation(false)
        setEnableLog(true)
        build().toControl(this@ScopeActivity)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            addView(viewGroup)
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
                        scopeFx.configControl.setEnableTouch(false)
                    }
                    addItemView("允许触摸事件(允许拖动)-默认允许") {
                        scopeFx.configControl.setEnableTouch(true)
                    }
                    addItemView("隐藏悬浮窗") {
                        scopeFx.hide()
                    }
                    addItemView("更换layout(通过布局更换)") {
                        scopeFx.updateView(R.layout.item_floating)
                    }
                    addItemView("更换layoutView(通过传递View)") {
                        scopeFx.updateView {
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
                        scopeFx.configControl.setEnableClick(false)
                    }
                    addItemView("打开点击事件响应") {
                        scopeFx.configControl.setEnableClick(true)
                    }
                    addItemView("当前是否显示") {
                        Toast.makeText(
                            this@ScopeActivity,
                            "当前是否显示-${scopeFx.isShow()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    addItemView("允许边缘吸附") {
                        scopeFx.configControl.setEnableEdgeAdsorption(true)
                    }
                    addItemView("禁止边缘吸附") {
                        scopeFx.configControl.setEnableEdgeAdsorption(false)
                    }
                    addItemView("允许边缘回弹") {
                        scopeFx.configControl.setEnableEdgeRebound(true)
                    }
                    addItemView("禁止边缘回弹") {
                        scopeFx.configControl.setEnableEdgeRebound(false)
                    }
                    addItemView("开启动画") {
                        scopeFx.configControl.setEnableAnimation(true)
                    }
                    addItemView("边距调整为100f") {
                        scopeFx.configControl.setBorderMargin(100f, 100f, 100f, 100f)
                    }
                    addItemView("移动浮窗到(0,0)") {
                        scopeFx.getManagerView()?.apply {
                            moveLocation(0F, 0F)
                        }
                    }
                    addItemView("浮窗向左移动20F") {
                        scopeFx.getManagerView()?.apply {
                            moveLocationByVector(-20F, 0F)
                        }
                    }
                    addItemView("设置浮窗子view点击事件(layoutId的示例)") {
                        scopeFx.updateViewContent {
                            it.getView<TextView>(R.id.tvItemFx).setOnClickListener {
                                Toast.makeText(this@ScopeActivity, "123123", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
            }
        }
        scopeFx.show()
    }
}
