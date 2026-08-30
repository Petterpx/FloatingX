package com.petterp.floatingx.demo.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.petterp.floatingx.demo.R

/** 各页共用的浮窗内容 */
object DemoContent {

    private const val CARD_SIZE_DP = 120
    private const val RESIZABLE_INIT_DP = 140
    private const val RESIZABLE_MIN_DP = 100
    private const val RESIZABLE_MAX_DP = 280
    private const val LIST_WIDTH_DP = 220

    /** 圆形卡片；用 theme 化的 context 才能解析 Material 属性 */
    fun card(context: Context, text: String): View =
        LayoutInflater.from(context).inflate(R.layout.fx_card, null, false).also { root ->
            // 同 resizable：inflate(root = null) 不会带上 XML 里的 layout_width/height（没有 parent 就
            // 解析不出 LayoutParams），不补的话 120dp 圆卡会退化成 wrap_content，只剩文字那么大
            val size = CARD_SIZE_DP.dp(context)
            root.layoutParams = ViewGroup.LayoutParams(size, size)
            root.findViewById<TextView>(R.id.tvTitle).text = text
        }

    /**
     * 可变尺寸卡片：卡片内自带「变大 / 变小」，页面上的按钮走 [resize]。
     * 用来验证内容尺寸变化时锚点不动（#187）。
     */
    fun resizable(context: Context): View =
        LayoutInflater.from(context).inflate(R.layout.fx_card_resizable, null, false).also { root ->
            // inflate(root = null) 不会带上 XML 里的 layout_width/height（没有 parent 就解析不出 LayoutParams），
            // 这里显式给一份确定的初值，resize() 才有基准可加减
            val size = RESIZABLE_INIT_DP.dp(context)
            root.layoutParams = ViewGroup.LayoutParams(size, size)
            root.findViewById<View>(R.id.btnGrow).setOnClickListener { resize(root, 40) }
            root.findViewById<View>(R.id.btnShrink).setOnClickListener { resize(root, -40) }
            root.findViewById<TextView>(R.id.tvSize).text = "$RESIZABLE_INIT_DP x $RESIZABLE_INIT_DP"
        }

    /**
     * 改内容 view 自身的宽高（[deltaDp] 为 dp 增量）并 requestLayout。
     * 浮窗不需要额外通知：容器监听内容的 layout 变化，尺寸一变就按锚点重算位置。
     */
    fun resize(view: View, deltaDp: Int) {
        val density = view.resources.displayMetrics.density
        val delta = (deltaDp * density).toInt()
        val min = RESIZABLE_MIN_DP.dp(view.context)
        val max = RESIZABLE_MAX_DP.dp(view.context)
        val lp = view.layoutParams ?: return
        // WRAP_CONTENT/MATCH_PARENT 是负数，这种情况下拿实测尺寸当基准
        val width = if (lp.width > 0) lp.width else view.width
        val height = if (lp.height > 0) lp.height else view.height
        lp.width = (width + delta).coerceIn(min, max)
        lp.height = (height + delta).coerceIn(min, max)
        view.layoutParams = lp
        view.requestLayout()
        view.findViewById<TextView>(R.id.tvSize)?.text =
            "${(lp.width / density).toInt()} x ${(lp.height / density).toInt()}"
    }

    /** header + 可滚动列表的内容：演示 dragRegion 与 childPriority */
    fun list(context: Context, adapter: RecyclerView.Adapter<*>): View =
        LayoutInflater.from(context).inflate(R.layout.fx_list, null, false).also { root ->
            // 同 resizable：root=null 时 XML 的宽高丢了，显式补一份
            root.layoutParams = ViewGroup.LayoutParams(LIST_WIDTH_DP.dp(context), ViewGroup.LayoutParams.WRAP_CONTENT)
            root.findViewById<RecyclerView>(R.id.rv).apply {
                layoutManager = LinearLayoutManager(context)
                this.adapter = adapter
            }
        }

    fun toast(context: Context, message: String) {
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun Int.dp(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
}
