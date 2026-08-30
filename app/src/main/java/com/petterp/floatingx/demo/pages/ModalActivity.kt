package com.petterp.floatingx.demo.pages

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.update
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

    private val fx: FxControl
        get() = fxOrNull?.takeIf { it.state != FxState.CANCELLED } ?: fxScope("modal") {
            view { ctx -> DemoContent.card(ctx, "Modal") }
            anchor(FxGravity.CENTER)
            modal(dismissOnOutsideTouch = true)
            enableLog("Fx-demo")
        }.also { fxOrNull = it }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("Modal") {
            section("浮窗")
            button("显示") { fx.show() }
            button("隐藏") { fx.hide() }

            section("modal")
            note("开启时：浮窗之外的触摸被容器吃掉（下面的按钮点不动），并且点一下外部浮窗自动 hide；关闭时触摸照常透传。")
            toggle("modal（拦截外部触摸 + 点外部自动 hide）", true) { enabled ->
                fx.update { modal(enabled, dismissOnOutsideTouch = true) }
            }

            section("Dialog 之上的浮窗")
            note("Dialog 是独立 Window，层级在 Activity 之上：想让浮窗盖住 Dialog，就把它挂到 Dialog 自己的 decorView 上。")
            button("弹 Dialog 并在其上显示浮窗") { showDialogWithFloating() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fxOrNull?.takeIf { it.state != FxState.CANCELLED }?.cancel()
    }

    private fun showDialogWithFloating() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("普通 Dialog")
            .setMessage("右上角的「Dlg」浮窗挂在本 Dialog 的 decorView 上，可以在 Dialog 范围内拖动；Dialog 关闭时它随之 cancel。")
            .setPositiveButton("关闭", null)
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
