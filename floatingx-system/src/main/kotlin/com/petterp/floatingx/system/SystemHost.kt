package com.petterp.floatingx.system

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.annotation.StyleRes
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.host.FxLayoutSpec
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxRect
import com.petterp.floatingx.system.container.FxWindowContainer
import com.petterp.floatingx.system.feature.KeyboardFeature
import com.petterp.floatingx.system.feature.SystemWindowFeature
import com.petterp.floatingx.system.permission.FxPermission
import com.petterp.floatingx.system.permission.FxPermissionRequest
import com.petterp.floatingx.system.permission.FxPermissionStrategy

/**
 * 系统级 host（spec §4）：容器是一个 WindowManager 窗口。
 * - 权限：Auto（默认，自动弹透明页申请）/ Manual（交给拦截器）/ Skip
 * - 被拒：有 fallback → requestSwap 降级（原 SYSTEM_AUTO）；无 → 停在 INSTALLED，之后 retryPermission()
 * - 默认 LayoutParams 见 defaultLayoutParams()，customizer 最后执行可覆盖任何字段
 *
 * **后台申请权限的限制**：Android 10（Q）起系统禁止后台启动 Activity。应用在后台（或从 Service）
 * 触发 Auto 策略时，权限申请页可能**悄无声息地起不来**，什么都不会弹出，用户毫无感知。
 * 需要在后台安装浮窗时，用 [FxPermissionStrategy.Manual] / [FxPermissionStrategy.Skip] 把申请推迟，
 * 等应用回到前台再 [retryPermission]（或自行申请后再调用它）。
 */
