@file:JvmName("_FxExt")

package com.petterp.floatingx.util

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.imp.FxAppLifecycleProvider
import com.petterp.floatingx.listener.control.IFxControl
import com.petterp.floatingx.view.FxBasicContainerView
import java.lang.Exception

internal const val FX_GRAVITY_TOP = 0x00000001

internal const val FX_GRAVITY_CENTER = 0x00000002

internal const val FX_GRAVITY_BOTTOM = 0x00000003

internal const val INVALID_TOUCH_ID = -1
internal const val INVALID_LAYOUT_ID = 0
internal const val INVALID_TOUCH_IDX = -1
internal const val TOUCH_TIME_THRESHOLD = 150L
internal const val TOUCH_CLICK_LONG_TIME = 500L
internal const val DEFAULT_MOVE_ANIMATOR_DURATION = 200L
internal const val FX_DEFAULT_TAG = "FX_DEFAULT_TAG"

internal const val FX_INSTALL_SCOPE_APP_TAG = "app"
internal const val FX_INSTALL_SCOPE_SYSTEM_TAG = "system"
internal const val FX_INSTALL_SCOPE_ACTIVITY_TAG = "activity"
internal const val FX_INSTALL_SCOPE_FRAGMENT_TAG = "fragment"
internal const val FX_INSTALL_SCOPE_VIEW_GROUP_TAG = "view"
internal const val FX_HALF_PERCENT_MIN = 0F
internal const val FX_HALF_PERCENT_MAX = 1F

internal val topActivity: Activity?
    get() = FxAppLifecycleProvider.getTopActivity()

internal var View.isVisibility
    set(value) {
        visibility = if (value) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
    get() = visibility == View.VISIBLE

internal val Activity.decorView: FrameLayout?
    get() = try {
        window.decorView as FrameLayout
    } catch (_: Exception) {
        null
    }

internal val Activity.contentView: FrameLayout?
    get() = try {
        window.decorView.findViewById(android.R.id.content)
    } catch (_: Exception) {
        null
    }

internal fun ViewGroup.safeAddView(view: View?, lp: ViewGroup.LayoutParams? = null) {
    if (view == null) return
    if (view.parent == this) return
    (view.parent as? ViewGroup)?.removeView(view)
    if (lp == null) {
        addView(view)
    } else {
        addView(view, lp)
    }
}

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
inline fun <T> createFx(crossinline obj: FxScopeHelper.Builder.() -> T) =
    lazy(LazyThreadSafetyMode.NONE) {
        FxScopeHelper.Builder().run(obj)
    }

internal inline fun <reified T : Any> lazyLoad(
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.NONE,
    crossinline obj: () -> T
): Lazy<T> =
    lazy(mode) {
        obj()
    }

internal fun Float.coerceInFx(min: Float, max: Float): Float {
    if (this < min) return min
    if (this > max) return max
    return this
}

internal fun Int.coerceInFx(min: Int, max: Int): Int {
    if (this < min) return min
    if (this > max) return max
    return this
}

internal fun Float.withIn(min: Number, max: Number): Boolean {
    return this in min.toFloat()..max.toFloat()
}

internal fun Float.shr(count: Int): Float {
    return this / count
}

internal fun <T : FxBasicContainerView> IFxControl.groupView(): T? {
    val view = getManagerView() ?: return null
    return view as? T
}

internal val MotionEvent.pointerId: Int
    get() = try {
        getPointerId(actionIndex)
    } catch (_: Exception) {
        INVALID_TOUCH_ID
    }
