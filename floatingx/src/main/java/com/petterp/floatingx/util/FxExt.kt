package com.petterp.floatingx.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.petterp.floatingx.assist.helper.ScopeHelper
import com.petterp.floatingx.listener.control.IFxControl

/**
 * 创建一个fx,自行初始化并控制插入位置
 *
 * val builder by createFx {
 *
 * setLayout(R.layout.item_floating) setEnableScrollOutsideScreen(false)
 * setAnimationImpl(FxAnimationImpl())
 * build().toControl().init(this@MainActivity)
 *
 * }
 */
@JvmSynthetic
inline fun <T> createFx(crossinline obj: ScopeHelper.Builder.() -> T) =
    lazy(LazyThreadSafetyMode.NONE) {
        ScopeHelper.Builder().run(obj)
    }

/** 创建一个fx,内部自行决定显示位置 */
@JvmSynthetic
inline fun createFx(
    crossinline initControlObj: (ScopeHelper.() -> IFxControl),
    crossinline builderObj: ScopeHelper.Builder.() -> Unit
) =
    lazy(LazyThreadSafetyMode.NONE) {
        ScopeHelper.build(builderObj).run(initControlObj)
    }

/** 快捷构建-在activity下创建一个fx */
@JvmSynthetic
inline fun activityToFx(activity: Activity, crossinline obj: ScopeHelper.Builder.() -> Unit) =
    lazy(LazyThreadSafetyMode.NONE) {
        ScopeHelper.build(obj).toControl(activity)
    }

/** 快捷构建-在fragment对应的view中显示一个fx */
@JvmSynthetic
inline fun fragmentToFx(fragment: Fragment, crossinline obj: ScopeHelper.Builder.() -> Unit) =
    lazy(LazyThreadSafetyMode.NONE) {
        ScopeHelper.build(obj).toControl(fragment)
    }

/** 快捷构建-在activity中创建一个view作用域的fx */
@JvmSynthetic
inline fun viewToFx(
    @IdRes id: Int,
    activity: Activity,
    crossinline obj: ScopeHelper.Builder.() -> Unit
) = lazy(LazyThreadSafetyMode.NONE) {
    val parent = activity.findViewById<ViewGroup>(id)
    ScopeHelper.build(obj).toControl(parent)
}

/** 快捷构建-在fragment中创建一个view作用域fx */
@JvmSynthetic
inline fun viewToFx(
    @IdRes id: Int,
    fragment: Fragment,
    crossinline obj: ScopeHelper.Builder.() -> Unit
) = lazy(LazyThreadSafetyMode.NONE) {
    val parent = fragment.requireView().findViewById<ViewGroup>(id)
    ScopeHelper.build(obj).toControl(parent)
}

@JvmSynthetic
internal inline fun <reified T : Any> lazyLoad(
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.NONE,
    crossinline obj: () -> T
): Lazy<T> =
    lazy(mode) {
        obj()
    }

@JvmSynthetic
internal fun Float.coerceInFx(min: Float, max: Float): Float {
    if (this < min) return min
    if (this > max) return max
    return this
}

@JvmSynthetic
internal fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> {
            this
        }
        is ContextWrapper -> {
            baseContext.findActivity()
        }
        else -> {
            null
        }
    }
}
