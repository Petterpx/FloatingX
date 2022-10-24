package com.petterp.floatingx.assist.helper

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.petterp.floatingx.assist.*
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxScrollListener
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.util.FxLog
import java.lang.ref.WeakReference
import kotlin.math.abs

/** 通用构建器helper */
open class BasisHelper {
    @LayoutRes
    internal var layoutId: Int = 0
    internal var layoutView: WeakReference<View>? = null
    internal var gravity: FxGravity = FxGravity.DEFAULT
    internal var clickTime: Long = 500L
    internal var layoutParams: FrameLayout.LayoutParams? = null
    internal var fxAnimation: FxAnimation? = null

    internal var defaultY: Float = 0f
    internal var defaultX: Float = 0f
    internal var edgeOffset: Float = 0f
    internal var borderMargin: BorderMargin = BorderMargin()

    internal var enableFx: Boolean = false
    internal var enableAbsoluteFix: Boolean = false
    internal var enableEdgeAdsorption: Boolean = true
    internal var enableEdgeRebound: Boolean = true
    internal var enableAnimation: Boolean = false
    internal var enableSaveDirection: Boolean = false
    internal var enableDebugLog: Boolean = false
    internal var enableTouch: Boolean = true
    internal var enableClickListener: Boolean = false
    internal var enableAssistLocation: Boolean = false

    internal var iFxScrollListener: IFxScrollListener? = null
    internal var iFxViewLifecycle: IFxViewLifecycle? = null
    internal var iFxConfigStorage: IFxConfigStorage? = null
    internal var iFxClickListener: View.OnClickListener? = null

    internal var fxLog: FxLog? = null
    private var fxLogTag: String = ""

    /** 底部导航栏与状态栏测量高度 */
    internal var navigationBarHeight: Int = 0
    internal var statsBarHeight: Int = 0

    internal fun initLog(scope: String) {
        if (enableDebugLog) fxLog = FxLog.builder("$scope$fxLogTag")
    }

    abstract class Builder<T, B : BasisHelper> {
        private var context: Context? = null

        @LayoutRes
        private var layoutId: Int = 0
        private var layoutView: WeakReference<View>? = null
        private var gravity: FxGravity = FxGravity.RIGHT_OR_BOTTOM
        private var clickTime: Long = 500L
        private var layoutParams: FrameLayout.LayoutParams? = null
        private var fxAnimation: FxAnimation? = null

        private var defaultY: Float = 0f
        private var defaultX: Float = 0f
        private var edgeOffset: Float = 0f
        private var enableFx: Boolean = false
        private var borderMargin: BorderMargin = BorderMargin()
        private var assistLocation: BorderMargin = BorderMargin()

        private var enableAbsoluteFix: Boolean = false
        private var enableEdgeAdsorption: Boolean = true
        private var enableEdgeRebound: Boolean = true
        private var enableAnimation: Boolean = false
        private var enableDebugLog: Boolean = false
        private var fxLogTag: String = ""
        private var enableTouch: Boolean = true
        private var enableClickListener: Boolean = false
        private var enableAssistLocation: Boolean = false

        private var enableSaveDirection: Boolean = false
        private var enableDefaultSave: Boolean = false

        private var iFxConfigStorage: IFxConfigStorage? = null
        private var iFxScrollListener: IFxScrollListener? = null
        private var iFxViewLifecycle: IFxViewLifecycle? = null
        private var ifxClickListener: View.OnClickListener? = null

        protected abstract fun buildHelper(): B

        open fun build(): B =
            buildHelper().apply {
                adtSizeViewDirection()
                enableFx = this@Builder.enableFx
                layoutId = this@Builder.layoutId
                layoutView = this@Builder.layoutView
                gravity = this@Builder.gravity
                clickTime = this@Builder.clickTime
                layoutParams = this@Builder.layoutParams
                fxAnimation = this@Builder.fxAnimation

                defaultY = this@Builder.defaultY
                defaultX = this@Builder.defaultX

                edgeOffset = this@Builder.edgeOffset
                enableAbsoluteFix = this@Builder.enableAbsoluteFix
                enableEdgeAdsorption = this@Builder.enableEdgeAdsorption
                enableEdgeRebound = this@Builder.enableEdgeRebound
                enableAnimation = this@Builder.enableAnimation
                borderMargin = this@Builder.borderMargin
                enableSaveDirection = this@Builder.enableSaveDirection
                enableTouch = this@Builder.enableTouch
                enableClickListener = this@Builder.enableClickListener
                enableAssistLocation = this@Builder.enableAssistLocation

                enableDebugLog = this@Builder.enableDebugLog
                fxLogTag = this@Builder.fxLogTag

                iFxScrollListener = this@Builder.iFxScrollListener
                iFxViewLifecycle = this@Builder.iFxViewLifecycle
                iFxConfigStorage = this@Builder.iFxConfigStorage
                iFxClickListener = this@Builder.ifxClickListener
            }

