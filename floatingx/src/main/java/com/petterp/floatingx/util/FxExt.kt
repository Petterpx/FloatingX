package com.petterp.floatingx.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.petterp.floatingx.assist.helper.FxScopeHelper
import java.lang.Exception

@JvmSynthetic
internal const val FX_GRAVITY_TOP = 0x00000001

@JvmSynthetic
internal const val FX_GRAVITY_CENTER = 0x00000002

@JvmSynthetic
internal const val FX_GRAVITY_BOTTOM = 0x00000003

internal const val TOUCH_CLICK_OFFSET = 2F
internal const val TOUCH_TIME_THRESHOLD = 150L
internal const val INVALID_TOUCH_ID = -1
internal const val INVALID_LAYOUT_ID = 0
internal const val INVALID_TOUCH_IDX = -1
internal const val DEFAULT_MOVE_ANIMATOR_DURATION = 200L
internal val HANDLER = Handler(Looper.getMainLooper())

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
inline fun <T> createFx(crossinline obj: FxScopeHelper.Builder.() -> T) =
    lazy(LazyThreadSafetyMode.NONE) {
        FxScopeHelper.Builder().run(obj)
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

internal fun Float.withIn(min: Number, max: Number): Boolean {
    return this in min.toFloat()..max.toFloat()
}

internal val MotionEvent.pointerId: Int
    get() = try {
        getPointerId(actionIndex)
    } catch (_: Exception) {
        INVALID_TOUCH_ID
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
