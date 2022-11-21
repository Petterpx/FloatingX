package com.petterp.floatingx.impl.control

import android.app.Activity
import android.app.Application
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.listener.control.IFxScopeControl
import com.petterp.floatingx.util.FxScopeEnum
import com.petterp.floatingx.util.contentView

/** Fx普通View控制器 */
class FxScopeControl(private val helper: BasisHelper) :
    FxBasisControlImpl(helper),
    IFxScopeControl<IFxControl> {

    override fun show() {
        super.show()
        if (isShow()) return
        if (getManagerView() == null) initManagerView()
        getContainerGroup()?.addView(getManagerView())
        getManagerView()?.show()
    }

    override fun init(group: FrameLayout): IFxControl {
        helper.initLog(FxScopeEnum.VIEW_GROUP_SCOPE.tag)
        setContainerGroup(group)
        return this
    }

    override fun init(fragment: Fragment): IFxControl {
        helper.initLog(FxScopeEnum.FRAGMENT_SCOPE.tag)
        val rootView = fragment.view as? FrameLayout
        checkNotNull(rootView) {
            "Check if your root layout is FrameLayout, or if the init call timing is after onCreateView()!"
        }
        setContainerGroup(rootView)
        return this
    }

    override fun init(activity: Activity): IFxControl {
        helper.initLog(FxScopeEnum.ACTIVITY_SCOPE.tag)
        activity.contentView?.let {
            setContainerGroup(it)
        } ?: helper.fxLog?.e("install to Activity the Error,current contentView(R.id.content) = null!")
        return this
    }

    override fun updateView(view: View) {
        if (view.context is Application) {
            throw IllegalArgumentException("view == Application,Scope floating windows cannot use application-level views!")
        }
        super.updateView(view)
    }
}
