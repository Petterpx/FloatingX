package com.petterp.floatingx.impl

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.petterp.floatingx.assist.FxHelper
import com.petterp.floatingx.ext.lazyLoad
import com.petterp.floatingx.listener.IFxControl
import com.petterp.floatingx.listener.IFxControlBasis
import com.petterp.floatingx.view.FxViewHolder

/**
 * @Author petterp
 * @Date 2021/5/27-7:13 下午
 * @Email ShiyihuiCloud@163.com
 * @Function 单View使用时的封装
 * 目前存在一定使用细节不足,等待完善
 */

fun createFloatingX(obj: FxHelper.Builder.() -> Unit) =
    lazyLoad {
        FxControlToScopeImpl.builder(obj)
    }

fun createFloatingX(helper: FxHelper) =
    lazyLoad {
        FxControlToScopeImpl.builder(helper)
    }

class FxControlToScopeImpl private constructor() : DefaultLifecycleObserver, IFxControlBasis {

    private var fxHelper: FxHelper? = null
    private var controlImpl: IFxControl? = null

    private fun init(helper: FxHelper, fxControlImpl: FxControlImpl) {
        this.fxHelper = helper
        this.controlImpl = fxControlImpl
    }

    private fun initManagerView() {
        if (fxHelper?.context is Application)
            throw ClassCastException("Application Context is forbidden here, which will cause the hidden danger of memory leak!")
        controlImpl?.show(fxHelper?.context as Activity)
    }

    override fun show() {
        controlImpl?.getView()?.let {
            controlImpl?.show()
        } ?: initManagerView()
    }

    override fun hide() {
        controlImpl?.hide()
    }

    override fun dismiss() {
        controlImpl?.dismiss()
    }

    override fun getView(): View? = controlImpl?.getView()

    override fun isShowRunning(): Boolean = controlImpl?.isShowRunning() ?: false

    override fun updateParams(params: ViewGroup.LayoutParams) {
        controlImpl?.updateParams(params)
    }

    override fun updateView(obj: (FxViewHolder) -> Unit) {
        controlImpl?.updateView(obj)
    }

    override fun updateView(resource: Int) {
        controlImpl?.updateView(resource)
    }

    override fun setClickListener(time: Long, obj: (View) -> Unit) {
        controlImpl?.setClickListener(time, obj = obj)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        controlImpl?.dismiss()
        controlImpl = null
        fxHelper = null
        super.onDestroy(owner)
    }

    companion object {

        @JvmStatic
        fun builder(fxHelper: FxHelper) = FxControlToScopeImpl().apply {
            init(fxHelper, FxControlImpl(fxHelper))
        }

        fun builder(obj: FxHelper.Builder.() -> Unit) =
            FxControlToScopeImpl().apply {
                val config = FxHelper.builder(obj)
                init(config, FxControlImpl(config))
            }

        // TODO: 2021/5/28 未实现 
        // 深拷贝全局悬浮窗的配置信息,避免重复配置 
//        fun builderToCopyFx() = FxControlToScopeImpl().apply {
//            FloatingX.config()
//        }
    }
}
