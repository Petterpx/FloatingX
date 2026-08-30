package com.petterp.floatingx.demo.pages

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.gesture.FxDrag
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.storage.FxSpStorage
import com.petterp.floatingx.core.update
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPageWithHeader
import com.petterp.floatingx.scope.fxScope

/**
 * 局部浮窗（floatingx-scope）三种宿主：
 * - ViewGroup：页面顶部那块黄色方块，浮窗只在方块内活动；
 * - Activity：挂在 android.R.id.content 上，铺满整页；
 * - Fragment：挂在 Fragment 根 view 上，view 创建后才出现（#244）。
 *
 * 局部浮窗不进 FloatingX 注册表，生命周期归调用方——所以本页 onDestroy 里统一 cancel。
 */
class ScopeHostActivity : AppCompatActivity() {

    /** ViewGroup 宿主：给 demoPageWithHeader 当 header 的黄色方块，高度必须写死才占得住位置 */
    private val box: FrameLayout by lazy {
        hostBox(this, R.string.label_viewgroup_host).apply {
            val height = (240 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
        }
    }

    /** Fragment 宿主：id 用资源 id，旋转重建后 FragmentManager 才找得回来 */
    private val slot: FrameLayout by lazy { fragmentSlot(this, 200) }

    // cancel 之后的 FxControl 不可复用（show/hide/moveTo 会抛 IllegalStateException，cancel() 本身幂等），
    // 所以按钮统一走 get()：没有或已 cancel 就重新建一个。
    private var boxFxOrNull: FxControl? = null
    private var actFxOrNull: FxControl? = null

    private val boxFx: FxControl
        get() = boxFxOrNull?.takeIf { it.state != FxState.CANCELLED } ?: box.fxScope("scope-box") {
            view { DemoContent.card(it, "Box") }
            anchor(FxGravity.TOP_START)
            enableLog("Fx-demo")
        }.also { boxFxOrNull = it }

    private val actFx: FxControl
        get() = actFxOrNull?.takeIf { it.state != FxState.CANCELLED } ?: fxScope("scope-act") {
            view { DemoContent.card(it, "Act") }
            anchor(FxGravity.BOTTOM_END)
            // 这里的 this 是 FxConfigScope，取 Context 要显式限定到 Activity
            persist(FxSpStorage(this@ScopeHostActivity))
        }.also { actFxOrNull = it }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPageWithHeader(box, R.string.page_scope_host_title) {
            section(R.string.section_scope_viewgroup)
            button(R.string.btn_show) { boxFx.show() }
            button(R.string.btn_hide) { boxFx.hide() }
            button(R.string.btn_drag_disabled) { boxFx.update { gesture { drag = FxDrag.DISABLED } } }
            button(R.string.btn_drag_long_press) { boxFx.update { gesture { drag = FxDrag.AFTER_LONG_PRESS } } }
            button(R.string.btn_drag_immediate) { boxFx.update { gesture { drag = FxDrag.IMMEDIATE } } }

            section(R.string.section_scope_activity)
            button(R.string.btn_show) { actFx.show() }
            button(R.string.btn_hide) { actFx.hide() }
            button(R.string.btn_cancel_recreate) { actFx.cancel() }
            note(R.string.note_scope_activity)

            section(R.string.section_fragment)
            note(R.string.note_scope_fragment)
            button(R.string.btn_fragment_add) {
                supportFragmentManager.beginTransaction().replace(R.id.fragmentSlot, ScopeFragment()).commit()
            }
            button(R.string.btn_fragment_remove) {
                scopeFragment()?.let { supportFragmentManager.beginTransaction().remove(it).commit() }
            }
            button(R.string.btn_fragment_detach) {
                scopeFragment()?.let { supportFragmentManager.beginTransaction().detach(it).commit() }
            }
            button(R.string.btn_fragment_attach) {
                scopeFragment()?.let { supportFragmentManager.beginTransaction().attach(it).commit() }
            }
            custom(slot)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewGroup / Activity 宿主的浮窗都不进注册表，页面销毁时自己收尾
        boxFxOrNull?.takeIf { it.state != FxState.CANCELLED }?.cancel()
        actFxOrNull?.takeIf { it.state != FxState.CANCELLED }?.cancel()
    }

    /** detach 之后 fragment 仍在 FragmentManager 里，按容器 id 还能找回来 */
    private fun scopeFragment(): Fragment? = supportFragmentManager.findFragmentById(R.id.fragmentSlot)
}

/**
 * #244 场景：浮窗在 onCreate 里创建，此时 fragment 还没有 view。
 * FragmentHost 观察 viewLifecycleOwnerLiveData——view 创建后挂载、view 销毁即摘下、
 * fragment destroy 时 Fragment.fxScope 自动 cancel，所以这里不需要任何手动清理。
 */
class ScopeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fxScope("scope-frag") {
            view { DemoContent.card(it, "Frag") }
            anchor(FxGravity.CENTER)
            enableLog("Fx-demo")
        }.show()
    }

    /** 根 view 必须是 ViewGroup，FragmentHost 才能往里加浮窗 */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        hostBox(inflater.context, R.string.label_fragment_root, Color.parseColor("#BBDEFB"))
}

/** 局部浮窗的宿主方块：带底色 + 一行说明，才看得出浮窗被限制在里面 */
internal fun hostBox(
    context: Context,
    @StringRes label: Int,
    color: Int = Color.parseColor("#FFF59D"),
): FrameLayout {
    val density = context.resources.displayMetrics.density
    fun Int.dp() = (this * density).toInt()
    return FrameLayout(context).apply {
        setBackgroundColor(color)
        addView(
            TextView(context).apply {
                text = context.getString(label)
                textSize = 12f
                setTextColor(Color.DKGRAY)
                setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.START,
            ),
        )
    }
}

/** Fragment 容器：放在示例列表里（跟着一起滚），高度固定 */
internal fun fragmentSlot(context: Context, heightDp: Int): FrameLayout {
    val density = context.resources.displayMetrics.density
    return FrameLayout(context).apply {
        id = R.id.fragmentSlot
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (heightDp * density).toInt(),
        ).apply {
            val margin = (16 * density).toInt()
            setMargins(margin, margin / 2, margin, margin)
        }
        setBackgroundColor(Color.parseColor("#EEEEEE"))
    }
}
