package com.petterp.floatingx.core.config

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

/**
 * 浮窗内容的来源。开放继承：compose 模块提供 Compose 实现。
 * 内容 view 在 install 时用 host.context 创建一次，之后跨 host 复用（spec §2.2）。
 */
public abstract class FxContent {

    /** 创建内容 view；parent 仅用于解析 LayoutParams，实现不得 addView */
    public abstract fun create(context: Context, parent: ViewGroup): View

    public class Layout(@LayoutRes public val id: Int) : FxContent() {
        override fun create(context: Context, parent: ViewGroup): View =
            LayoutInflater.from(context).inflate(id, parent, false)
    }

    public class Static(public val view: View) : FxContent() {
        override fun create(context: Context, parent: ViewGroup): View = view
    }

    public class Provider(public val provider: (Context) -> View) : FxContent() {
        override fun create(context: Context, parent: ViewGroup): View = provider(context)
    }

    public companion object {
        @JvmStatic public fun layout(@LayoutRes id: Int): FxContent = Layout(id)

        @JvmStatic public fun view(view: View): FxContent = Static(view)

        @JvmStatic public fun provider(provider: (Context) -> View): FxContent = Provider(provider)
    }
}
