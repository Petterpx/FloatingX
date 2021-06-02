package com.petterp.floatingx

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import androidx.annotation.MainThread
import com.petterp.floatingx.config.FxHelper
import com.petterp.floatingx.ext.FxDebug
import com.petterp.floatingx.impl.FxControlImpl
import com.petterp.floatingx.listener.FxLifecycleCallback
import com.petterp.floatingx.listener.IFxControl

/**
 * @Author petterp
 * @Date 2021/5/20-7:48 下午
 * @Email ShiyihuiCloud@163.com
 * @Function Single Control To Fx
 */
@SuppressLint("StaticFieldLeak")
object FloatingX {

    private var iFxAppLifecycle: FxLifecycleCallback? = null
    private var fxControl: IFxControl? = null
    private var helper: FxHelper? = null
    internal val topActivity: Activity
        get() = iFxAppLifecycle?.topActivity
            ?: throw NullPointerException("topActivity == null !,Have you ever called FloatingX.init()?")

    /** dsl初始化 */
    fun init(obj: FxHelper.Builder.() -> Unit): FloatingX {
        return init(FxHelper.builder(obj))
    }

    /** 悬浮窗配置信息 */
    @JvmStatic
    fun init(helper: FxHelper): FloatingX {
        this.helper = helper
        initControl()
        initAppLifecycle()
        return this
    }

    @JvmStatic
    fun isDebug(isDebug: Boolean = true): FloatingX {
        FxDebug.updateMode(isDebug)
        return this
    }

    /**
     * 显示悬浮窗,此方法禁止Application中调用
     * */
    @MainThread
    @JvmStatic
    fun show() {
        initControl()
        control().show()
    }

    @MainThread
    @JvmStatic
    fun show(activity: Activity) {
        initControl()
        control().show(activity)
    }

    @MainThread
    @JvmStatic
    fun dismiss() {
        fxControl?.dismiss()
    }

    @MainThread
    @JvmStatic
    fun hide() {
        fxControl?.hide()
    }

    /** 调用此方法将关闭悬浮窗,保留配置信息helper与AppLifecycle监听
     * 如果放弃配置信息与appLifecycle,再次调用show将导致无法获取栈顶Activity
     * */
    @JvmStatic
    fun cancel() {
        dismiss()
        fxControl = null
    }

    private fun initControl() {
        if (fxControl == null) {
            fxControl = FxControlImpl(config())
            iFxAppLifecycle?.control = fxControl
        }
    }

    private fun initAppLifecycle() {
        if (iFxAppLifecycle == null) {
            val application = getConfigApplication()
            iFxAppLifecycle = FxLifecycleCallback(config())
            iFxAppLifecycle?.control = fxControl
            application.registerActivityLifecycleCallbacks(iFxAppLifecycle)
        }
    }

    private fun getConfigApplication(): Application {
        if (config().context is Application)
            return config().context as Application
        else throw ClassCastException(
            "If you use the global floating window, FxConfig.context must be Application,otherwise the appLifecycle cannot be registered"
        )
    }

    private fun config(): FxHelper =
        helper ?: throw NullPointerException("config==null!!!,FxConfig Cannot be null")

    @JvmStatic
    fun control(): IFxControl =
        fxControl ?: throw NullPointerException("control==null!!!,IFloatingControl Cannot be null")
}
