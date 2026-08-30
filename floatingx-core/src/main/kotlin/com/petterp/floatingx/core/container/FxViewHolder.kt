package com.petterp.floatingx.core.container

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes

/** 内容 view 的 ViewHolder，findViewById 结果带缓存 */
public class FxViewHolder(public val view: View) {

    private val views = SparseArray<View>()

    public fun <T : View> getView(@IdRes viewId: Int): T =
        checkNotNull(getViewOrNull(viewId)) { "内容里没有 id=$viewId 的 view" }

    @Suppress("UNCHECKED_CAST")
    public fun <T : View> getViewOrNull(@IdRes viewId: Int): T? {
        val cached = views.get(viewId)
        if (cached != null) return cached as? T
        val found = view.findViewById<T>(viewId) ?: return null
        views.put(viewId, found)
        return found
    }

    public fun setOnClickListener(@IdRes viewId: Int, listener: View.OnClickListener): FxViewHolder = apply { getView<View>(viewId).setOnClickListener(listener) }
    public fun setText(@IdRes viewId: Int, value: CharSequence?): FxViewHolder = apply { getView<TextView>(viewId).text = value }
    public fun setText(@IdRes viewId: Int, @StringRes resId: Int): FxViewHolder = apply { getView<TextView>(viewId).setText(resId) }
    public fun setTextSize(@IdRes viewId: Int, size: Float): FxViewHolder = apply { getView<TextView>(viewId).textSize = size }
    public fun setTextSize(@IdRes viewId: Int, unit: Int, size: Float): FxViewHolder = apply { getView<TextView>(viewId).setTextSize(unit, size) }
    public fun setImageResource(@IdRes viewId: Int, @DrawableRes source: Int): FxViewHolder = apply { getView<ImageView>(viewId).setImageResource(source) }
    public fun setImageBitmap(@IdRes viewId: Int, bitmap: Bitmap?): FxViewHolder = apply { getView<ImageView>(viewId).setImageBitmap(bitmap) }
    public fun setImageDrawable(@IdRes viewId: Int, drawable: Drawable?): FxViewHolder = apply { getView<ImageView>(viewId).setImageDrawable(drawable) }
    public fun setBackgroundResource(@IdRes viewId: Int, @DrawableRes source: Int): FxViewHolder = apply { getView<View>(viewId).setBackgroundResource(source) }
    public fun setBackgroundColor(@IdRes viewId: Int, @ColorInt color: Int): FxViewHolder = apply { getView<View>(viewId).setBackgroundColor(color) }
    public fun setGone(@IdRes viewId: Int, gone: Boolean): FxViewHolder = apply { getView<View>(viewId).visibility = if (gone) View.GONE else View.VISIBLE }
    public fun setEnabled(@IdRes viewId: Int, enabled: Boolean): FxViewHolder = apply { getView<View>(viewId).isEnabled = enabled }
}
