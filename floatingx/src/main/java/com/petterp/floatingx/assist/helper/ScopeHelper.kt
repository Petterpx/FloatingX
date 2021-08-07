package com.petterp.floatingx.assist.helper

import com.petterp.floatingx.impl.control.FxViewControl
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.listener.control.IFxScopeControl

/** 特定范围的Helper构建器 */
class ScopeHelper : BaseHelper() {

    fun toControl(): IFxScopeControl<IFxControl> = FxViewControl(this)

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()

        inline fun toControl(obj: Builder.() -> Unit) = Builder().apply(obj).build().toControl()
    }

    class Builder : BaseHelper.Builder<Builder, ScopeHelper>() {
        override fun buildHelper(): ScopeHelper = ScopeHelper()
    }
}
