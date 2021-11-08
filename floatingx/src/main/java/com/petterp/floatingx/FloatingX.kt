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
        // 如果用户未开启全局浮窗,那么此时初始化控制器意义页不大
        if (helper.enableFx) {
            initControl()
        }
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
        if (iFxAppLifecycleImpl == null) return
        getConfigApplication().unregisterActivityLifecycleCallbacks(iFxAppLifecycleImpl)
        iFxAppLifecycleImpl = null
    }

    private fun initControl() {
        if (fxControl == null) {
            fxControl = FxAppControlImpl(config())
            if (!config().enableFx) return
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
        helper
            ?: throw NullPointerException("helper==null!!!,AppHelper Cannot be null,Please check if init() is called.")
}
