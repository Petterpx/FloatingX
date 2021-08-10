package com.petterp.floatingx.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import java.util.*

/**
 * 作为三方库,我们不应该将某些测量方法暴露,完成基本职责即可,避免与业务产生耦合,所以下面方法都是internal或者private
 * */

/** 缓存的内容高度与底部导航栏高度 */
private var screenHeightBf: Int = 0
private var navigationHeight: Int = 0

/** 获取内容视图高度,需要在onWindowFocusChanged方法后调用才生效 */
internal val Activity.contentHeightFromAndroid: Int
    get() =
        window.decorView.findViewById<FrameLayout>(android.R.id.content).height

/** 获取内容视图高度,需要在onWindowFocusChanged方法后调用才生效 */
internal val Activity.contentWidthFromAndroid: Int
    get() =
        window.decorView.findViewById<FrameLayout>(android.R.id.content).width

/** 真实屏幕高度,往往不会改变 */
internal val Context.realScreenHeight: Int
    get() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val dm = DisplayMetrics()
        display.getRealMetrics(dm)
        return dm.heightPixels
    }

/** 真实屏幕宽度,往往不会改变 */
internal val Context.realScreenWidth: Int
    get() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val dm = DisplayMetrics()
        display.getRealMetrics(dm)
        return dm.widthPixels
    }

/** 屏幕内容高度,一般情况下不会包含底部导航栏 [navigationBarHeight] ,全面屏机型可能会包含,故不能直接使用 */
internal val Context.screenHeight: Int
    get() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val dm = DisplayMetrics()
        display.getMetrics(dm)
        return dm.heightPixels
    }

/** 屏幕内容宽度,一般情况下与 [realScreenWidth] 一致*/
internal val Context.screenWidth: Int
    get() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val dm = DisplayMetrics()
        display.getMetrics(dm)
        return dm.widthPixels
    }

/** 部分机型,直接使用AppContext测量,部分情况会不准确 */
internal val Activity.contentHeight: Int
    get() = realScreenHeight - navigationBarHeight

/** 部分机型,直接使用AppContext测量,部分情况会不准确 */
internal val Activity.statusBarHeight: Int
    get() {
        var height = 0
        val resourceId: Int =
            resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            height = resources
                .getDimensionPixelSize(resourceId)
        }
        return height
    }

/** 部分机型,直接使用AppContext测量,部分情况会不准确 */
internal val Activity.navigationBarHeight: Int
    get() {
        // 获取底部导航栏内部会用到反射,这里进行缓存,尽可能避免多余损耗
        // 当导航栏改变时，往往屏幕高度会发生变化,故可以借此进行判断
        val newScreenHeight = screenHeight
        if (newScreenHeight == screenHeightBf) {
            return navigationHeight
        }
        screenHeightBf = newScreenHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            navigationHeight = getRealNavHeight(this)
            return navigationHeight
        }
        val isShow = checkNavigationBarShow(this) || isNavBarVendorHide(this) == 0
        val realSize = realScreenHeight
        // 少部分机型上述逻辑会判断失误,所以还得再判断屏幕大小与内容大小是否一致
        // 华为部分机型测量需要特别注意
        val newNavigationBarHeight = if (!isShow || realSize == newScreenHeight) {
            0
        } else getNavigationBarHeightFromSystem(newScreenHeight, realSize, this)
        navigationHeight = newNavigationBarHeight
        return newNavigationBarHeight
    }

/** from weilu@掘金 */
private val BRAND = Build.BRAND.toLowerCase(Locale.ROOT)
private val isXiaomi: Boolean
    get() = Build.MANUFACTURER.toLowerCase(Locale.ROOT) == "xiaomi"
private val isVivo: Boolean
    get() = BRAND.contains("vivo")
private val isOppo: Boolean
    get() = BRAND.contains("oppo") || BRAND.contains("realme")
private val isHuawei: Boolean
    get() = BRAND.contains("huawei") || BRAND.contains("honor")
private val isOnePlus: Boolean
    get() = BRAND.contains("oneplus")
private val isSamsung: Boolean
    get() = BRAND.contains("samsung")
private val isSmarTisan
    get() = BRAND.contains("smartisan")
private val isNokia: Boolean
    get() = BRAND.contains("nokia")
private val isGoogle: Boolean
    get() = BRAND.contains("google")

