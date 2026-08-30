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
            text = LOG_EMPTY
        }
        demoPage("手势") {
            section("浮窗")
            button("显示") { fx.show() }
            button("隐藏") { fx.hide() }

            section("拖动模式")
            button("IMMEDIATE（按下即可拖，默认）") { fx.update { gesture { drag = FxDrag.IMMEDIATE } } }
            button("AFTER_LONG_PRESS（长按后才可拖，#222）") { fx.update { gesture { drag = FxDrag.AFTER_LONG_PRESS } } }
            button("DISABLED（禁止拖动，只保留点击）") { fx.update { gesture { drag = FxDrag.DISABLED } } }

            section("拖动区域")
            note("dragRegion 限定「从哪里按下才算起拖」（#165）；区域之外的按下照常交给内容自己处理。")
            button("只允许 header 拖动") { fx.update { gesture { dragRegion = FxRegion.child(R.id.tvHeader) } } }
            button("任意位置都能拖") { fx.update { gesture { dragRegion = null } } }

            section("子 view 优先级")
            note("内容里有可滚动的 RecyclerView 时，纵向手势归谁：")
            button("AUTO（落点下可滚就不抢，默认）") { fx.update { gesture { childPriority = FxChildPriority.AUTO } } }
            button("PARENT（超过 slop 就抢，列表滚不动）") { fx.update { gesture { childPriority = FxChildPriority.PARENT } } }
            button("CHILD（永不抢，只能滚列表）") { fx.update { gesture { childPriority = FxChildPriority.CHILD } } }

            section("透传")
            note("关掉 touchable 后浮窗完全不吃触摸（#243/#108），点它等于点下面的页面。")
            toggle("touchable", true) { enabled -> fx.update { gesture { touchable = enabled } } }

            section("回调")
            note("addListener 的 onClick / onLongClick / onDragStart / onDragEnd：")
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
            holder.text.text = "第 ${position + 1} 行"
        }

        override fun getItemCount(): Int = count
    }

    private companion object {
        const val ROW_COUNT = 20
        const val MAX_LOGS = 6
        const val LOG_EMPTY = "（点击 / 长按 / 拖动浮窗试试）"
    }
}
