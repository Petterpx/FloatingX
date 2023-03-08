package com.petterp.floatingx

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.impl.control.FxAppControlImpl
import com.petterp.floatingx.impl.lifecycle.FxLifecycleCallbackImpl
import com.petterp.floatingx.impl.lifecycle.FxProxyLifecycleCallBackImpl
import com.petterp.floatingx.listener.control.IFxAppControl
import com.petterp.floatingx.listener.control.IFxConfigControl

/** Single Control To Fx */
@SuppressLint("StaticFieldLeak")
object FloatingX {
    private lateinit var context: Application
    private const val FX_DEFAULT_INITIAL_CAPACITY = 3
    private var fxs = HashMap<String, FxAppControlImpl>(FX_DEFAULT_INITIAL_CAPACITY)
    private var fxLifecycleCallback: FxLifecycleCallbackImpl? = null

    @JvmSynthetic
    internal const val FX_DEFAULT_TAG = "FX_DEFAULT_TAG"

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
    inline fun init(obj: AppHelper.Builder.() -> Unit) = install(obj)

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
    inline fun install(obj: AppHelper.Builder.() -> Unit) =
        install(AppHelper.builder().apply(obj).build())

    /**
     * 安装一个新的全局浮窗
     *
     * 如果你需要多个浮窗，记得调用AppHelper.setTag()方法，设置浮窗tag，如果没有调用setTag()方法，则默认tag为[FX_DEFAULT_TAG]
     *
     * 多次调用install()时，如果当前tag对应的浮窗存在，则会取消上一个浮窗，重新安装一个新的浮窗
     */
    @JvmStatic
    fun install(helper: AppHelper): IFxAppControl {
        if (fxs.isNotEmpty()) fxs[helper.tag]?.cancel()
        val fxAppControlImpl = FxAppControlImpl(helper, FxProxyLifecycleCallBackImpl())
        fxs[helper.tag] = fxAppControlImpl
        if (helper.enableFx) checkAppLifecycleInstall()
        return fxAppControlImpl
    }

    /**
     * 全局浮窗操作控制器
     *
     * @param tag 浮窗tag,默认是 [FX_DEFAULT_TAG]
     */
    @JvmStatic
    @JvmOverloads
    fun control(tag: String = FX_DEFAULT_TAG): IFxAppControl {
        return getTagFxControl(tag)
    }

    /**
     * 获得全局浮窗操作控制器(可null)
     *
     * @param tag 浮窗tag,默认是 [FX_DEFAULT_TAG]
     */
    @JvmStatic
    @JvmOverloads
    fun controlOrNull(tag: String = FX_DEFAULT_TAG): IFxAppControl? {
        return fxs[tag]
    }

    /**
     * 全局浮窗配置控制器
     *
     * @param tag 浮窗tag,默认是 [FX_DEFAULT_TAG]
     */
    @JvmStatic
    @JvmOverloads
    fun configControl(tag: String = FX_DEFAULT_TAG): IFxConfigControl {
        return getTagFxControl(tag).configControl
    }

    /**
     * 全局浮窗配置控制器(可null)
     *
     * @param tag 浮窗tag,默认是 [FX_DEFAULT_TAG]
     */
    @JvmStatic
    @JvmOverloads
    fun configControlOrNull(tag: String = FX_DEFAULT_TAG): IFxConfigControl? {
        return fxs[tag]?.configControl
    }

    /** 判断该tag对应的全局浮窗是否存在
     * @param tag 浮窗tag,默认是 [FX_DEFAULT_TAG]
     * */
    @JvmStatic
    @JvmOverloads
    fun isInstalled(tag: String = FX_DEFAULT_TAG): Boolean {
        return fxs[tag] != null
    }

    /** 卸载所有全局浮窗,后续使用需要重新install */
    @JvmStatic
    fun uninstallAll() {
        if (fxs.isEmpty()) return
        // 这里需要避免 ConcurrentModificationException
        val keys = fxs.keys.toList()
        keys.forEach {
            fxs[it]?.cancel()
        }
    }

    @JvmSynthetic
    internal fun initContext(context: Context) {
        this.context = context as Application
    }

    @JvmSynthetic
    internal fun getFxList(): Map<String, FxAppControlImpl> = fxs

    @JvmSynthetic
    internal fun getContext(): Context = context

    @JvmSynthetic
    internal fun uninstall(tag: String, control: FxAppControlImpl) {
        if (fxs.values.contains(control)) fxs.remove(tag)
        // 如果全局浮窗为null，自动清空配置
        if (fxs.isEmpty()) {
            release()
        }
    }

    /**
     * 检查AppLifecycle是否安装
     *
     * @param activity 初始化时的activity
     */
    @JvmSynthetic
    internal fun checkAppLifecycleInstall(activity: Activity? = null) {
        if (fxLifecycleCallback != null) return
        FxLifecycleCallbackImpl.updateTopActivity(activity)
        fxLifecycleCallback = FxLifecycleCallbackImpl()
        context.registerActivityLifecycleCallbacks(fxLifecycleCallback)
    }

    private fun getTagFxControl(tag: String): FxAppControlImpl {
        val errorMessage =
            "fxs[$tag]==null!,Please check if FloatingX.install() or AppHelper.setTag() is called."
        return fxs[tag] ?: throw NullPointerException(errorMessage)
    }

    private fun release() {
        if (fxLifecycleCallback == null && FxLifecycleCallbackImpl.topActivity == null) return
        context.unregisterActivityLifecycleCallbacks(fxLifecycleCallback)
        FxLifecycleCallbackImpl.releaseTopActivity()
        fxLifecycleCallback = null
    }
}
