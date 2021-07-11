package com.petterp.floatingx.view

import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes

/**
 * @Author petterp
 * @Date 2021/5/24-7:06 下午
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
class FxViewHolder(val magnetView: FxMagnetView) {
    @PublishedApi
    internal val sparseArray: SparseArray<View> = SparseArray()

    // 添加进去的子View
    val childView: View?
        get() = magnetView.childView

    inline fun <reified T : View> getView(id: Int): T? {
        val bfView = sparseArray[id]
        return if (bfView == null) {
            val view = this.magnetView.findViewById<View>(id)
            if (view != null) sparseArray.put(id, view)
            view as? T
        } else bfView as? T
    }

    fun text(@IdRes id: Int, txt: String) {
        getView<TextView>(id)?.text = txt
    }

    fun imageResource(@IdRes id: Int, @DrawableRes source: Int) {
        getView<ImageView>(id)?.setImageResource(source)
    }

    fun backResource(@IdRes id: Int, @DrawableRes source: Int) {
        getView<ImageView>(id)?.setBackgroundResource(source)
    }
}
