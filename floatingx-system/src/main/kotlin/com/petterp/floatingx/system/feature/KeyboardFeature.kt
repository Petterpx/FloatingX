package com.petterp.floatingx.system.feature

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope
import com.petterp.floatingx.system.container.FxWindowContainer

/**
 * 系统窗口里的 EditText 唤起键盘（spec §4）：窗口默认 FLAG_NOT_FOCUSABLE 收不到输入，
 * 触摸到指定 EditText 时临时去掉该 flag 并弹键盘，返回键收起键盘后恢复。
 * 只对 Window 容器生效。
 */
public class KeyboardFeature(private val editTextIds: IntArray) : FxFeature {

    private var container: FxWindowContainer? = null
    private val bound = mutableListOf<View>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttach(scope: FxFeatureScope) {
        val c = scope.container as? FxWindowContainer
        if (c == null) {
            scope.logger?.e("KeyboardFeature 仅支持系统窗口容器，当前为 ${scope.container::class.java.simpleName}")
            return
        }
        container = c
        val root = c.contentView ?: return
        for (id in editTextIds) {
            val v = root.findViewById<View>(id) ?: continue
            v.setOnTouchListener { view, ev ->
                if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                    c.setWindowFocusable(true)
                    // 窗口变为可聚焦要等 WindowManager 应用新 LayoutParams，下一轮再请求焦点与键盘（非定位用途的 post）
                    view.post {
                        view.requestFocus()
                        imm(view.context)?.showSoftInput(view, 0)
                    }
                }
                false
            }
            bound += v
        }
        c.onImeBack = {
            bound.firstOrNull { it.hasFocus() }?.let { imm(it.context)?.hideSoftInputFromWindow(it.windowToken, 0) }
            c.setWindowFocusable(false)
        }
    }

    override fun onDetach() {
        bound.forEach { it.setOnTouchListener(null) }
        bound.clear()
        container?.let {
            it.onImeBack = null
            it.setWindowFocusable(false)
        }
        container = null
    }

    private fun imm(context: Context): InputMethodManager? =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
}
