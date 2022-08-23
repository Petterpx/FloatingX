package com.petterp.floatingx.impl.control

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.petterp.floatingx.assist.helper.BasisHelper
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.listener.control.IFxScopeControl
import com.petterp.floatingx.util.FxScopeEnum
import com.petterp.floatingx.util.contentView
import java.lang.ref.WeakReference

/** Fx普通View控制器 */
class FxScopeControl(private val helper: BasisHelper) :
    FxBasisControlImpl(helper),
    IFxScopeControl<IFxControl> {

    override fun show() {
        super.show()
        if (isShow()) return
        if (getManagerView() == null) initManagerView()
        getContainer()?.addView(getManagerView())
        getManagerView()?.show()
    }

    override fun init(viewGroup: ViewGroup): IFxControl {
        helper.initLog(FxScopeEnum.VIEW_GROUP_SCOPE.tag)
        mContainer = WeakReference(viewGroup)
        return this
    }

    override fun init(fragment: Fragment): IFxControl {
        helper.initLog(FxScopeEnum.FRAGMENT_SCOPE.tag)
        val parent = fragment.requireView() as ViewGroup
        init(parent)
        return this
    }

    override fun init(activity: Activity): IFxControl {
        helper.initLog(FxScopeEnum.ACTIVITY_SCOPE.tag)
        activity.contentView?.let {
            init(it)
        }
        return this
    }

    override fun updateManagerView(view: View) {
        if (view.context is Application) {
            throw IllegalArgumentException("view == Application,Scope floating windows cannot use application-level views!")
        }
        super.updateManagerView(view)
    }
}
