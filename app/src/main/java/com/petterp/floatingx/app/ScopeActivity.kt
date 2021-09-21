package com.petterp.floatingx.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.petterp.floatingx.app.simple.FxAnimationImpl
import com.petterp.floatingx.util.createFx

/**
 *
 * @author petterp
 */
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                orientation = LinearLayout.VERTICAL
                addScopeViewGroup()
                addView(
                    TextView(context).apply {
                        text = "api列表可拖动"
                        gravity = Gravity.CENTER
                        setPadding(0, 0, 0, 20)
                        textSize = 19f
                    }
                )
                addView(
                    NestedScrollView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(-1, -2)
                        addView(
                            LinearLayout(context).apply {
                                orientation = LinearLayout.VERTICAL
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
                                addItemView("更换layout") {
                                    scopeFx.updateManagerView(R.layout.item_floating_new)
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
                        )
                    }
                )
            }
        )
    }

    private fun ViewGroup.addScopeViewGroup() = addView(
        FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                700
            ).apply {
                leftMargin = 50
                topMargin = 50
                rightMargin = 50
                bottomMargin = 50
            }
            setBackgroundColor(Color.YELLOW)
            viewGroup = this
        }
    )

    private fun ViewGroup.addItemView(text: String, click: View.OnClickListener) =
        addView(
            Button(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                (this as TextView).isAllCaps = false
                gravity = Gravity.CENTER
                setOnClickListener(click)
                this.text = text
            }
        )
}
