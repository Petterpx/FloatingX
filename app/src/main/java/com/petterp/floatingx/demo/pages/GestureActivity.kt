package com.petterp.floatingx.demo.pages

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.gesture.FxChildPriority
import com.petterp.floatingx.core.gesture.FxDrag
import com.petterp.floatingx.core.gesture.FxRegion
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.update
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage
import com.petterp.floatingx.scope.fxScope

/**
 * 手势页：拖动模式 / 拖动区域 / 与子 view 的冲突策略 / 触摸透传 / 回调。
 *
 * 用局部浮窗（挂在 android.R.id.content 上），内容是「header + 可滚动列表」，
 * 这样才看得出 dragRegion（只有 header 能起拖）和 childPriority（拖窗口还是滚列表）的差别。
 */
class GestureActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private val logs = ArrayDeque<String>()

    /** 回调打到页面底部；浮窗盖住的话把它拖开就能看到 */
    private val logListener = object : FxListener {
        override fun onClick(control: FxControl, view: View) = log("onClick")
        override fun onLongClick(control: FxControl, view: View) = log("onLongClick")
        override fun onDragStart(control: FxControl) = log("onDragStart")
        override fun onDragEnd(control: FxControl, x: Float, y: Float) = log("onDragEnd(${x.toInt()}, ${y.toInt()})")
    }

    // cancel 之后的 FxControl 不可复用（show/hide/moveTo 会抛 IllegalStateException），按钮统一走 get()：没有或已 cancel 就重建
    private var fxOrNull: FxControl? = null

    private val fx: FxControl
        get() = fxOrNull?.takeIf { it.state != FxState.CANCELLED } ?: fxScope("gesture") {
            view { ctx -> DemoContent.list(ctx, RowAdapter(ROW_COUNT)) }
            anchor(FxGravity.TOP_START, dx = 16f, dy = 16f)
            enableLog("Fx-demo")
        }.also {
            it.addListener(logListener)
            fxOrNull = it
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logView = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.DKGRAY)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 4, pad, pad)
            text = getString(R.string.hint_gesture_log)
        }
        demoPage(R.string.page_gesture_title) {
            section(R.string.section_window)
            button(R.string.btn_show) { fx.show() }
            button(R.string.btn_hide) { fx.hide() }

            section(R.string.section_drag_mode)
            button(R.string.btn_drag_mode_immediate) { fx.update { gesture { drag = FxDrag.IMMEDIATE } } }
            button(R.string.btn_drag_mode_long_press) { fx.update { gesture { drag = FxDrag.AFTER_LONG_PRESS } } }
            button(R.string.btn_drag_mode_disabled) { fx.update { gesture { drag = FxDrag.DISABLED } } }

            section(R.string.section_drag_region)
            note(R.string.note_drag_region)
            button(R.string.btn_drag_region_header) { fx.update { gesture { dragRegion = FxRegion.child(R.id.tvHeader) } } }
            button(R.string.btn_drag_region_any) { fx.update { gesture { dragRegion = null } } }

            section(R.string.section_child_priority)
            note(R.string.note_child_priority)
            button(R.string.btn_child_priority_auto) { fx.update { gesture { childPriority = FxChildPriority.AUTO } } }
            button(R.string.btn_child_priority_parent) { fx.update { gesture { childPriority = FxChildPriority.PARENT } } }
            button(R.string.btn_child_priority_child) { fx.update { gesture { childPriority = FxChildPriority.CHILD } } }

            section(R.string.section_pass_through)
            note(R.string.note_touchable_gesture)
            toggle(R.string.toggle_touchable, true) { enabled -> fx.update { gesture { touchable = enabled } } }

            section(R.string.section_callbacks)
            note(R.string.note_callbacks)
            custom(logView)
        }
        fx.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 局部浮窗不进注册表，生命周期归调用方
        fxOrNull?.takeIf { it.state != FxState.CANCELLED }?.cancel()
    }

    private fun log(event: String) {
        logs.addFirst(event)
        while (logs.size > MAX_LOGS) logs.removeLast()
        logView.text = logs.joinToString("\n")
    }

    /** 20 行的简单列表；item 直接用代码建 TextView，省一个布局文件 */
    private class RowAdapter(private val count: Int) : RecyclerView.Adapter<RowAdapter.VH>() {

        class VH(val text: TextView) : RecyclerView.ViewHolder(text)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val density = parent.resources.displayMetrics.density
            fun Int.dp() = (this * density).toInt()
            val text = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                )
                setPadding(12.dp(), 10.dp(), 12.dp(), 10.dp())
                textSize = 13f
            }
            return VH(text)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.text.text = holder.text.context.getString(R.string.item_row, position + 1)
        }

        override fun getItemCount(): Int = count
    }

    private companion object {
        const val ROW_COUNT = 20
        const val MAX_LOGS = 6
    }
}
