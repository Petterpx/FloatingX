package com.petterp.floatingx.assist.helper

import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.petterp.floatingx.assist.FxAdsorbDirection
import com.petterp.floatingx.assist.FxAnimation
import com.petterp.floatingx.assist.FxBorderMargin
import com.petterp.floatingx.assist.FxDisplayMode
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxTouchListener
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.util.FxLog
import kotlin.math.abs

@DslMarker
annotation class FxBuilderDsl

/** 通用构建器helper */
abstract class FxBasisHelper {
    @JvmField
    internal var layoutId: Int = 0

    @JvmField
    internal var offsetX: Float = 0f

    @JvmField
    internal var offsetY: Float = 0f

    @JvmField
    internal var layoutView: View? = null

    @JvmField
    internal var gravity: FxGravity = FxGravity.DEFAULT

    @JvmField
    internal var clickTime: Long = 300L

    @JvmField
    internal var layoutParams: FrameLayout.LayoutParams? = null

    @JvmField
    internal var fxAnimation: FxAnimation? = null

    @JvmField
    internal var defaultY: Float = 0f

    @JvmField
    internal var defaultX: Float = 0f

    @JvmField
    internal var edgeOffset: Float = 0f

    @JvmField
    internal var fxBorderMargin: FxBorderMargin = FxBorderMargin()

    @JvmField
    internal var assistLocation: FxBorderMargin? = null

    @JvmField
    internal var displayMode: FxDisplayMode = FxDisplayMode.Normal

    @JvmField
    internal var adsorbDirection: FxAdsorbDirection = FxAdsorbDirection.LEFT_OR_RIGHT

