package com.petterp.floatingx.app

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
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
            addItemView("显示全局悬浮窗-(展示与多指触摸)") {
                FloatingX.control().show(this@MainActivity)
                FloatingX.control().updateView {
                    it.text(R.id.tvItemFx, "App")
                }
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