        /** 设置启用fx */
        fun enableFx(): T {
            this.enableFx = true
            return this as T
        }

        @Deprecated("使用enableFx()替代show()", replaceWith = ReplaceWith("enableFx()"))
        fun show(): T {
            this.enableFx = true
            return this as T
        }

        /** 设置悬浮窗view的layout */
        fun setLayout(@LayoutRes layoutId: Int): T {
            this.layoutView?.clear()
            this.layoutView = null
            this.layoutId = layoutId
            return this as T
        }

        /** 设置悬浮窗View */
        fun setLayoutView(view: View): T {
            layoutId = 0
            this.layoutView = WeakReference(view)
            return this as T
        }

        /**
         * 是否启用触摸事件-(onTouchEvent)
         *
         * true -> 浮窗允许移动 , 并且主动消费所有onTouchEvent中的事件
         *
         * false -> ,浮窗屏蔽移动 , 事件将遵循默认传递过程，将询问其子view是否需要消费,用户可自行处理
         *
         * @param isEnable 默认true
         */
        fun setEnableTouch(isEnable: Boolean): T {
            this.enableTouch = isEnable
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

        /** 设置启用边缘自动吸附 */
        fun setEnableEdgeAdsorption(isEnable: Boolean): T {
            this.enableEdgeAdsorption = isEnable
            return this as T
        }

        /**
         * 启用位置修复 用于 onConfigurationChanged 不能被正常调用的情况下,比如特定机型 默认
         * false 启用此开关,每一次onDraw,框架都会计算当前视图是否发生大小改变，如果改变，则强行修复当前错乱的位置
         * 理论上,当屏幕旋转或者小窗模式,view会收到onConfigurationChanged 调用,框架内部会进行一次修复 ps:
         * 部分机型，在小窗模式缩放窗口大小时,并不会触发 onConfigurationChanged ps: 此方法对性能有一定影响,如果
         * onConfigurationChanged 不能正常调用,检查Activity-manifest 是否添加了以下
         * android:configChanges="orientation|screenSize"
         */
        fun setEnableAbsoluteFix(isEnable: Boolean): T {
            this.enableAbsoluteFix = isEnable
            return this as T
        }

        /** 设置悬浮窗点击事件 [clickListener] 点击事件 [time] 重复时间-> default=500ms */
        @JvmOverloads
        fun setOnClickListener(
            time: Long = 500L,
            clickListener: View.OnClickListener
        ): T {
            this.enableClickListener = true
            this.ifxClickListener = clickListener
            this.clickTime = time
            return this as T
        }

        /**
         * 设置悬浮窗的layoutParams,即浮窗容器,非自己传递进去的用于显示的View
         *
         * 默认wrap-wrap
         *
         * ps: 不建议自行调用,此方法会影响浮窗的正常滑动效果
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
            borderMargin.apply {
                this.t = t
                this.l = l
                this.b = b
                this.r = r
            }
            return this as T
        }

        /** 设置可移动范围内相对屏幕顶部偏移量 */
        fun setTopBorderMargin(t: Float): T {
            borderMargin.t = abs(t)
            return this as T
        }

        /** 设置可移动范围内相对屏幕左侧偏移量 */
        fun setLeftBorderMargin(l: Float): T {
            borderMargin.l = abs(l)
            return this as T
        }

        /** 设置可移动范围内相对屏幕右侧偏移量 */
        fun setRightBorderMargin(r: Float): T {
            borderMargin.r = abs(r)
            return this as T
        }

        fun setBottomBorderMargin(b: Float): T {
            borderMargin.b = abs(b)
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

        /**
         * 调用此方法,将忽视传递的(x,y)。 浮窗的坐标将根据 传递进来的 [gravity] + 此方法传入的偏移量
         * 计算，而非直接坐标。 这样的好处是,你不用去关注具体浮窗坐标应该是什么，而是可以依靠参照物的方式摆放。
         * 比如默认你的浮窗在右下角，但是想增加一点在右侧偏移，此时就可以依靠此方法，将浮窗位置设置在右下角，然后增加相应方向的偏移量即可。
         *
         * @param t 设置可移动范围内的相对屏幕顶部偏移量 App级别时
         *     不包含状态栏,框架会自行计算高度并减去,即顶部偏移量最终=topMargin+框架计算好的状态栏+moveEdg。
         *     需要注意,当悬浮窗插入到普通view时,框架不会考虑状态栏
         * @param b 设置可移动范围内的相对屏幕底部偏移量,不包含导航栏,框架会自行计算高度并减去
         *     即底部偏移量最终=屏幕高度-bottomMargin-框架计算好的导航栏-moveEdge。
         *     需要注意,当悬浮窗插入到普通View时，框架不会考虑底部导航栏
         * @param l 设置可移动范围内相对父容器右侧偏移量
         * @param r 设置可移动范围内相对父容器左侧偏移量
         */
        fun setEnableAssistDirection(
            t: Float = 0f,
            b: Float = 0f,
            l: Float = 0f,
            r: Float = 0f
        ): T {
            this.enableAssistLocation = true
            this.assistLocation.t = t
            this.assistLocation.b = b
            this.assistLocation.l = l
            this.assistLocation.r = r
            return this as T
        }

        /** 设置是否启用动画 */
        fun setEnableAnimation(isEnable: Boolean): T {
            enableAnimation = isEnable
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
        fun setViewLifecycle(iFxViewLifecycle: IFxViewLifecycle): T {
            this.iFxViewLifecycle = iFxViewLifecycle
            return this as T
        }

        /**
         * 设置悬浮窗view-移动监听
         *
         * @sample com.petterp.floatingx.impl.FxScrollImpl
         */
        fun setScrollListener(iFxScrollListener: IFxScrollListener): T {
            this.iFxScrollListener = iFxScrollListener
            return this as T
        }

        /**
         * 设置存储坐标保存实现逻辑
         *
         * @param [iFxConfigStorage] 传入IFxConfig对象, 也可自行实现接口，自定义具体实现逻辑
         * @sample com.petterp.floatingx.app.simple.FxConfigStorageToSpImpl
         *     如果使用默认实现,需自行传入context PS: 当启用并且存在历史坐标,
         *     gravity以及自定义的x,y坐标将会失效，优先使用历史坐标
         *     如果某次边框变动或者其他影响导致原视图范围改变,现有的历史坐标位置不准确，请先移除历史坐标信息
         *     -> 即调用外部的FloatingX.clearConfig()清除历史坐标信息
         */
        @Deprecated("此方法的调用需要确保页面固定不变,暂时不建议使用,后续会考虑更新逻辑")
        fun setSaveDirectionImpl(iFxConfigStorage: IFxConfigStorage): T {
//            this.enableSaveDirection = true
//            this.iFxConfigStorage = iFxConfigStorage
            return this as T
        }

        /** 辅助坐标的实现 采用坐标偏移位置,框架自行计算合适的x,y */
        private fun adtSizeViewDirection() {
            // 如果坐标规则不符合要求,则按照默认规则
            if (!enableAssistLocation && gravity == FxGravity.DEFAULT) return
            val edgeOffset = if (enableEdgeRebound) edgeOffset else 0f
            val b = assistLocation.b + edgeOffset + borderMargin.b
            val t = assistLocation.t + edgeOffset + borderMargin.t
            val r = assistLocation.r + edgeOffset + borderMargin.r
            val l = assistLocation.l + edgeOffset + borderMargin.l
            defaultX = 0f
            defaultY = 0f
            when (gravity) {
                FxGravity.LEFT_OR_BOTTOM -> {
                    defaultY = -b
                    defaultX = l
                }
                FxGravity.RIGHT_OR_BOTTOM -> {
                    defaultY = -b
                    defaultX = -r
                }
                FxGravity.RIGHT_OR_TOP -> {
                    defaultX = -r
                    defaultY = t
                }
                FxGravity.RIGHT_OR_CENTER -> {
                    defaultX = -r
                }
                FxGravity.LEFT_OR_CENTER -> {
                    defaultX = l
                }
                FxGravity.DEFAULT,
                FxGravity.LEFT_OR_TOP -> {
                    defaultX = l
                    defaultY = t
                }
            }
        }
    }
}
