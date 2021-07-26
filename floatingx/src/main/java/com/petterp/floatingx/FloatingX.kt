package com.petterp.floatingx

import android.annotation.SuppressLint
import android.app.Application
import com.petterp.floatingx.assist.FxHelper
import com.petterp.floatingx.impl.FxAppControlImpl
import com.petterp.floatingx.impl.FxLifecycleCallbackImpl
import com.petterp.floatingx.listener.IFxAppControl

/**
 * @Author petterp
 * @Date 2021/5/20-7:48 下午
 * @Email ShiyihuiCloud@163.com
 * @Function Single Control To Fx
 */
@SuppressLint("StaticFieldLeak")
object FloatingX {
    private var fxControl: FxAppControlImpl? = null
    internal var helper: FxHelper? = null
    internal var iFxAppLifecycleImpl: FxLifecycleCallbackImpl? = null

    /** dsl初始化 */
    fun init(obj: FxHelper.Builder.() -> Unit) =
        init(FxHelper.builder(obj))

    /** 悬浮窗配置信息 */
    @JvmStatic
    fun init(helper: FxHelper): IFxAppControl {
        this.helper = helper
        return control()
    }

    @JvmStatic
    fun control(): IFxAppControl {
        if (fxControl == null) {
            initControl()
        }
        return fxControl!!
    }

    /** 清除历史坐标信息，如果开启了历史存储 */
    @JvmStatic
    fun clearConfig() {
        helper?.iFxConfigStorage?.clear()
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
        if (config().context is Application)
            return config().context as Application
        else throw ClassCastException(
            "If you use the global floating window, FxConfig.context must" +
                " be Application,otherwise the appLifecycle cannot be registered"
        )
    }

    private fun config(): FxHelper =
        helper ?: throw NullPointerException("config==null!!!,FxConfig Cannot be null")
}
