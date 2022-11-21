package com.petterp.floatingx.assist.helper

import android.app.Activity
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.petterp.floatingx.impl.control.FxScopeControl
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.listener.control.IFxScopeControl

/** 特定范围的Helper构建器 */
class ScopeHelper : BasisHelper() {

    /** 插入到Activity中 */
    fun toControl(activity: Activity): IFxControl =
        toControl().init(activity)

    /** 插入到Fragment中 */
    fun toControl(fragment: Fragment): IFxControl =
        toControl().init(fragment)

    /** 插入到ViewGroup中 */
    fun toControl(group: FrameLayout): IFxControl =
        toControl().init(group)

    private fun toControl(): IFxScopeControl<IFxControl> = FxScopeControl(this)

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()

        @JvmSynthetic
        inline fun build(obj: Builder.() -> Unit) = builder().apply(obj).build()
    }

    class Builder : BasisHelper.Builder<Builder, ScopeHelper>() {
        override fun buildHelper(): ScopeHelper = ScopeHelper()
    }
}
