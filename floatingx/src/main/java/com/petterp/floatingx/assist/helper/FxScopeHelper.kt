package com.petterp.floatingx.assist.helper

import android.app.Activity
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.petterp.floatingx.imp.scope.FxScopeControl
import com.petterp.floatingx.listener.control.IFxScopeControl
import com.petterp.floatingx.util.FX_INSTALL_SCOPE_ACTIVITY_TAG
import com.petterp.floatingx.util.FX_INSTALL_SCOPE_FRAGMENT_TAG
import com.petterp.floatingx.util.FX_INSTALL_SCOPE_VIEW_GROUP_TAG
import com.petterp.floatingx.util.contentView

/** 特定范围的Helper构建器 */
class FxScopeHelper : FxBasisHelper() {

    /** 插入到Activity中 */
    fun toControl(activity: Activity): IFxScopeControl {
        initLog(FX_INSTALL_SCOPE_ACTIVITY_TAG)
        val control = FxScopeControl(this)
        control.initProvider()
        activity.contentView?.let {
            control.setContainerGroup(it)
        } ?: fxLog.e("install to Activity the Error,current contentView(R.id.content) = null!")
        return control
    }

    /** 插入到Fragment中 */
    fun toControl(fragment: Fragment): IFxScopeControl {
        initLog(FX_INSTALL_SCOPE_FRAGMENT_TAG)
        val rootView = fragment.view as? ViewGroup
        checkNotNull(rootView) {
            "your root view is null, init call timing is after onCreateView()!"
        }
        val control = FxScopeControl(this)
        control.initProvider()
        control.setContainerGroup(rootView)
        return control
    }

    /** 插入到ViewGroup中 */
    fun toControl(group: ViewGroup): IFxScopeControl {
        initLog(FX_INSTALL_SCOPE_VIEW_GROUP_TAG)
        val control = FxScopeControl(this)
        control.initProvider()
        control.setContainerGroup(group)
        return control
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()

        @JvmSynthetic
        inline fun build(obj: Builder.() -> Unit) = builder().apply(obj).build()
    }

    @FxBuilderDsl
    class Builder : FxBasisHelper.Builder<Builder, FxScopeHelper>() {
        override fun buildHelper(): FxScopeHelper = FxScopeHelper()
    }
}
