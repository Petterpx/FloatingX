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

/** Single Control To Fx */
@SuppressLint("StaticFieldLeak")
object FloatingX {
    private lateinit var context: Context
    private const val FX_FIRST_ONE = "FX_FIRST_ONE"
    private var fxLifecycleCallback: FxLifecycleCallbackImpl? = null
    private var fxs = mutableMapOf<String, FxAppControlImpl>()

    /** 悬浮窗初始化 */
    @Deprecated(
        "In order to be compatible with multi-floating windows,Please Use init() instead.",
        ReplaceWith("", "")
    )
    @JvmSynthetic
    fun init(obj: AppHelper.Builder.() -> Unit) = create(obj)

    @Deprecated(
        "In order to be compatible with multi-floating windows,Please Use init() instead.",
        ReplaceWith("FloatingX.create(helper)", "com.petterp.floatingx.FloatingX.create")
    )
    @JvmStatic
    fun init(helper: AppHelper): IFxAppControl = create(helper)

    @JvmSynthetic
    fun create(obj: AppHelper.Builder.() -> Unit) = create(AppHelper.builder().apply(obj).build())

    @JvmStatic
    fun create(helper: AppHelper): IFxAppControl {
        // 虽然我们map中可以保存 [""]，但是这并不是好的做法
        val tag = helper.tag.ifEmpty {
            FX_FIRST_ONE
        }
        fxs[tag]?.cancel()
        val fxAppControlImpl = FxAppControlImpl(helper, FxProxyLifecycleCallBackImpl())
        fxs[tag] = fxAppControlImpl
        return fxAppControlImpl
    }

    /** 浮窗控制器 */
    @JvmStatic
    @JvmOverloads
    fun control(tag: String = FX_FIRST_ONE): IFxAppControl {
        return getTagFxControl(tag)
    }

    /** 浮窗配置控制器 */
    @JvmStatic
    @JvmOverloads
    fun configControl(tag: String = FX_FIRST_ONE): IFxConfigControl {
        return getTagFxControl(tag).configControl
    }

    /** 关闭所有浮窗 */
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
