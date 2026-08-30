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
        hostBox(this, "ViewGroup 宿主：浮窗只能在本区域内活动").apply {
            val height = (240 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
        }
    }

    /** Fragment 宿主：id 用资源 id，旋转重建后 FragmentManager 才找得回来 */
    private val slot: FrameLayout by lazy { fragmentSlot(this, 200) }

    // cancel 之后的 FxControl 不可复用（再调用会抛 IllegalStateException），
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
        demoPageWithHeader(box, "局部浮窗") {
            section("ViewGroup 内浮窗")
            button("显示") { boxFx.show() }
            button("隐藏") { boxFx.hide() }
            button("禁止拖动") { boxFx.update { gesture { drag = FxDrag.DISABLED } } }
            button("长按后可拖") { boxFx.update { gesture { drag = FxDrag.AFTER_LONG_PRESS } } }
            button("恢复（按下即可拖）") { boxFx.update { gesture { drag = FxDrag.IMMEDIATE } } }

            section("Activity 内浮窗")
            button("显示") { actFx.show() }
            button("隐藏") { actFx.hide() }
            button("cancel（销毁，再点显示会重建）") { actFx.cancel() }
            note("API 29+ 页面销毁自动 cancel；低版本需自行在 onDestroy 里 cancel（本页两种都做了）。位置用 FxSpStorage 记住，返回本页会恢复。")

            section("Fragment")
            note("ScopeFragment 在 onCreate（view 还没创建）里就调用了 fxScope().show()，浮窗要等 view 就绪才出现——正是 #244。")
            button("添加 Fragment（内含浮窗）") {
                supportFragmentManager.beginTransaction().replace(R.id.fragmentSlot, ScopeFragment()).commit()
            }
            button("移除 Fragment（浮窗随之 cancel）") {
                scopeFragment()?.let { supportFragmentManager.beginTransaction().remove(it).commit() }
            }
            button("detach Fragment（view 销毁，浮窗消失）") {
                scopeFragment()?.let { supportFragmentManager.beginTransaction().detach(it).commit() }
            }
            button("attach Fragment（view 重建，浮窗自动回来）") {
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
        hostBox(inflater.context, "Fragment 根 view", Color.parseColor("#BBDEFB"))
}

/** 局部浮窗的宿主方块：带底色 + 一行说明，才看得出浮窗被限制在里面 */
internal fun hostBox(
    context: Context,
    label: String,
    color: Int = Color.parseColor("#FFF59D"),
): FrameLayout {
    val density = context.resources.displayMetrics.density
    fun Int.dp() = (this * density).toInt()
    return FrameLayout(context).apply {
        setBackgroundColor(color)
        addView(
            TextView(context).apply {
                text = label
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
