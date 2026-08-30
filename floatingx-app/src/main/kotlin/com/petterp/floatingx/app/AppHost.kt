package com.petterp.floatingx.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.petterp.floatingx.app.internal.ActivityRules
import com.petterp.floatingx.core.FxActivityTracker
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxInsets
import com.petterp.floatingx.core.layout.FxRect

/**
 * App 级 host（spec §3）：跟随前台 Activity，把同一个 Layer 容器挂到当前 Activity 的 DecorView（或 content）上。
 *
 * - 换页：API 29+ 在 onActivityPostResumed、API < 29 在 onActivityResumed 后主线程 post 里
 *   把容器从旧父**静默**挪到新父——engine 状态、feature、动画不重来，位置由 translation 保留，
 *   新父首次布局时按锚点校正（insets 不同的页面也对齐）。
 * - 被过滤（黑白名单 / 自定义规则）的 Activity：卸下容器（浮窗不显示），回到允许的页面再挂上。
 * - 当前挂载的 Activity 销毁 → onHostLost；其它 Activity 销毁忽略。
 * - bounds()：父容器尺寸 + 系统栏 / 刘海 insets。
 */
public class AppHost private constructor(
    public val application: Application,
    override val context: Context,
    private val rules: ActivityRules,
    public val target: AppAttachTarget,
) : FxHost, FxActivityTracker.Observer {

    /** 容器当前挂在哪个 Activity 上；未挂载为 null */
    public var attachedActivity: Activity? = null
        private set

    private var session: FxHostSession? = null
    private var parent: ViewGroup? = null
    private var container: FxContainer? = null
    private var released = false
    private val tmpLocation = IntArray(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 父容器每次布局（首帧、旋转、insets 变化）都让 core 按锚点重算；relayout 只写 translation，不会成环 */
    private val parentLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> session?.onBoundsChanged() }

    public fun accepts(activity: Activity): Boolean = rules.accept(activity)

    // ---------- FxHost ----------

    override fun bind(session: FxHostSession) {
        this.session = session
        FxActivityTracker.init(application)
        FxActivityTracker.addObserver(this)
        FxActivityTracker.topActivity?.let { moveTo(it) }
    }

    override fun createContainer(): FxContainer = FxLayerContainer(context)

    override fun attach(container: FxContainer) {
        val p = checkNotNull(parent) { "AppHost 尚未 ready 就被 attach" }
        this.container = container
        p.addView(container.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        p.addOnLayoutChangeListener(parentLayoutListener)
    }

    override fun detach(container: FxContainer) {
        (container.view.parent as? ViewGroup)?.let {
            it.removeOnLayoutChangeListener(parentLayoutListener)
            it.removeView(container.view)
        }
        this.container = null
    }

    override fun bounds(): FxBounds {
        val p = parent ?: return FxBounds(FxRect(0f, 0f, 0f, 0f))
        val rect = FxRect(0f, 0f, p.width.toFloat(), p.height.toFloat())
        val windowInsets = ViewCompat.getRootWindowInsets(p) ?: return FxBounds(rect)
        val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        p.getLocationInWindow(tmpLocation)
        val root = p.rootView
        return FxBounds(rect, contentInsets(bars, target, tmpLocation[0], tmpLocation[1], p.width, p.height, root.width, root.height))
    }

    override fun release() {
        released = true
        FxActivityTracker.removeObserver(this)
        parent?.removeOnLayoutChangeListener(parentLayoutListener)
        parent = null
        attachedActivity = null
        container = null
        session = null
    }

    // ---------- FxActivityTracker.Observer ----------

    override fun onActivityResumed(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return // 29+ 走 onActivityPostResumed
        // 低版本没有 postResumed：排到当前 onResume 派发之后再挂载（不用 decorView.post：此时 decor 未必已 attach）
        mainHandler.post { if (!released && !activity.isDestroyed) moveTo(activity) }
    }

    override fun onActivityPostResumed(activity: Activity) {
        moveTo(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity === attachedActivity) lose()
    }

    // ---------- internal ----------

    private fun moveTo(activity: Activity) {
        if (released || activity === attachedActivity) return
        if (!rules.accept(activity)) {
            if (attachedActivity != null) lose()
            return
        }
        val newParent = parentOf(activity)
        val oldParent = parent
        val c = container
        attachedActivity = activity
        parent = newParent
        if (c != null && oldParent != null) {
            oldParent.removeOnLayoutChangeListener(parentLayoutListener)
            oldParent.removeView(c.view)
            newParent.addView(c.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            newParent.addOnLayoutChangeListener(parentLayoutListener)
            session?.onBoundsChanged()
        } else {
            session?.onHostReady()
        }
    }

    private fun lose() {
        parent?.removeOnLayoutChangeListener(parentLayoutListener)
        attachedActivity = null
        parent = null
        session?.onHostLost()
    }

    private fun parentOf(activity: Activity): ViewGroup = when (target) {
        AppAttachTarget.DECOR -> activity.window.decorView as ViewGroup
        AppAttachTarget.CONTENT -> activity.findViewById(android.R.id.content)
    }

    // ---------- Builder ----------

    public class Builder(private val application: Application) {
        private val blackClasses = mutableListOf<Class<out Activity>>()
        private val blackNames = mutableSetOf<String>()
        private val whiteClasses = mutableListOf<Class<out Activity>>()
        private val whiteNames = mutableSetOf<String>()
        private val filters = mutableListOf<AppActivityFilter>()
        private var target = AppAttachTarget.DECOR
        private var context: Context = application

        /** 这些 Activity（含子类）上不显示浮窗 */
        public fun blacklist(vararg classes: Class<out Activity>): Builder = apply { blackClasses += classes }

        /** 按类全名精确匹配的黑名单 */
        public fun blacklist(vararg classNames: String): Builder = apply { blackNames += classNames }

        /** 只在这些 Activity（含子类）上显示浮窗 */
        public fun whitelist(vararg classes: Class<out Activity>): Builder = apply { whiteClasses += classes }

        public fun whitelist(vararg classNames: String): Builder = apply { whiteNames += classNames }

        /** 自定义规则（#221），可多次调用，全部通过才显示 */
        public fun filter(filter: AppActivityFilter): Builder = apply { filters += filter }

        public fun attachTo(target: AppAttachTarget): Builder = apply { this.target = target }

        /** 内容 view 用 Application context 创建；需要主题属性（Material 组件等）时在这里指定 */
        public fun theme(@StyleRes themeRes: Int): Builder = apply { context = ContextThemeWrapper(application, themeRes) }

        public fun build(): AppHost = AppHost(
            application,
            context,
            ActivityRules(blackClasses.toList(), blackNames.toSet(), whiteClasses.toList(), whiteNames.toSet(), filters.toList()),
            target,
        )
    }

    public companion object {
        @JvmStatic
        public fun builder(application: Application): Builder = Builder(application)

        /**
         * 把窗口的系统栏 insets 换算到父容器坐标系：
         * DECOR 直接用；CONTENT 扣掉父容器已经被系统栏挤开的部分（非 edge-to-edge 时结果为 0），不为负。
         */
        internal fun contentInsets(
            bars: Insets,
            target: AppAttachTarget,
            offsetX: Int,
            offsetY: Int,
            parentWidth: Int,
            parentHeight: Int,
            rootWidth: Int,
            rootHeight: Int,
        ): FxInsets {
            if (target == AppAttachTarget.DECOR) {
                return FxInsets(bars.left.toFloat(), bars.top.toFloat(), bars.right.toFloat(), bars.bottom.toFloat())
            }
            val left = (bars.left - offsetX).coerceAtLeast(0)
            val top = (bars.top - offsetY).coerceAtLeast(0)
            val right = (bars.right - (rootWidth - offsetX - parentWidth)).coerceAtLeast(0)
            val bottom = (bars.bottom - (rootHeight - offsetY - parentHeight)).coerceAtLeast(0)
            return FxInsets(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        }
    }
}
