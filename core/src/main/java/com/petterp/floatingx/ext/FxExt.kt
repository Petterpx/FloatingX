package com.petterp.floatingx.ext

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.FrameLayout
import com.petterp.floatingx.FloatingX

/**
 * @Author petterp
 * @Date 2021/5/20-5:17 下午
 * @Email ShiyihuiCloud@163.com
 * @Function
 */

internal inline fun <reified T : Any> lazyLoad(
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.NONE,
    crossinline obj: () -> T
): Lazy<T> =
    lazy(mode) {
        obj()
    }

internal val topActivity: Activity?
    get() = FloatingX.iFxAppLifecycle?.topActivity

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

private const val CONFIG_X = "saveX"
private const val CONFIG_Y = "saveY"
private const val CONFIG_VERSION_CODE = "fx_save_version_code"
internal var SharedPreferences.x: Float
    get() = getFloat(CONFIG_X, 0f)
    set(value) = edit().putFloat(CONFIG_X, value).apply()

internal var SharedPreferences.y: Float
    get() = getFloat(CONFIG_Y, 0f)
    set(value) = edit().putFloat(CONFIG_Y, value).apply()

internal var SharedPreferences.versionCode: Int
    get() = getInt(CONFIG_VERSION_CODE, 0)
    set(value) = edit().putInt(CONFIG_VERSION_CODE, value).apply()

internal val SharedPreferences.hasConfig: Boolean
    get() = versionCode > 0
