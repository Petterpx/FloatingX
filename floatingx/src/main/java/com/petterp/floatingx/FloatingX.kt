package com.petterp.floatingx

import android.annotation.SuppressLint
import android.content.Context
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.impl.control.FxAppControlImpl
import com.petterp.floatingx.listener.control.IFxAppControl

/**
 * Single Control To Fx
 */
@SuppressLint("StaticFieldLeak")
object FloatingX {
    internal lateinit var context: Context
    internal var helper: AppHelper? = null
    internal var fxControl: FxAppControlImpl? = null

    /** 悬浮窗初始化 */
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

    internal fun reset() {
        fxControl = null
    }

    private fun initControl() {
        if (fxControl == null) {
            fxControl = FxAppControlImpl(config())
        }
    }

    private fun config(): AppHelper =
        helper
            ?: throw NullPointerException("helper==null!!!,AppHelper Cannot be null,Please check if init() is called.")
}