public class SystemHost private constructor(
    override val context: Context,
    private val customizer: SystemLayoutParamsCustomizer?,
    private val strategy: FxPermissionStrategy,
    private val fallback: FxHost?,
    private val backListener: SystemBackListener?,
    private val keyboardIds: IntArray,
) : FxHost {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var session: FxHostSession? = null
    private var container: FxWindowContainer? = null
    private var released = false
    private val features: List<FxFeature> by lazy {
        listOfNotNull<FxFeature>(
            SystemWindowFeature(),
            if (keyboardIds.isNotEmpty()) KeyboardFeature(keyboardIds) else null,
        )
    }

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
        } catch (e: IllegalStateException) {
            // "View has already been added to the window manager"：窗口其实还在屏幕上。
            // 这里必须记成"已挂载"，否则 detach 会直接 return，窗口再也摘不掉
            Log.e(TAG, "系统浮窗 addView 失败（该 view 已挂在 WindowManager 上），按已挂载处理", e)
            c.isAttachedToWm = true
        }
    }

    override fun detach(container: FxContainer) {
        val c = container as FxWindowContainer
        if (!c.isAttachedToWm) return
        try {
            wm.removeViewImmediate(c)
        } catch (e: IllegalArgumentException) {
            // "View not attached to window manager"：窗口早就没了（进程被 WMS 清过、外部先摘过），
            // 按已摘掉处理即可，重复抛出只会把 cancel() 打断
            Log.w(TAG, "系统浮窗 removeView 失败（该 view 未挂在 WindowManager 上），按已摘除处理", e)
        }
        c.isAttachedToWm = false
    }

    override fun updateLayout(container: FxContainer, spec: FxLayoutSpec) {
        (container as FxWindowContainer).applyLayout(spec.x, spec.y, spec.gravity, spec.ltr)
    }

    /**
     * 屏幕尺寸与 insets 都以容器缓存的为准（创建/挂载/旋转时 refreshBounds 刷新）；
     * insets 是屏幕级的，与窗口挂没挂上、挂在哪都无关。
     * release 之后没有容器，返回全 0——core 把零尺寸当作"还不能定位"，不会拿脏数据算坐标。
     */
    override fun bounds(): FxBounds {
        val c = container ?: return FxBounds(FxRect(0f, 0f, 0f, 0f))
        return FxBounds(FxRect(0f, 0f, c.boundsWidth.toFloat(), c.boundsHeight.toFloat()), c.windowInsets)
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

    /**
     * 权限被拒后业务方拿到权限时调用；已有权限则直接挂载。与 core 一致，只能在主线程调用。
     *
     * Q+ 后台起不了 Activity（见类注释），所以后台安装 + Auto 策略时页面可能根本没弹出来：
     * 回到前台后调用本方法重新申请/挂载。
     */
    @MainThread
    public fun retryPermission() {
        if (released) return
        val c = container
        val s = session
        // addView 失败过（权限被撤销 / type 不被允许 / Skip 策略配了需要权限的 type）：
        // 容器还在 core 手里但没挂到 WindowManager 上，此时 core 的状态是 ATTACHED/SHOWN，onHostReady 会被忽略。
        // 先让它退回 INSTALLED 再重新挂载，desiredVisible 会被保留（detach 对未挂载的容器直接 return，不会重复 removeView）。
        // Skip 策略下不看 isPermissionGranted——它压根不检查权限，否则这条恢复路径对 Skip 永远不生效
        if (c != null && !c.isAttachedToWm && s != null && (strategy is FxPermissionStrategy.Skip || isPermissionGranted)) {
            s.onHostLost()
            s.onHostReady()
            return
        }
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

    private fun buildLayoutParams(): WindowManager.LayoutParams = defaultLayoutParams().also { customizer?.customize(it) }

    // RtlHardcoded：WMS 应用 LayoutParams.gravity 时不带布局方向，START/END 由 WindowLayoutMath 自行解析成 LEFT/RIGHT
    @SuppressLint("RtlHardcoded")
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
        // WMS 应用 LayoutParams.gravity 时不带布局方向，START 在 RTL 下不会自动翻转，直接写 LEFT 更诚实；
        // 何况首次 applyLayout 就会按锚点覆盖它（见 WindowLayoutMath）
        gravity = Gravity.TOP or Gravity.LEFT
    }

    // ---------- Builder ----------

    public class Builder(context: Context) {
        /** 解包后的基准 context（一定不是 Activity）；[theme] 每次都基于它包一层，多次调用不会套娃 */
        private val baseContext: Context = unwrapActivity(context)
        private var context: Context = baseContext
        private var customizer: SystemLayoutParamsCustomizer? = null
        private var strategy: FxPermissionStrategy = FxPermissionStrategy.Auto
        private var fallback: FxHost? = null
        private var backListener: SystemBackListener? = null
        private var keyboardIds: IntArray = IntArray(0)

        /** 在默认 LayoutParams 之后执行，可覆盖 type / flags / softInputMode 等任何字段 */
        public fun layoutParams(customizer: SystemLayoutParamsCustomizer): Builder = apply { this.customizer = customizer }

        public fun permission(strategy: FxPermissionStrategy): Builder = apply { this.strategy = strategy }

        /** 权限被拒时降级到的 host（通常是 AppHost） */
        public fun fallback(host: FxHost): Builder = apply { fallback = host }

        public fun onBackPressed(listener: SystemBackListener): Builder = apply { backListener = listener }

        /** 这些 EditText 被触摸时窗口临时可聚焦并弹出键盘 */
        public fun keyboard(vararg editTextIds: Int): Builder = apply { keyboardIds = editTextIds }

        /** 内容 view 用（解包后的）application context 创建；需要主题属性（Material 组件等）时在这里指定 */
        public fun theme(@StyleRes themeRes: Int): Builder = apply { context = ContextThemeWrapper(baseContext, themeRes) }

        public fun build(): SystemHost = SystemHost(context, customizer, strategy, fallback, backListener, keyboardIds)

        private companion object {
            /**
             * 系统窗口活得比页面久，绝不能持有 Activity。
             * Activity 常被 ContextThemeWrapper / ContextWrapper 包一层再传进来（Material 主题、动态换肤…），
             * 所以要沿 baseContext 链一路查下去，任何一层是 Activity 都解包成 applicationContext。
             */
            fun unwrapActivity(context: Context): Context {
                var c: Context? = context
                while (c != null) {
                    if (c is Activity) {
                        Log.w(TAG, "SystemHost.Builder 收到 Activity（可能被 ContextWrapper 包裹），已解包为 applicationContext；需要主题请用 theme(themeRes)")
                        return context.applicationContext
                    }
                    c = (c as? ContextWrapper)?.baseContext
                }
                return context
            }
        }
    }

    public companion object {
        private const val TAG = "Fx-system"

        @JvmStatic
        public fun builder(context: Context): Builder = Builder(context)
    }
}
