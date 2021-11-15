package com.petterp.floatingx.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
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
        setContentView(
            LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                orientation = LinearLayout.VERTICAL
                addScopeViewGroup()
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
                    ImmersedActivity::class.java.start()
                }
                addItemView("跳转到局部悬浮窗页面-(测试api功能)") {
                    ScopeActivity::class.java.start()
                }
            }
        )
    }

    private fun ViewGroup.addScopeViewGroup() = addView(
        FrameLayout(context).apply {
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
                gravity = Gravity.CENTER
                setOnClickListener(click)
                this.text = text
            }
        )

    private fun Class<*>.start() {
        startActivity(Intent(this@MainActivity, this))
    }
}
