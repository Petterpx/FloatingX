package com.petterp.floatingx.assist.helper

import android.app.Activity
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.petterp.floatingx.impl.control.FxScopeControl
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.listener.control.IFxScopeControl

/** 特定范围的Helper构建器 */
class ScopeHelper : BasisHelper() {

    fun toControl(activity: Activity): IFxControl =
        toControl().init(activity)

    fun toControl(fragment: Fragment): IFxControl =
        toControl().init(fragment)

    fun toControl(group: ViewGroup): IFxControl =
        toControl().init(group)

    private fun toControl(): IFxScopeControl<IFxControl> = FxScopeControl(this)

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()

        inline fun build(obj: Builder.() -> Unit) = builder().apply(obj).build()
    }

    class Builder : BasisHelper.Builder<Builder, ScopeHelper>() {
        override fun buildHelper(): ScopeHelper = ScopeHelper()
    }
}
