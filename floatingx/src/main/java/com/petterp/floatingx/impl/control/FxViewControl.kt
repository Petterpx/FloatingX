package com.petterp.floatingx.impl.control

import android.app.Activity
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.petterp.floatingx.assist.helper.BaseHelper
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.listener.control.IFxScopeControl
import com.petterp.floatingx.util.contentView
import java.lang.ref.WeakReference

/**  Fx普通View控制器 */
class FxViewControl(helper: BaseHelper) :
    FxBasisControlImpl(helper),
    IFxScopeControl<IFxControl> {

    override fun show() {
        if (isShow()) return
        getContainer()?.addView(getManagerView())
        getManagerView()?.show()
    }

    override fun init(viewGroup: ViewGroup): IFxControl {
        mContainer = WeakReference(viewGroup)
        initManagerView()
        return this
    }

    override fun init(fragment: Fragment): IFxControl {
        val parent = fragment.requireView() as ViewGroup
        init(parent)
        return this
    }

    override fun init(activity: Activity): IFxControl {
        activity.contentView?.let {
            init(it)
        }
        return this
    }
}
