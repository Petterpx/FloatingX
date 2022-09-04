package com.petterp.floatingx

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.impl.control.FxAppControlImpl
import com.petterp.floatingx.impl.lifecycle.FxLifecycleCallbackImpl
import com.petterp.floatingx.listener.control.IFxAppControl

/**
 * Single Control To Fx
 */
@SuppressLint("StaticFieldLeak")
object FloatingX {
    private lateinit var context: Context
    private var helper: AppHelper? = null
    private var fxControl: FxAppControlImpl? = null
    private var fxLifecycleCallback: FxLifecycleCallbackImpl? = null

    /** 悬浮窗初始化 */
    @JvmSynthetic
    fun init(obj: AppHelper.Builder.() -> Unit) =
        init(AppHelper.builder().apply(obj).build())

    @JvmStatic
    fun init(helper: AppHelper) {
        this.helper = helper
        if (helper.enableFx) initControl()
    }

    /** 浮窗控制器 */
    @JvmStatic
    fun control(): IFxAppControl {
        initControl()
        return fxControl!!
    }

    private fun initControl() {
        if (fxControl == null) {
            fxControl = FxAppControlImpl(config())
        }
    }

    private fun config(): AppHelper =
        helper
            ?: throw NullPointerException("helper==null!!!,AppHelper Cannot be null,Please check if init() is called.")

    @JvmSynthetic
    internal fun initAppLifecycle(context: Context) {
        this.context = context
        if (fxLifecycleCallback == null) {
            fxLifecycleCallback = FxLifecycleCallbackImpl()
        }
        (context as Application).apply {
            unregisterActivityLifecycleCallbacks(fxLifecycleCallback)
            registerActivityLifecycleCallbacks(fxLifecycleCallback)
        }
    }

    @JvmSynthetic
    internal fun getHelper(): AppHelper? = helper

    @JvmSynthetic
    internal fun getContext(): Context = context

    @JvmSynthetic
    internal fun getControl(): FxAppControlImpl? = fxControl

    @JvmSynthetic
    internal fun reset() {
        fxControl = null
    }
}
