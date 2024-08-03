package com.petterp.floatingx.util

import android.annotation.SuppressLint
import android.content.Context.INPUT_METHOD_SERVICE
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.R
import com.petterp.floatingx.view.FxSystemContainerView

/**
 * Fx键盘助手
 * @author petterp
 */
object FxInputHelper {

    @SuppressLint("ClickableViewAccessibility")
    fun setEditTextAdapt(view: EditText?, fxTag: String) {
        if (view == null) return
        val helper = view.getTag(R.id.fx_input_touch_key)
        if (helper != null) return
        val imp = FxKeyBoardTouchImpl(fxTag)
        view.setTag(R.id.fx_input_touch_key, imp)
        view.setOnTouchListener(imp)
    }

    fun showKeyBoard(fxTag: String, view: View) {
        val fxView = FloatingX.controlOrNull(fxTag)?.groupView<FxSystemContainerView>() ?: return
        fxView.updateKeyBoardStatus(true)
        view.post {
            val inputManager =
                view.context.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            inputManager?.showSoftInput(view, 0)
        }
    }

    fun hideKeyBoard(fxTag: String, view: View) {
        val fxView = FloatingX.controlOrNull(fxTag)?.groupView<FxSystemContainerView>() ?: return
        fxView.updateKeyBoardStatus(false)
        view.post {
            val inputManager =
                view.context.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            inputManager?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}

internal class FxKeyBoardTouchImpl(private val tag: String) : View.OnTouchListener {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN && v != null) {
            FxInputHelper.showKeyBoard(tag, v)
        }
        return false
    }
}