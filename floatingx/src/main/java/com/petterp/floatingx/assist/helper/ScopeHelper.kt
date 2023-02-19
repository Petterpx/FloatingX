package com.petterp.floatingx.assist.helper

import android.app.Activity
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.petterp.floatingx.impl.control.FxScopeControl
import com.petterp.floatingx.listener.control.IFxScopeControl
import com.petterp.floatingx.util.FxScopeEnum
import com.petterp.floatingx.util.contentView

/** 特定范围的Helper构建器 */
class ScopeHelper : BasisHelper() {

    /** 插入到Activity中 */
    fun toControl(activity: Activity): IFxScopeControl<Activity> {
        initLog(FxScopeEnum.ACTIVITY_SCOPE.tag)
        val control = FxScopeControl<Activity>(this)
        activity.contentView?.let {
            control.setContainerGroup(it)
        } ?: fxLog?.e("install to Activity the Error,current contentView(R.id.content) = null!")
        return control
    }

    /** 插入到Fragment中 */
    fun toControl(fragment: Fragment): IFxScopeControl<Fragment> {
        initLog(FxScopeEnum.FRAGMENT_SCOPE.tag)
        val rootView = fragment.view as? FrameLayout
        checkNotNull(rootView) {
            "Check if your root layout is FrameLayout, or if the init call timing is after onCreateView()!"
        }
        val control = FxScopeControl<Fragment>(this)
        control.setContainerGroup(rootView)
        return control
    }

    /** 插入到ViewGroup中 */
    fun toControl(group: FrameLayout): IFxScopeControl<FrameLayout> {
        initLog(FxScopeEnum.VIEW_GROUP_SCOPE.tag)
        val control = FxScopeControl<FrameLayout>(this)
        control.setContainerGroup(group)
        return control
    }

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
