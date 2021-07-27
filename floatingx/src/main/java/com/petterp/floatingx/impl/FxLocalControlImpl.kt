package com.petterp.floatingx.impl

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.petterp.floatingx.assist.FxHelper
import com.petterp.floatingx.listener.IFxAppControl
import com.petterp.floatingx.listener.IFxControlBasis
import com.petterp.floatingx.view.FxViewHolder

/**
 * @Author petterp
 * @Date 2021/5/27-7:13 下午
 * @Email ShiyihuiCloud@163.com
 * @Function 单View使用时的封装
 * 目前存在一定使用细节不足,等待完善
 */

class FxLocalControlImpl private constructor() : DefaultLifecycleObserver, IFxControlBasis {

    private var fxHelper: FxHelper? = null
    private var appControlImpl: IFxAppControl? = null

    private fun init(helper: FxHelper, fxControlImpl: FxAppControlImpl) {
        this.fxHelper = helper
        this.appControlImpl = fxControlImpl
    }

    private fun initManagerView() {
        if (fxHelper?.context is Application)
            throw ClassCastException("Application Context is forbidden here, which will cause the hidden danger of memory leak!")
        appControlImpl?.show(fxHelper?.context as Activity)
    }

    override fun show(isAnimation: Boolean) {
        appControlImpl?.getManagerView()?.let {
            appControlImpl?.show()
        } ?: initManagerView()
    }

    override fun hide(isAnimation: Boolean) {
        appControlImpl?.hide()
    }

    override fun cancel(isAnimation: Boolean) {
    }

    override fun getManagerView(): View? = appControlImpl?.getManagerView()
    override fun getView(): View? = appControlImpl?.getView()

    override fun isShowRunning(): Boolean = appControlImpl?.isShowRunning() ?: false

    override fun updateParams(params: ViewGroup.LayoutParams) {
        appControlImpl?.updateParams(params)
    }

    override fun updateView(obj: (FxViewHolder) -> Unit) {
        appControlImpl?.updateView(obj)
    }

    override fun updateView(resource: Int) {
        appControlImpl?.updateView(resource)
    }

    override fun setClickListener(time: Long, obj: (View) -> Unit) {
        appControlImpl?.setClickListener(time, obj = obj)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        appControlImpl?.hide()
        appControlImpl = null
        fxHelper = null
        super.onDestroy(owner)
    }

    companion object {

        @JvmStatic
        fun builder(fxHelper: FxHelper) = FxLocalControlImpl().apply {
            init(fxHelper, FxAppControlImpl(fxHelper))
        }

        fun builder(obj: FxHelper.Builder.() -> Unit) =
            FxLocalControlImpl().apply {
                val config = FxHelper.builder(obj)
                init(config, FxAppControlImpl(config))
            }

        // TODO: 2021/5/28 未实现 
        // 深拷贝全局悬浮窗的配置信息,避免重复配置 
//        fun builderToCopyFx() = FxControlToScopeImpl().apply {
//            FloatingX.config()
//        }
    }
}
