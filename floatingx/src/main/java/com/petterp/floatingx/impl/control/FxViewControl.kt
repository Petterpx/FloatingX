package com.petterp.floatingx.impl.control

import android.app.Activity
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.petterp.floatingx.assist.helper.BaseHelper
import com.petterp.floatingx.listener.IFxScopeControl
import com.petterp.floatingx.util.decorView
import java.lang.ref.WeakReference

/**
 * @Author petterp
 * @Date 2021/8/5-11:15 PM
 * @Email ShiyihuiCloud@163.com
 * @Function Fx普通View控制器
 */
class FxViewControl(helper: BaseHelper) :
    FxBasisControlImpl(helper),
    IFxScopeControl<FxViewControl> {

    override fun show() {
        if (isShow()) return
        getContainer()?.addView(getManagerView())
        getManagerView()?.show()
    }

    override fun init(viewGroup: ViewGroup): FxViewControl {
        mContainer = WeakReference(viewGroup)
        initManagerView()
        return this
    }

    override fun init(fragment: Fragment): FxViewControl {
        val parent = fragment.requireView() as ViewGroup
        init(parent)
        return this
    }

    override fun init(activity: Activity): FxViewControl {
        activity.decorView?.let {
            init(it)
        }
        return this
    }
}
