package com.petterp.floatingx.app

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.simple.FxAnimationImpl
import com.petterp.floatingx.util.activityToFx
import com.petterp.floatingx.util.createFx

class MainActivity : AppCompatActivity() {

    private lateinit var viewGroup: ViewGroup

    private val activityFx by activityToFx(this) {
        setLayout(R.layout.item_floating)
    }

    private val viewFx by createFx({
        toControl(viewGroup)
    }) {
        setLayout(R.layout.item_floating)
        setEnableScrollOutsideScreen(false)
        setAnimationImpl(FxAnimationImpl())
        setEnableLog(true, "main_fx")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            viewGroup = addScopeViewGroup()
            addItemView("隐藏全局悬浮窗") {
                FloatingX.control().hide()
            }
            addItemView("显示全局悬浮窗-(展示与多指触摸)") {
                // 虽然可以不传递activity,不传递时将使用当前栈顶activity
//                FloatingX.control().show(this@MainActivity)
                FloatingX.control().show()
            }
            addItemView("更新当前全局浮窗显示View-(layoutId)") {
                FloatingX.control().apply {
                    updateManagerView(R.layout.item_floating)
                    updateView {
                        it.text(R.id.tvItemFx, "App")
                    }
                }.show()
            }
            addItemView("更新当前全局浮窗显示View-(layoutView)") {
                FloatingX.control().updateManagerView {
                    TextView(it).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        text = "App"
                        textSize = 15f
                        setBackgroundColor(Color.GRAY)
                        setPadding(10, 10, 10, 10)
                    }
                }
                FloatingX.control().show()
            }
            addItemView("显示Activity悬浮窗-(展示与多指触摸)") {
                activityFx.show()
                activityFx.updateView {
                    it.text(R.id.tvItemFx, "Act")
                    it.getView<CardView>(R.id.cardItemFx)?.setCardBackgroundColor(Color.BLUE)
                }
            }
            addItemView("显示View级别悬浮窗-(展示与多指触摸)") {
                viewFx.show()
                viewFx.updateView {
                    it.text(R.id.tvItemFx, "view")
                    it.getView<CardView>(R.id.cardItemFx)?.setCardBackgroundColor(Color.GREEN)
                }
            }
            addItemView("调整到无状态栏页面-(测试状态栏影响)") {
                ImmersedActivity::class.java.start(context)
            }
            addItemView("跳转到局部悬浮窗页面-(测试api功能)") {
                ScopeActivity::class.java.start(context)
            }
//            addItemView("跳转到测试页面-(测试申请权限的浮窗)") {
//                SingleActivity::class.java.start(context)
//            }
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
