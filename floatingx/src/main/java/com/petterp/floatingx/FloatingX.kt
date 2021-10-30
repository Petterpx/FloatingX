package com.petterp.floatingx

import android.annotation.SuppressLint
import android.app.Application
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.impl.control.FxAppControlImpl
import com.petterp.floatingx.impl.lifecycle.FxLifecycleCallbackImpl

/**
 * Single Control To Fx
 */
@SuppressLint("StaticFieldLeak")
object FloatingX {
    private var fxControl: FxAppControlImpl? = null
    internal var helper: AppHelper? = null
    internal var iFxAppLifecycleImpl: FxLifecycleCallbackImpl? = null

    /** dsl初始化 */
    fun init(obj: AppHelper.Builder.() -> Unit): FloatingX =
        init(AppHelper.builder().apply(obj).build())

    /** 悬浮窗配置信息 */
    @JvmStatic
    fun init(helper: AppHelper): FloatingX {
        this.helper = helper
        return this
    }

    @JvmStatic
    fun control(): FxAppControlImpl {
        if (fxControl == null) {
            initControl()
        }
        return fxControl!!
    }

    /** 调用此方法将直接关闭悬浮窗,保留配置信息helper
     * */
    @JvmStatic
    internal fun reset() {
        fxControl = null
        getConfigApplication().unregisterActivityLifecycleCallbacks(iFxAppLifecycleImpl)
        iFxAppLifecycleImpl = null
    }

    private fun initControl() {
        if (fxControl == null) {
            fxControl = FxAppControlImpl(config())
            initAppLifecycle()
            iFxAppLifecycleImpl?.appControl = fxControl
        }
    }

    private fun initAppLifecycle() {
        if (iFxAppLifecycleImpl == null) {
            val application = getConfigApplication()
            iFxAppLifecycleImpl = FxLifecycleCallbackImpl(config())
            iFxAppLifecycleImpl?.appControl = fxControl
            application.registerActivityLifecycleCallbacks(iFxAppLifecycleImpl)
        }
    }

    private fun getConfigApplication(): Application {
        return config().application
    }

    private fun config(): AppHelper =
        helper ?: throw NullPointerException("config==null!!!,FxConfig Cannot be null")
}
