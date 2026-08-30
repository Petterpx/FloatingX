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
) : FxHost {

    /** 容器当前挂在哪个 Activity 上；未挂载为 null */
    public var attachedActivity: Activity? = null
        private set

    private var session: FxHostSession? = null
    private var parent: ViewGroup? = null
    private var container: FxContainer? = null
    private var released = false
    private val tmpLocation = IntArray(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 可用区快照，全是原始 int（下标见 [SNAPSHOT_SIZE] 上方说明）：bounds() 与布局监听共用同一段计算，
     * 布局监听只比较这些 int，**不构造 FxInsets / FxBounds**——几何对象只在真的要派发或外部调 bounds() 时才建。
     */
    private val boundsScratch = IntArray(SNAPSHOT_SIZE)
    private val lastSnapshot = IntArray(SNAPSHOT_SIZE)
    private var hasLastSnapshot = false

    /**
     * 父容器布局回调。**必须做变化过滤**：只要页面里任何子孙 view 调过 requestLayout
     * （TextView.setText、RecyclerView 增删、软键盘弹出……），父容器的 layout 就会重跑并触发这里，
     * 频率远高于「首帧 / 旋转 / insets 变化」。而 core 的 LocationFeature.onBoundsChanged 会清掉
     * dragInput 并 relayout——拖动中途收到就会卡住这一轮手势并弹回已提交的锚点，吸附动画也会被截断。
     * 所以这里只在可用区（父容器尺寸 + insets）真的变了时才派发；换父与 lost 时清缓存，
     * 保证新父的第一次布局一定派发（spec 需要的锚点校正）。
     */
    private val parentLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> onParentLayout() }

    public fun accepts(activity: Activity): Boolean = rules.accept(activity)

    // ---------- FxHost ----------

    override fun bind(session: FxHostSession) {
        check(!released) { "AppHost 已 release，不能复用；请新建一个 AppHost" }
        this.session = session
        FxActivityTracker.init(application)
        FxActivityTracker.addObserver(trackerObserver)
        FxActivityTracker.topActivity?.let { moveTo(it) }
    }

    override fun createContainer(): FxContainer = FxLayerContainer(context)

    override fun attach(container: FxContainer) {
        val p = checkNotNull(parent) { "AppHost 尚未 ready 就被 attach" }
        this.container = container
        mount(p, container.view)
    }

    override fun detach(container: FxContainer) {
        (container.view.parent as? ViewGroup)?.let { unmount(it, container.view) }
        this.container = null
    }

    override fun bounds(): FxBounds {
        val s = boundsScratch
        if (!computeSnapshot(s)) return FxBounds(FxRect(0f, 0f, 0f, 0f))
        return FxBounds(FxRect(0f, 0f, s[0].toFloat(), s[1].toFloat()), insetsOf(s))
    }

    override fun release() {
        released = true
        FxActivityTracker.removeObserver(trackerObserver)
        mainHandler.removeCallbacksAndMessages(null)
        // 今天 core 在 cancel() 与 swapHost() 之前都会先 detach（engine.onHostLost/cancel → performDetach），
        // 所以这里摘不到东西，是防御性的兜底：万一以后 core 改成直接 release，容器也不会留在页面上。
        container?.view?.let { v -> (v.parent as? ViewGroup)?.let { unmount(it, v) } }
        parent?.removeOnLayoutChangeListener(parentLayoutListener)
        parent = null
        attachedActivity = null
        container = null
        session = null
        hasLastSnapshot = false
    }

    // ---------- 前台 Activity 跟踪 ----------

    /** 不作为 AppHost 的公开父类型暴露：这些回调是内部实现，调用方不该也不需要直接调 */
    private val trackerObserver = object : FxActivityTracker.Observer {
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
    }

    // ---------- internal ----------

    private fun moveTo(activity: Activity) {
        if (released || activity === attachedActivity) return
        if (!accepts(activity)) {
            if (attachedActivity != null) lose()
            return
        }
        val newParent = parentOf(activity)
        val oldParent = parent
        val c = container
        attachedActivity = activity
        parent = newParent
        // 新父的可用区（尺寸/insets）与旧父无关：清掉快照，让新父第一次布局一定派发，core 才能按锚点校正
        hasLastSnapshot = false
        if (c != null && oldParent != null) {
            unmount(oldParent, c.view)
            mount(newParent, c.view)
            session?.onBoundsChanged()
        } else {
            session?.onHostReady()
        }
    }

    private fun lose() {
        parent?.removeOnLayoutChangeListener(parentLayoutListener)
        attachedActivity = null
        parent = null
        hasLastSnapshot = false
        session?.onHostLost()
    }

    private fun mount(parent: ViewGroup, view: View) {
        parent.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        parent.addOnLayoutChangeListener(parentLayoutListener)
    }

    private fun unmount(parent: ViewGroup, view: View) {
        parent.removeOnLayoutChangeListener(parentLayoutListener)
        parent.removeView(view)
    }

    private fun onParentLayout() {
        val s = boundsScratch
        if (!computeSnapshot(s)) return
        // 只比 int：没变就直接返回，这条路径上一个 FxInsets / FxBounds 都不会构造
        if (hasLastSnapshot && s.contentEquals(lastSnapshot)) return
        s.copyInto(lastSnapshot)
        hasLastSnapshot = true
        session?.onBoundsChanged()
    }

    /**
     * 把当前可用区的原始 int 算进 [out]（下标见 [SNAPSHOT_SIZE]），父容器不存在则返回 false。
     * 独立成函数是为了让 bounds() 与布局监听共用同一段计算，且监听侧只做 int 比较。
     */
    private fun computeSnapshot(out: IntArray): Boolean {
        val p = parent ?: return false
        out[0] = p.width
        out[1] = p.height
        val windowInsets = ViewCompat.getRootWindowInsets(p)
        if (windowInsets == null) {
            // 还没拿到窗口 insets（未 attach / 老设备）：按无 insets 处理
            out.fill(0, 2, SNAPSHOT_SIZE)
            return true
        }
        out[HAS_INSETS] = 1
        val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        out[2] = bars.left
        out[3] = bars.top
        out[4] = bars.right
        out[5] = bars.bottom
        if (target == AppAttachTarget.DECOR) {
            // DECOR 的 insets 直接就是 bars，用不到父容器在窗口内的位置，省掉一次层级遍历
            out.fill(0, 6, HAS_INSETS)
            return true
        }
        // CONTENT 的 insets 还取决于父容器在窗口里的位置与窗口尺寸，一并进快照，这些也变了才算变
        p.getLocationInWindow(tmpLocation)
        val root = p.rootView
        out[6] = tmpLocation[0]
        out[7] = tmpLocation[1]
        out[8] = root.width
        out[9] = root.height
        return true
    }

    /** 快照 → FxInsets；只在真的要用几何对象（bounds()）时调用 */
    private fun insetsOf(s: IntArray): FxInsets {
        if (s[HAS_INSETS] == 0) return FxInsets.NONE
        return contentInsets(s[2], s[3], s[4], s[5], target, s[6], s[7], s[0], s[1], s[8], s[9])
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
        @SafeVarargs
        public fun blacklist(vararg classes: Class<out Activity>): Builder = apply { blackClasses += classes }

        /** 按类全名精确匹配的黑名单 */
        public fun blacklist(vararg classNames: String): Builder = apply { blackNames += classNames }

        /** 只在这些 Activity（含子类）上显示浮窗 */
        @SafeVarargs
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
        /**
         * 可用区快照的长度。下标：
         * 0 父宽、1 父高、2..5 系统栏 insets（左上右下）、6..7 父容器在窗口内的 x/y、8..9 窗口宽高、[HAS_INSETS] 是否拿到了窗口 insets。
         * 6..9 只有 CONTENT 目标用得上（DECOR 恒为 0）。
         */
        private const val SNAPSHOT_SIZE = 11
        private const val HAS_INSETS = 10

        @JvmStatic
        public fun builder(application: Application): Builder = Builder(application)

        /**
         * 把窗口的系统栏 insets（[barsLeft]..[barsBottom]）换算到父容器坐标系：
         * DECOR 直接用；CONTENT 扣掉父容器已经被系统栏挤开的部分（非 edge-to-edge 时结果为 0），不为负。
         * 参数全是原始 int，调用方（布局监听）因此可以只比较 int、不构造任何几何对象。
         */
        internal fun contentInsets(
            barsLeft: Int,
            barsTop: Int,
            barsRight: Int,
            barsBottom: Int,
            target: AppAttachTarget,
            offsetX: Int,
            offsetY: Int,
            parentWidth: Int,
            parentHeight: Int,
            rootWidth: Int,
            rootHeight: Int,
        ): FxInsets {
            if (target == AppAttachTarget.DECOR) {
                return FxInsets(barsLeft.toFloat(), barsTop.toFloat(), barsRight.toFloat(), barsBottom.toFloat())
            }
            val left = (barsLeft - offsetX).coerceAtLeast(0)
            val top = (barsTop - offsetY).coerceAtLeast(0)
            val right = (barsRight - (rootWidth - offsetX - parentWidth)).coerceAtLeast(0)
            val bottom = (barsBottom - (rootHeight - offsetY - parentHeight)).coerceAtLeast(0)
            return FxInsets(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        }
    }
}
