package com.petterp.floatingx.system

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.MainThread
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.host.FxLayoutSpec
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxInsets
import com.petterp.floatingx.core.layout.FxRect
import com.petterp.floatingx.system.container.FxWindowContainer
import com.petterp.floatingx.system.feature.SystemWindowFeature
import com.petterp.floatingx.system.permission.FxPermission
import com.petterp.floatingx.system.permission.FxPermissionRequest
import com.petterp.floatingx.system.permission.FxPermissionStrategy

/**
 * 系统级 host（spec §4）：容器是一个 WindowManager 窗口。
 * - 权限：Auto（默认，自动弹透明页申请）/ Manual（交给拦截器）/ Skip
 * - 被拒：有 fallback → requestSwap 降级（原 SYSTEM_AUTO）；无 → 停在 INSTALLED，之后 retryPermission()
 * - 默认 LayoutParams 见 defaultLayoutParams()，customizer 最后执行可覆盖任何字段
 */
public class SystemHost private constructor(
    override val context: Context,
    private val customizer: SystemLayoutParamsCustomizer?,
    private val strategy: FxPermissionStrategy,
    private val fallback: FxHost?,
    private val backListener: SystemBackListener?,
) : FxHost {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var session: FxHostSession? = null
    private var container: FxWindowContainer? = null
    private var released = false
    private val features: List<FxFeature> by lazy { listOf<FxFeature>(SystemWindowFeature()) }

    /** 没有容器时（release 之后）自己读屏幕尺寸的复用出参 */
    private val screen = Point()

    /** 当前窗口 LayoutParams 的只读快照（拷贝） */
    public val windowLayoutParams: WindowManager.LayoutParams
        get() = WindowManager.LayoutParams().also { it.copyFrom(container?.windowParams ?: buildLayoutParams()) }

    public val isPermissionGranted: Boolean get() = FxPermission.isGranted(context)

    // ---------- FxHost ----------

    override fun bind(session: FxHostSession) {
        check(!released) { "SystemHost 已 release，不能复用；请新建一个 SystemHost" }
        this.session = session
        checkPermission()
    }

    override fun createContainer(): FxContainer =
        FxWindowContainer(context, wm, buildLayoutParams(), backListener).also {
            container = it
            it.refreshBounds()
        }

    override fun attach(container: FxContainer) {
        val c = container as FxWindowContainer
        c.refreshBounds()
        try {
            wm.addView(c, c.windowParams)
            c.isAttachedToWm = true
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "系统浮窗 addView 失败（权限被撤销或 type 不允许）", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "系统浮窗 addView 失败（权限被撤销或 type 不允许）", e)
        }
    }

    override fun detach(container: FxContainer) {
        val c = container as FxWindowContainer
        if (!c.isAttachedToWm) return
        wm.removeViewImmediate(c)
        c.isAttachedToWm = false
    }

    override fun updateLayout(container: FxContainer, spec: FxLayoutSpec) {
        (container as FxWindowContainer).applyLayout(spec.x, spec.y, spec.gravity, spec.ltr)
    }

    /** 屏幕尺寸以容器缓存的为准（旋转/insets 时容器自己刷新），没有容器才现读 */
    override fun bounds(): FxBounds {
        val c = container
        if (c == null) {
            readScreen()
            return FxBounds(FxRect(0f, 0f, screen.x.toFloat(), screen.y.toFloat()))
        }
        val insets = if (c.isAttachedToWm) c.windowInsets else FxInsets.NONE
        return FxBounds(FxRect(0f, 0f, c.boundsWidth.toFloat(), c.boundsHeight.toFloat()), insets)
    }

    override fun hostFeatures(): List<FxFeature> = features

    override fun release() {
        released = true
        // 兜底（同 AppHost）：今天 core 在 cancel/swapHost 之前一定先 detach，这里摘不到东西；
        // 万一以后 core 改成直接 release，系统窗口也不会永远留在屏幕上——那是用户自己关不掉的
        container?.let { detach(it) }
        session = null
        container = null
    }

    // ---------- 权限 ----------

    /** 权限被拒后业务方拿到权限时调用；已有权限则直接挂载。与 core 一致，只能在主线程调用 */
    @MainThread
    public fun retryPermission() {
        if (released) return
        checkPermission()
    }

    private fun checkPermission() {
        when {
            strategy is FxPermissionStrategy.Skip || FxPermission.isGranted(context) -> session?.onHostReady()
            strategy is FxPermissionStrategy.Manual -> strategy.interceptor.onRequest(request)
            else -> requestPermission()
        }
    }

    private fun requestPermission() {
        FxPermission.request(context) { granted ->
            if (released) return@request
            if (granted) session?.onHostReady() else denied()
        }
    }

    private fun denied() {
        val fb = fallback
        if (fb != null) {
            session?.requestSwap(fb)
        } else {
            Log.w(TAG, "悬浮窗权限被拒，浮窗停留在 INSTALLED；获得权限后调用 SystemHost.retryPermission()")
        }
    }

    private val request = object : FxPermissionRequest {
        override fun proceed() {
            if (!released) requestPermission()
        }

        override fun deny() {
            if (!released) denied()
        }

        override fun useFallback() {
            if (!released) denied()
        }
    }

    // ---------- internal ----------

    private fun readScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            screen.set(b.width(), b.height())
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealSize(screen)
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams = defaultLayoutParams().also { customizer?.customize(it) }

    private fun defaultLayoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        format = PixelFormat.TRANSLUCENT
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        // NO_LIMITS：半隐 / overflow 要把内容放到屏幕外；core 已按 safe area clamp，不会误出界
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        gravity = Gravity.TOP or Gravity.START
    }

    // ---------- Builder ----------

    public class Builder(context: Context) {
        /** 系统窗口活得比页面久，绝不能持有 Activity */
        private val context: Context = if (context is Activity) context.applicationContext else context
        private var customizer: SystemLayoutParamsCustomizer? = null
        private var strategy: FxPermissionStrategy = FxPermissionStrategy.Auto
        private var fallback: FxHost? = null
        private var backListener: SystemBackListener? = null

        /** 在默认 LayoutParams 之后执行，可覆盖 type / flags / softInputMode 等任何字段 */
        public fun layoutParams(customizer: SystemLayoutParamsCustomizer): Builder = apply { this.customizer = customizer }

        public fun permission(strategy: FxPermissionStrategy): Builder = apply { this.strategy = strategy }

        /** 权限被拒时降级到的 host（通常是 AppHost） */
        public fun fallback(host: FxHost): Builder = apply { fallback = host }

        public fun onBackPressed(listener: SystemBackListener): Builder = apply { backListener = listener }

        public fun build(): SystemHost = SystemHost(context, customizer, strategy, fallback, backListener)
    }

    public companion object {
        private const val TAG = "Fx-system"

        @JvmStatic
        public fun builder(context: Context): Builder = Builder(context)
    }
}
