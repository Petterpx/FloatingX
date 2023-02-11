package com.petterp.floatingx

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.impl.control.FxAppControlImpl
import com.petterp.floatingx.impl.lifecycle.FxLifecycleCallbackImpl
import com.petterp.floatingx.impl.lifecycle.FxProxyLifecycleCallBackImpl
import com.petterp.floatingx.listener.control.IFxAppControl
import com.petterp.floatingx.listener.control.IFxConfigControl
import com.petterp.floatingx.util.FX_DEFAULT_TAG

/** Single Control To Fx */
@SuppressLint("StaticFieldLeak")
object FloatingX {
    private lateinit var context: Context
    private var fxs = HashMap<String, FxAppControlImpl>(3)
    private var fxLifecycleCallback: FxLifecycleCallbackImpl? = null

    /**
     * 初始化全局悬浮窗,以dsl方式
     *
     * 该方法已弃用，请使用 [install(obj: AppHelper.Builder.() -> Unit)]
     */
    @Deprecated(
        "In order to be compatible with multi-floating windows,Please Use init() instead.",
        ReplaceWith("", "")
    )
    @JvmSynthetic
    fun init(obj: AppHelper.Builder.() -> Unit) = install(obj)

    /**
     * 初始化全局悬浮窗
     *
     * 该方法已弃用，请使用 [install(helper: AppHelper)]
     */
    @Deprecated(
        "In order to be compatible with multi-floating windows,Please Use init() instead.",
        ReplaceWith("FloatingX.install(helper)", "com.petterp.floatingx.FloatingX.install")
    )
    @JvmStatic
    fun init(helper: AppHelper): IFxAppControl = install(helper)

    /**
     * 安装一个新的全局浮窗,以dsl方式
     *
     * 方法含义见 [install(helper: AppHelper)]
     */
    @JvmSynthetic
    fun install(obj: AppHelper.Builder.() -> Unit) = install(AppHelper.builder().apply(obj).build())

    /**
     * 安装一个新的全局浮窗
     *
     * 如果你需要多个浮窗，记得调用AppHelper.setTag()方法，设置浮窗tag，如果没有调用setTag()方法，则默认tag为[FX_DEFAULT_TAG]
     *
     * 多次调用install()时，如果当前tag对应的浮窗存在，则会取消上一个浮窗，重新安装一个新的浮窗
     */
    @JvmStatic
    fun install(helper: AppHelper): IFxAppControl {
        fxs[helper.tag]?.cancel()
        val fxAppControlImpl = FxAppControlImpl(helper, FxProxyLifecycleCallBackImpl())
        fxs[helper.tag] = fxAppControlImpl
        return fxAppControlImpl
    }

    /**
     * 全局浮窗操作控制器
     *
     * @param tag 浮窗tag,默认是第一个浮窗
     */
    @JvmStatic
    @JvmOverloads
    fun control(tag: String = FX_DEFAULT_TAG): IFxAppControl {
        return getTagFxControl(tag)
    }

    /**
     * 获得全局浮窗操作控制器(可null)
     *
     * @param tag 浮窗tag,默认是第一个浮窗
     */
    @JvmStatic
    @JvmOverloads
    fun controlOrNull(tag: String = FX_DEFAULT_TAG): IFxAppControl? {
        return fxs[tag]
    }

    /**
     * 全局浮窗配置控制器
     *
     * @param tag 浮窗tag,默认是第一个浮窗
     */
    @JvmStatic
    @JvmOverloads
    fun configControl(tag: String = FX_DEFAULT_TAG): IFxConfigControl {
        return getTagFxControl(tag).configControl
    }

    /**
     * 全局浮窗配置控制器(可null)
     *
     * @param tag 浮窗tag,默认是第一个浮窗
     */
    @JvmStatic
    @JvmOverloads
    fun configControlOrNull(tag: String = FX_DEFAULT_TAG): IFxConfigControl? {
        return fxs[tag]?.configControl
    }

    /** 判断该tag对应的全局浮窗是否存在 */
    fun isInstalled(tag: String): Boolean {
        return fxs[tag] != null
    }

    /** 关闭当前所有全局浮窗 */
    @JvmStatic
    fun cancelAll() {
        if (fxs.isEmpty()) return
        for (fx in fxs.values) {
            fx.cancel()
        }
    }

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
    internal fun getFxList(): Map<String, FxAppControlImpl> = fxs

    @JvmSynthetic
    internal fun getContext(): Context = context

    @JvmSynthetic
    internal fun reset(tag: String) {
        fxs.remove(tag)
    }

    private fun getTagFxControl(tag: String): FxAppControlImpl {
        return fxs[tag]
            ?: throw NullPointerException("fxs[$tag]==null!,Please check if install() is called.")
    }
}
