package com.petterp.floatingx.demo.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

/** 示例页 DSL：section / button / toggle / note / page，一行一个按钮，避免每页重复样板 */
class DemoPageScope(private val root: LinearLayout) {
    private val ctx get() = root.context

    private fun Int.dp(): Int = (this * ctx.resources.displayMetrics.density).toInt()

    fun section(title: String) {
        root.addView(
            TextView(ctx).apply {
                text = title
                textSize = 13f
                setTextColor(Color.GRAY)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(16.dp(), 20.dp(), 16.dp(), 6.dp())
            },
        )
    }

    fun note(text: String) {
        root.addView(
            TextView(ctx).apply {
                this.text = text
                textSize = 12f
                setTextColor(Color.DKGRAY)
                setPadding(16.dp(), 4.dp(), 16.dp(), 8.dp())
            },
        )
    }

    fun button(text: String, onClick: (View) -> Unit) {
        root.addView(
            MaterialButton(ctx).apply {
                this.text = text
                isAllCaps = false
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setOnClickListener(onClick)
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(16.dp(), 2.dp(), 16.dp(), 2.dp())
            },
        )
    }

    fun toggle(text: String, initial: Boolean, onChange: (Boolean) -> Unit) {
        root.addView(
            MaterialSwitch(ctx).apply {
                this.text = text
                isChecked = initial
                setOnCheckedChangeListener { _, checked -> onChange(checked) }
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(20.dp(), 2.dp(), 20.dp(), 2.dp())
            },
        )
    }

    fun custom(view: View) {
        root.addView(view)
    }

    /** 跳转到另一个示例页 */
    fun page(text: String, cls: Class<out Activity>) = button(text) { it.context.startActivity(Intent(it.context, cls)) }
}

/**
 * 主题是 NoActionBar，[Activity.setTitle] 在页面上看不见，
 * 所以标题额外在列顶画一个加粗 TextView（setTitle 也留着，最近任务列表等地方还用得上）。
 */
private fun Activity.addTitle(column: LinearLayout, title: String) {
    setTitle(title)
    val pad = (16 * resources.displayMetrics.density).toInt()
    column.addView(
        TextView(this).apply {
            text = title
            textSize = 20f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(pad, pad, pad, pad / 2)
        },
    )
}

/** Activity 直接 setContentView 一个可滚动的示例页 */
fun Activity.demoPage(title: String? = null, block: DemoPageScope.() -> Unit) {
    val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    title?.let { addTitle(column, it) }
    DemoPageScope(column).block()
    setContentView(NestedScrollView(this).apply { addView(column) })
}

/** 可以在页面顶部放一个真实的 ViewGroup（局部浮窗宿主）再接示例列表 */
fun Activity.demoPageWithHeader(header: View, title: String? = null, block: DemoPageScope.() -> Unit) {
    val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    outer.addView(header)
    val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    title?.let { addTitle(column, it) }
    DemoPageScope(column).block()
    outer.addView(
        NestedScrollView(this).apply { addView(column) },
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f),
    )
    setContentView(outer)
}
