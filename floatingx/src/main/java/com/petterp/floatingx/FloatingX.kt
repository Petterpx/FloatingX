package com.petterp.floatingx

import android.annotation.SuppressLint
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.imp.app.FxAppControlImp
import com.petterp.floatingx.imp.system.FxSystemControlImp
import com.petterp.floatingx.listener.control.IFxAppControl
import com.petterp.floatingx.listener.control.IFxConfigControl
import com.petterp.floatingx.util.FX_DEFAULT_TAG

/** Single Control To Fx */
@SuppressLint("StaticFieldLeak")
object FloatingX {
    private const val FX_DEFAULT_INITIAL_CAPACITY = 3

    @JvmSynthetic
    internal val fxs = HashMap<String, IFxAppControl>(FX_DEFAULT_INITIAL_CAPACITY)

    /**
     * 安装一个新的全局浮窗,以dsl方式
     *
     * 方法含义见 [install(helper: AppHelper)]
     */
    @JvmSynthetic
    inline fun install(obj: FxAppHelper.Builder.() -> Unit) =
        install(FxAppHelper.builder().apply(obj).build())

    /**
     * 安装一个新的全局浮窗
     *
     * 如果你需要多个浮窗，记得调用AppHelper.setTag()方法，设置浮窗tag，如果没有调用setTag()方法，则默认tag为[FX_DEFAULT_TAG]
     *
     * 多次调用install()时，如果当前tag对应的浮窗存在，则会取消上一个浮窗，重新安装一个新的浮窗
     *
     */
    @JvmStatic
    fun install(helper: FxAppHelper): IFxAppControl {
        fxs[helper.tag]?.cancel()
        val fxAppControlImp = if (helper.scope.hasPermission) {
            FxSystemControlImp(helper)
        } else {
            FxAppControlImp(helper)
        }
        fxAppControlImp.initProvider()
        fxs[helper.tag] = fxAppControlImp
        return fxAppControlImp
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

    /**
     * 获取浮窗控制器的key合集，之所以这样，是为了尽可能避免 map 操作异常
     * cancel() 时还包括了动画的调度，故只对外暴漏keys
     * */
    @JvmStatic
    fun getAllControlTags(): List<String> {
        return fxs.keys.toList()
    }

    @JvmSynthetic
    internal fun uninstall(tag: String, control: IFxAppControl) {
        if (!fxs.values.contains(control)) return
        fxs.remove(tag)
    }

    /** 卸载所有全局浮窗,后续使用需要重新install */
    @JvmStatic
    fun uninstallAll() {
        if (fxs.isEmpty()) return
        getAllControlTags().forEach {
            fxs[it]?.cancel()
        }
    }

    private fun getTagFxControl(tag: String): IFxAppControl {
        val control = fxs[tag]
        checkNotNull(control) { "fxs[$tag]==null!,Please check if FloatingX.install() or AppHelper.setTag() is called." }
        return control
    }
}