@RequiresApi(api = Build.VERSION_CODES.R)
private fun getRealNavHeight(context: Context): Int {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val windowMetrics = wm.currentWindowMetrics
    val windowInsets = windowMetrics.windowInsets
    val typeMask = WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
    val insets = windowInsets.getInsetsIgnoringVisibility(typeMask)
    return insets.bottom
}

private fun checkNavigationBarShow(context: Context): Boolean {
    var hasNavigationBar = false
    val rs = context.resources
    val id = rs.getIdentifier("config_showNavigationBar", "bool", "android")
    if (id > 0) {
        hasNavigationBar = rs.getBoolean(id)
    }
    try {
        @SuppressLint("PrivateApi") val systemPropertiesClass =
            Class.forName("android.os.SystemProperties")
        val m = systemPropertiesClass.getMethod("get", String::class.java)
        val navBarOverride = m.invoke(systemPropertiesClass, "qemu.hw.mainkeys") as String
        // 判断是否隐藏了底部虚拟导航
        var navigationBarIsMin = 0
        navigationBarIsMin = Settings.Global.getInt(
            context.contentResolver,
            "navigationbar_is_min", 0
        )
        if ("1" == navBarOverride || 1 == navigationBarIsMin) {
            hasNavigationBar = false
        } else if ("0" == navBarOverride) {
            hasNavigationBar = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return hasNavigationBar
}

private fun getNavigationBarHeightFromSystem(
    screenSize: Int,
    realSize: Int,
    context: Context
): Int {
    val resourceId =
        context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        val height = context.resources.getDimensionPixelSize(resourceId)
        // 超出系统默认的导航栏高度以上，则认为存在虚拟导航
        if (realSize - screenSize > height - 10) {
            height
        } else 0
    } else 0
}

private fun isNavBarVendorHide(context: Context): Int =
    when {
        isVivo -> vivoNavigationEnabled(context)
        isOppo -> oppoNavigationEnabled(context)
        isXiaomi -> xiaomiNavigationEnabled(context)
        isHuawei -> huaWeiNavigationEnabled(context)
        isOnePlus -> onePlusNavigationEnabled(context)
        isSamsung -> samsungNavigationEnabled(context)
        isSmarTisan -> smartisanNavigationEnabled(context)
        isNokia -> nokiaNavigationEnabled(context)
        // // navigation_mode 三种模式均有导航栏，只是高度不同。
        isGoogle -> 0
        else -> -1
    }

/**
 * 判断当前系统是使用导航键还是手势导航操作
 *
 * @param context
 * @return 0 表示使用的是虚拟导航键，1 表示使用的是手势导航，默认是0
 */
private fun vivoNavigationEnabled(context: Context): Int {
    return Settings.Secure.getInt(context.contentResolver, "navigation_gesture_on", 0)
}

private fun oppoNavigationEnabled(context: Context): Int {
    return Settings.Secure.getInt(context.contentResolver, "hide_navigationbar_enable", 0)
}

private fun xiaomiNavigationEnabled(context: Context): Int {
    return Settings.Global.getInt(context.contentResolver, "force_fsg_nav_bar", 0)
}

private fun huaWeiNavigationEnabled(context: Context): Int {
    return Settings.Global.getInt(context.contentResolver, "navigationbar_is_min", 0)
}

/**
 * @param context
 * @return 0虚拟导航键  2为手势导航
 */
private fun onePlusNavigationEnabled(context: Context): Int {
    val result = Settings.Secure.getInt(context.contentResolver, "navigation_mode", 0)
    if (result == 2 && Settings.System.getInt(
            context.contentResolver,
            "buttons_show_on_screen_navkeys",
            0
        ) != 0
    ) {
        // 两种手势 0有按钮， 1没有按钮
        return 0
    }
    return result
}

private fun samsungNavigationEnabled(context: Context): Int {
    return Settings.Global.getInt(context.contentResolver, "navigationbar_hide_bar_enabled", 0)
}

private fun smartisanNavigationEnabled(context: Context): Int {
    return Settings.Global.getInt(context.contentResolver, "navigationbar_trigger_mode", 0)
}

private fun nokiaNavigationEnabled(context: Context): Int {
    val result = (
        Settings.Secure.getInt(
            context.contentResolver,
            "swipe_up_to_switch_apps_enabled",
            0
        ) != 0 ||
            Settings.System.getInt(
            context.contentResolver,
            "navigation_bar_can_hiden",
            0
        ) != 0
        )
    return if (result) {
        1
    } else {
        0
    }
}
