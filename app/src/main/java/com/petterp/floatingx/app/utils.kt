package com.petterp.floatingx.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView

/**
 * 写示例页面的工具
 *
 * @author petterp
 */
inline fun Activity.createLinearLayoutToParent(obj: LinearLayout.() -> Unit) {
    val view = createLinearLayout(obj)
    setContentView(view)
}

inline fun Context.createLinearLayout(obj: LinearLayout.() -> Unit) =
    LinearLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        orientation = LinearLayout.VERTICAL
        obj.invoke(this)
    }

inline fun ViewGroup.addLinearLayout(obj: LinearLayout.() -> Unit) =
    addView(
        LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            obj.invoke(this)
        }
    )

inline fun ViewGroup.addFrameLayout(obj: FrameLayout.() -> Unit) =
    addView(
        FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            obj.invoke(this)
        }
    )

inline fun ViewGroup.addNestedScrollView(obj: NestedScrollView.() -> Unit) {
    addView(
        NestedScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -2)
            obj.invoke(this)
        }
    )
}

fun ViewGroup.addItemView(text: String, click: View.OnClickListener) =
    addView(
        Button(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            (this as TextView).isAllCaps = false
            gravity = Gravity.CENTER
            setOnClickListener(click)
            this.text = text
        }
    )

inline fun ViewGroup.addTextView(obj: TextView.() -> Unit) {
    addView(
        TextView(context).apply {
            obj.invoke(this)
        }
    )
}

fun Class<*>.start(context: Context) {
    context.startActivity(Intent(context, this))
}

val Number.dp: Int
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toFloat(), Resources.getSystem().displayMetrics).toInt()

val Number.dpF: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toFloat(), Resources.getSystem().displayMetrics)