    internal var enableFx: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            fxLog.v("update enableFx: [$value]")
        }

    @JvmField
    internal var enableEdgeAdsorption: Boolean = true

    @JvmField
    internal var enableEdgeRebound: Boolean = true

    @JvmField
    internal var enableHalfHide: Boolean = false

    @JvmField
    internal var halfHidePercent: Float = 0.5f

    @JvmField
    internal var enableAnimation: Boolean = false

    @JvmField
    internal var enableSaveDirection: Boolean = false

    @JvmField
    internal var enableDebugLog: Boolean = false

    @JvmField
    internal var enableClickListener: Boolean = true

    @JvmField
    internal var enableAssistLocation: Boolean = false

    @JvmField
    internal var iFxTouchListener: IFxTouchListener? = null

    @JvmField
    internal var iFxViewLifecycles: MutableList<IFxViewLifecycle> = mutableListOf()

    @JvmField
    internal var iFxConfigStorage: IFxConfigStorage? = null

    @JvmField
    internal var iFxClickListener: View.OnClickListener? = null

    @JvmField
    internal var iFxLongClickListener: View.OnLongClickListener? = null

    internal lateinit var fxLog: FxLog

    @JvmField
    internal var fxLogTag: String = ""

    @JvmField
    /** 底部导航栏与状态栏测量高度 */
    internal var navigationBarHeight: Int = 0

    @JvmField
    internal var statsBarHeight: Int = 0

    @JvmField
    internal var reInstall: Boolean = false

    @JvmSynthetic
    internal fun initLog(scope: String) {
        fxLog = FxLog.builder(enableDebugLog, "$scope-$fxLogTag")
    }

    @JvmSynthetic
    internal fun clear() {
        fxAnimation?.cancelAnimation()
        if (reInstall) {
            reInstall = false
            return
        }
        layoutView = null
        enableFx = false
    }

    val safeEdgeOffSet: Float
        get() = if (enableEdgeRebound) edgeOffset else 0F

    internal val hasClickStatus: Boolean
        get() = enableClickListener && (iFxClickListener != null || iFxLongClickListener != null)

    internal val hasDefaultXY: Boolean
        get() = defaultX != 0f || defaultY != 0f

    abstract class Builder<T, B : FxBasisHelper> {
        @LayoutRes
        private var layoutId: Int = 0
        private var layoutView: View? = null
        private var clickTime: Long = 300L
        private var fxAnimation: FxAnimation? = null
        private var gravity: FxGravity = FxGravity.DEFAULT
        private var layoutParams: FrameLayout.LayoutParams? = null
        private var displayMode: FxDisplayMode = FxDisplayMode.Normal
        private var edgeAdsorbDirection: FxAdsorbDirection = FxAdsorbDirection.LEFT_OR_RIGHT

        private var offsetX: Float = 0f
        private var offsetY: Float = 0f
        private var edgeOffset: Float = 0f
        private var enableFx: Boolean = false
        private var defaultX = 0f
        private var defaultY = 0f
        private var fxBorderMargin: FxBorderMargin = FxBorderMargin()
        private var assistLocation: FxBorderMargin = FxBorderMargin()

        private var fxLogTag: String = ""

        private var enableDebugLog: Boolean = false
        private var enableAnimation: Boolean = false
        private var enableHalfHide: Boolean = false
        private var halfHidePercent: Float = 0.5f
        private var enableEdgeRebound: Boolean = true
        private var enableSaveDirection: Boolean = false
        private var enableClickListener: Boolean = false
        private var enableEdgeAdsorption: Boolean = true

        private var iFxConfigStorage: IFxConfigStorage? = null
        private var iFxTouchListener: IFxTouchListener? = null
        private var ifxClickListener: View.OnClickListener? = null
        private var ifxLongClickListener: View.OnLongClickListener? = null
        private var iFxViewLifecycles: MutableList<IFxViewLifecycle> = mutableListOf()

        protected abstract fun buildHelper(): B

        open fun build(): B =
            buildHelper().apply {
                enableFx = this@Builder.enableFx
                layoutId = this@Builder.layoutId
                layoutView = this@Builder.layoutView
                gravity = this@Builder.gravity
                clickTime = this@Builder.clickTime
                layoutParams = this@Builder.layoutParams
                fxAnimation = this@Builder.fxAnimation

                displayMode = this@Builder.displayMode
                defaultX = this@Builder.defaultX
                defaultY = this@Builder.defaultY
                assistLocation = this@Builder.assistLocation

                offsetX = this@Builder.offsetX
                offsetY = this@Builder.offsetY

                edgeOffset = this@Builder.edgeOffset
                fxBorderMargin = this@Builder.fxBorderMargin
                adsorbDirection = this@Builder.edgeAdsorbDirection

                enableAnimation = this@Builder.enableAnimation
                enableHalfHide = this@Builder.enableHalfHide
                halfHidePercent = this@Builder.halfHidePercent
                enableEdgeAdsorption = this@Builder.enableEdgeAdsorption
                enableEdgeRebound = this@Builder.enableEdgeRebound
                enableSaveDirection = this@Builder.enableSaveDirection
                enableClickListener = this@Builder.enableClickListener
                enableAssistLocation = assistLocation != null

                enableDebugLog = this@Builder.enableDebugLog
                fxLogTag = this@Builder.fxLogTag

                iFxTouchListener = this@Builder.iFxTouchListener
                iFxViewLifecycles = this@Builder.iFxViewLifecycles
                iFxConfigStorage = this@Builder.iFxConfigStorage
                iFxClickListener = this@Builder.ifxClickListener
                iFxLongClickListener = this@Builder.ifxLongClickListener
            }

        /** 设置悬浮窗view的layout */
        fun setLayout(@LayoutRes layoutId: Int): T {
            this.layoutView = null
            this.layoutId = layoutId
            return this as T
        }

        /** 设置悬浮窗View */
        fun setLayoutView(view: View): T {
            layoutId = 0
            this.layoutView = view
            return this as T
        }

        /**
         * 是否允许浮窗移动 -(onTouchEvent)
         *
         * true -> 浮窗允许移动
         *
         * false -> 浮窗屏蔽移动
         *
         * Tips: 不影响原有手势事件的传递流程
         * @param isEnable 默认true
         */
        @Deprecated("已废弃，建议使用 [setDisplayMode()]")
        fun setEnableTouch(isEnable: Boolean): T {
            displayMode = if (isEnable) {
                FxDisplayMode.Normal
            } else {
                FxDisplayMode.ClickOnly
            }
            return this as T
        }

        /**
         * 设置浮窗展示模式
         * @param mode 默认是[FxDisplayMode.Normal]
         * */
        fun setDisplayMode(mode: FxDisplayMode): T {
            this.displayMode = mode
            return this as T
        }

        /**
         * 设置启用屏幕外滚动 默认为true,即悬浮窗可以拖动到全屏任意位置(除了状态栏与导航栏禁止覆盖)
         * false时,可拖动范围受 borderMargin-边框偏移 与 moveEdge-边缘偏移 限制
         * 即可拖动范围=屏幕大小-(borderMargin+moveEdge+系统状态栏与导航栏(y轴))
         */
        fun setEnableScrollOutsideScreen(isEnable: Boolean): T {
            this.enableEdgeRebound = isEnable
            return this as T
        }

        /** 设置启用边缘自动吸附，默认启用 */
        fun setEnableEdgeAdsorption(isEnable: Boolean): T {
            this.enableEdgeAdsorption = isEnable
            return this as T
        }

        /** 设置边缘吸附方向，默认 [FxAdsorbDirection.LEFT_OR_RIGHT] */
        fun setEdgeAdsorbDirection(direction: FxAdsorbDirection): T {
            this.edgeAdsorbDirection = direction
            return this as T
        }

        /** 设置悬浮窗点击事件 [clickListener] 点击事件 [time] 重复时间-> default=500ms
         * */
        @JvmOverloads
        fun setOnClickListener(
            time: Long = 500L,
            clickListener: View.OnClickListener,
        ): T {
            this.enableClickListener = true
            this.ifxClickListener = clickListener
            this.clickTime = time
            return this as T
        }

        /** 设置悬浮窗长按事件 [ifxLongClickListener]
         * */
        fun setOnLongClickListener(listener: View.OnLongClickListener): T {
            this.enableClickListener = true
            this.ifxLongClickListener = listener
            return this as T
        }

        /**
         * 设置悬浮窗的layoutParams,即浮窗容器,非自己传递进去的用于显示的View
         *
         * 默认wrap-wrap
         *
         * ps: 不建议自行调用,此方法会影响浮窗的展示效果
         */
        fun setManagerParams(layoutParams: FrameLayout.LayoutParams): T {
            this.layoutParams = layoutParams
            return this as T
        }

        /** 设置边缘吸附的偏移量 */
        fun setEdgeOffset(edge: Float): T {
            this.edgeOffset = abs(edge)
            return this as T
        }

        /** 设置悬浮窗可移动位置偏移 */
        fun setBorderMargin(t: Float, l: Float, b: Float, r: Float): T {
            fxBorderMargin.apply {
                this.t = t
                this.l = l
                this.b = b
                this.r = r
            }
            return this as T
        }

        /** 设置可移动范围内相对屏幕顶部偏移量 */
        fun setTopBorderMargin(t: Float): T {
            fxBorderMargin.t = abs(t)
            return this as T
        }

        /** 设置可移动范围内相对屏幕左侧偏移量 */
        fun setLeftBorderMargin(l: Float): T {
            fxBorderMargin.l = abs(l)
            return this as T
        }

        /** 设置可移动范围内相对屏幕右侧偏移量 */
        fun setRightBorderMargin(r: Float): T {
            fxBorderMargin.r = abs(r)
            return this as T
        }

        fun setBottomBorderMargin(b: Float): T {
            fxBorderMargin.b = abs(b)
            return this as T
        }

        @JvmOverloads
        fun setEnableLog(isLog: Boolean = true, tag: String = ""): T {
            enableDebugLog = isLog
            fxLogTag = if (tag.isNotEmpty()) "-$tag" else ""
            return this as T
        }

        /** 设置默认的x坐标 */
        fun setX(x: Float): T {
            this.defaultX = x
            return this as T
        }

        /** 设置默认的y坐标 */
        fun setY(y: Float): T {
            this.defaultY = y
            return this as T
        }

        /** 设置默认x,y坐标 */
        fun setXY(x: Float, y: Float): T {
            this.defaultX = x
            this.defaultY = y
            return this as T
        }

        /**
         * 调用此方法,将忽视传递的(x,y)。 浮窗的坐标将根据 传递进来的 [gravity] + 此方法传入的偏移量
         * 计算，而非直接坐标。 这样的好处是,你不用去关注具体浮窗坐标应该是什么，而是可以依靠参照物的方式摆放。
         * 比如默认你的浮窗在右下角，但是想增加一点在右侧偏移，此时就可以依靠此方法，将浮窗位置设置在右下角，然后增加相应方向的偏移量即可。
         */
        fun setOffsetXY(x: Float, y: Float): T {
            this.offsetX = x
            this.offsetY = y
            return this as T
        }

        /** 设置是否启用动画 */
        fun setEnableAnimation(isEnable: Boolean): T {
            enableAnimation = isEnable
            return this as T
        }

        /** 设置是否支持半隐藏
         * 系统浮窗时，目前会同时打开[setEnableSafeArea(false)]，以避免浮窗高度超出屏幕(系统行为)
         * */
        fun setEnableHalfHide(isEnable: Boolean): T {
            enableHalfHide = isEnable
            return this as T
        }

        /** 设置半隐藏比例[0:全显, 0.9:几乎全隐], 默认0.5f，设置后会开enable半隐 */
        fun setHalfHidePercent(percent: Float): T {
            setEnableHalfHide(true)
            halfHidePercent = percent.coerceIn(0f, 0.9f)
            return this as T
        }

        /**
         * 设置悬浮窗视图默认位置,默认右下角,
         *
         * 注意：此方法会影响setX()||setY()
         */
        fun setGravity(gravity: FxGravity): T {
            this.gravity = gravity
            return this as T
        }

        /**
         * 设置启用动画具体实现
         *
         * @param fxAnimation 动画的具体实现类
         * @sample [com.petterp.floatingx.app.simple.FxAnimationImpl]
         */
        fun setAnimationImpl(fxAnimation: FxAnimation): T {
            this.fxAnimation = fxAnimation
            this.enableAnimation = true
            return this as T
        }

        /** 设置悬浮窗view-lifecycle */
        @Deprecated(replaceWith = ReplaceWith("addViewLifecycle"), message = "use addViewLifecycle")
        fun setViewLifecycle(listener: IFxViewLifecycle): T {
            this.iFxViewLifecycles[0] = listener
            return this as T
        }

        /** 增加悬浮窗view-lifecycle */
        fun addViewLifecycle(listener: IFxViewLifecycle): T {
            this.iFxViewLifecycles.add(listener)
            return this as T
        }

        @Deprecated(replaceWith = ReplaceWith("setTouchListener"), message = "use setTouchListener")
        fun setScrollListener(listener: IFxTouchListener): T {
            this.iFxTouchListener = listener
            return this as T
        }

        /**
         * 设置悬浮窗view-移动监听
         */
        fun setTouchListener(listener: IFxTouchListener): T {
            this.iFxTouchListener = listener
            return this as T
        }

        /**
         * 设置存储坐标保存实现逻辑
         *
         * @param iFxConfigStorage 传入IFxConfig对象, 也可自行实现接口，自定义具体实现逻辑
         * @sample com.petterp.floatingx.app.simple.FxConfigStorageToSpImpl
         *     如果使用默认实现,需自行传入context PS: 当启用并且存在历史坐标,
         *     gravity以及自定义的x,y坐标将会失效，优先使用历史坐标
         *     如果某次边框变动或者其他影响导致原视图范围改变,现有的历史坐标位置不准确，请先移除历史坐标信息 ->
         *     即调用外部的FloatingX.clearConfig()清除历史坐标信息
         */
        fun setSaveDirectionImpl(iFxConfigStorage: IFxConfigStorage): T {
            this.enableSaveDirection = true
            this.iFxConfigStorage = iFxConfigStorage
            return this as T
        }
    }
}
