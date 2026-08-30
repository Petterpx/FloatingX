package com.petterp.floatingx.demo.pages

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.update
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage
import com.petterp.floatingx.scope.fxScope
import com.petterp.floatingx.scope.viewGroupHost

/**
 * Modal 页：
 * - modal = 拦截内容之外的触摸（#212），可选点击外部自动 hide（#151）；只对 Layer 容器（app / scope）生效；
 * - Dialog 之上的浮窗：Dialog 有自己的 Window，挂在 Activity 上的浮窗盖不住它，
 *   要把浮窗直接挂到 Dialog 的 decorView 上（ViewGroupHost）。
 */
class ModalActivity : AppCompatActivity() {

    private var fxOrNull: FxControl? = null

    /** 「modal 下方按钮」被真正点到的次数：modal 打开**且浮窗显示中**时它应当一直不变 */
    @get:VisibleForTesting
    var outsideClicks: Int = 0
        private set

    /** 本页的局部浮窗（未创建过为 null）；instrumentation 用例靠它断言状态 */
    @get:VisibleForTesting
    val modalControl: FxControl?
        get() = fxOrNull

    private lateinit var clicksView: TextView

    private val fx: FxControl
        get() = fxOrNull?.takeIf { it.state != FxState.CANCELLED } ?: fxScope("modal") {
            view { ctx -> DemoContent.card(ctx, "Modal") }
            anchor(FxGravity.CENTER)
            modal(dismissOnOutsideTouch = true)
            enableLog("Fx-demo")
        }.also { fxOrNull = it }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clicksView = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.DKGRAY)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 4, pad, pad / 4)
        }
        renderClicks()
        demoPage(R.string.page_modal_title) {
            // 放在最上面：浮窗是居中显示的，这个按钮要离它足够远，点下去才算"点在内容之外"
            section(R.string.section_modal_check)
            note(R.string.note_modal_check)
            custom(clicksView)
            button(R.string.btn_modal_outside) {
                outsideClicks++
                renderClicks()
            }

            section(R.string.section_window)
            button(R.string.btn_show) { fx.show() }
            button(R.string.btn_hide) { fx.hide() }

            section(R.string.section_modal)
            note(R.string.note_modal)
            toggle(R.string.toggle_modal, true) { enabled ->
                fx.update { modal(enabled, dismissOnOutsideTouch = true) }
            }

            section(R.string.section_dialog_overlay)
            note(R.string.note_dialog_overlay)
            button(R.string.btn_show_dialog_window) { showDialogWithFloating() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fxOrNull?.takeIf { it.state != FxState.CANCELLED }?.cancel()
    }

    private fun renderClicks() {
        clicksView.text = getString(R.string.label_click_count, outsideClicks)
    }

    private fun showDialogWithFloating() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_plain_title)
            .setMessage(R.string.dialog_plain_message)
            .setPositiveButton(R.string.dialog_close, null)
            .create()
        dialog.show()
        // decorView 必须在 show() 之后取：show 之前 window 还没建好
        val decor = dialog.window?.decorView as? ViewGroup ?: return
        val dialogFx = FloatingX.create("dialog") {
            view { ctx -> DemoContent.card(ctx, "Dlg") }
            anchor(FxGravity.TOP_END)
            enableLog("Fx-demo")
            viewGroupHost(decor)
        }
        dialogFx.show()
        // 局部浮窗生命周期归调用方：Dialog 一关就 cancel
        dialog.setOnDismissListener {
            if (dialogFx.state != FxState.CANCELLED) dialogFx.cancel()
        }
    }
}
