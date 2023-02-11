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
    private var fxLifecycleCallback: FxLifecycleCallbackImpl? = null
    private var fxs = mutableMapOf<String, FxAppControlImpl>()

    /**
     * 初始化全局悬浮窗,以dsl方式
     *
     * 该方法已弃用，请使用 [create(obj: AppHelper.Builder.() -> Unit)]
     */
    @Deprecated(
        "In order to be compatible with multi-floating windows,Please Use init() instead.",
        ReplaceWith("", "")
    )
    @JvmSynthetic
    fun init(obj: AppHelper.Builder.() -> Unit) = create(obj)

    /**
     * 初始化全局悬浮窗
     *
     * 该方法已弃用，请使用 [create(helper: AppHelper)]
     */
    @Deprecated(
        "In order to be compatible with multi-floating windows,Please Use init() instead.",
        ReplaceWith("FloatingX.create(helper)", "com.petterp.floatingx.FloatingX.create")
    )
    @JvmStatic
    fun init(helper: AppHelper): IFxAppControl = create(helper)

    /**
     * 创建一个新的全局浮窗,以dsl方式
     *
     * 方法含义见 [create(helper: AppHelper)]
     */
    @JvmSynthetic
    fun create(obj: AppHelper.Builder.() -> Unit) = create(AppHelper.builder().apply(obj).build())

    /**
     * 创建一个新的全局浮窗
     *
     * 如果你需要多个浮窗，记得调用AppHelper.setTag()方法，设置浮窗tag，如果没有调用setTag()方法，则默认tag为[FX_DEFAULT_TAG]
     *
     * 多次调用create()时，如果当前tag对应的浮窗存在，则会取消上一个浮窗，重新创建一个新的浮窗
     */
    @JvmStatic
    fun create(helper: AppHelper): IFxAppControl {
        fxs[helper.tag]?.cancel()
        val fxAppControlImpl = FxAppControlImpl(helper, FxProxyLifecycleCallBackImpl())
        fxs[helper.tag] = fxAppControlImpl
        return fxAppControlImpl
    }

    /**
     * 全局浮窗控制器
     *
     * @param tag 浮窗tag,默认是第一个浮窗
     */
    @JvmStatic
    @JvmOverloads
    fun control(tag: String = FX_DEFAULT_TAG): IFxAppControl {
        return getTagFxControl(tag)
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
            ?: throw NullPointerException("fxs[$tag]==null!,Please check if create() is called.")
    }
}
