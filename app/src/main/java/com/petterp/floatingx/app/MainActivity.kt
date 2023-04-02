package com.petterp.floatingx.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.simple.FxAnimationImpl
import com.petterp.floatingx.util.createFx

class MainActivity : AppCompatActivity() {

    private lateinit var viewGroup: FrameLayout

    private val activityFx by createFx {
        setLayout(R.layout.item_floating)
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
                    addItemView("更新当前[全局浮窗1]内容-(layoutId方式)") {
                        FloatingX.control(MultipleFxActivity.TAG_1).apply {
                            updateView(R.layout.item_floating)
                            this.updateViewContent {
                                it.setText(R.id.tvItemFx, "App")
                            }
                        }.show(this@MainActivity)
                    }
                    addItemView("更新当前[全局浮窗1]内容-(传递view方式)") {
                        FloatingX.control(MultipleFxActivity.TAG_1).apply {
                            updateView {
                                TextView(it).apply {
                                    layoutParams = ViewGroup.LayoutParams(50.dp, 50.dp)
                                    text = "App"
                                    gravity = Gravity.CENTER
                                    textSize = 15f
                                    setBackgroundColor(Color.GRAY)
                                }
                            }
                            show(this@MainActivity)
                        }
                    }
                    addItemView("隐藏全局悬浮窗") {
                        FloatingX.control(MultipleFxActivity.TAG_1).hide()
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
                    addItemView("进入多浮窗页面(测试多浮窗功能)") {
                        MultipleFxActivity::class.java.start(context)
                    }
                    addItemView("进入黑名单页面(该页面禁止展示浮窗1)") {
                        BlackActivity::class.java.start(context)
                    }
                    addItemView("进入无状态栏页面-(测试状态栏影响)") {
                        ImmersedActivity::class.java.start(context)
                    }
                    addItemView("进入局部悬浮窗页面-(测试api功能)") {
                        ScopeActivity::class.java.start(context)
                    }
//            addItemView("跳转到测试页面-(测试申请权限的浮窗)") {
//                SingleActivity::class.java.start(context)
//            }
                }
            }
        }
    }

    private fun ViewGroup.addScopeFrameViewGroup(): FrameLayout {
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
