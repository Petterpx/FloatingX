package com.petterp.floatingx.view

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes

/** FxManagerView对应的ViewHolder */
class FxViewHolder(private val itemView: View?) {

    private val views: SparseArray<View> = SparseArray()

    fun <T : View> getView(@IdRes viewId: Int): T {
        val view = getViewOrNull<T>(viewId)
        checkNotNull(view) { "No view found with id $viewId" }
        return view
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : View> getViewOrNull(@IdRes viewId: Int): T? {
        val view = views.get(viewId)
        return if (view == null) {
            itemView?.findViewById<T>(viewId)?.let {
                views.put(viewId, it)
                it
            }
        } else view as? T
    }

    fun setOnClickListener(@IdRes viewId: Int, listener: OnClickListener): FxViewHolder {
        getView<View>(viewId).setOnClickListener(listener)
        return this
    }

    fun setText(@IdRes viewId: Int, value: CharSequence?): FxViewHolder {
        getView<TextView>(viewId).text = value
        return this
    }

    fun setText(@IdRes viewId: Int, @StringRes resId: Int): FxViewHolder {
        getView<TextView>(viewId).setText(resId)
        return this
    }

    fun setTextSize(@IdRes viewId: Int, size: Float): FxViewHolder {
        getView<TextView>(viewId).textSize = size
        return this
    }

    fun setTextSize(@IdRes viewId: Int, unit: Int, size: Float): FxViewHolder {
        getView<TextView>(viewId).setTextSize(unit, size)
        return this
    }

    fun setImageResource(@IdRes viewId: Int, @DrawableRes source: Int): FxViewHolder {
        getView<ImageView>(viewId).setImageResource(source)
        return this
    }

    fun setImageBitMap(@IdRes viewId: Int, bitmap: Bitmap?): FxViewHolder {
        getView<ImageView>(viewId).setImageBitmap(bitmap)
        return this
    }

    fun setImageDrawable(@IdRes viewId: Int, drawable: Drawable?): FxViewHolder {
        getView<ImageView>(viewId).setImageDrawable(drawable)
        return this
    }

    fun setBackgroundResource(@IdRes id: Int, @DrawableRes source: Int): FxViewHolder {
        getView<View>(id).setBackgroundResource(source)
        return this
    }

    fun setBackgroundColor(@IdRes id: Int, @ColorInt color: Int): FxViewHolder {
        getView<View>(id).setBackgroundColor(color)
        return this
    }

    fun setGone(@IdRes viewId: Int, isGone: Boolean): FxViewHolder {
        val view = getView<View>(viewId)
        view.visibility = if (isGone) View.GONE else View.VISIBLE
        return this
    }

    fun setEnabled(@IdRes viewId: Int, isEnabled: Boolean): FxViewHolder {
        getView<View>(viewId).isEnabled = isEnabled
        return this
    }
}
