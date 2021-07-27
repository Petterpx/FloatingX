package com.petterp.floatingx.util

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.FxHelper
import com.petterp.floatingx.impl.FxLocalControlImpl

/**
 * @Author petterp
 * @Date 2021/5/20-5:17 下午
 * @Email ShiyihuiCloud@163.com
 * @Function Fx的一些kotlin扩展
 */

fun createFloatingX(obj: FxHelper.Builder.() -> Unit) =
    lazyLoad {
        FxLocalControlImpl.builder(obj)
    }

fun createFloatingX(helper: FxHelper) =
    lazyLoad {
        FxLocalControlImpl.builder(helper)
    }

internal inline fun <reified T : Any> lazyLoad(
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.NONE,
    crossinline obj: () -> T
): Lazy<T> =
    lazy(mode) {
        obj()
    }

internal val topActivity: Activity?
    get() = FloatingX.iFxAppLifecycleImpl?.topActivity?.get()

internal val appContext: Context
    get() = FloatingX.helper?.context
        ?: throw NullPointerException("appContext == null !,Did you initialize the context?")

internal val Activity.fxParentView: FrameLayout?
    get() = try {
        window.decorView as FrameLayout
    } catch (e: Exception) {
        e.printStackTrace()
        FxDebug.e("rootView -> Null")
        null
    }
