package com.petterp.floatingx.util

import android.app.Activity
import android.widget.FrameLayout
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.helper.BaseHelper
import com.petterp.floatingx.impl.control.FxBasisControlImpl

/**
 * @Author petterp
 * @Date 2021/5/20-5:17 下午
 * @Email ShiyihuiCloud@163.com
 * @Function Fx的一些kotlin扩展
 */

fun createFloatingX(helper: BaseHelper) =
    lazyLoad {
        FxBasisControlImpl(helper)
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

internal val Activity.decorView: FrameLayout?
    get() = try {
        window.decorView as FrameLayout
    } catch (e: Exception) {
        e.printStackTrace()
        FxDebug.e("rootView -> Null")
        null
    }
