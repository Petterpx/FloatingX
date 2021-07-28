package com.petterp.floatingx.assist.helper

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.petterp.floatingx.assist.*
import com.petterp.floatingx.assist.FxHelper
import com.petterp.floatingx.impl.simple.FxConfigStorageToSpImpl
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxScrollListener
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.util.FxScopeEnum
import kotlin.math.abs

/**
 * @Author petterp
 * @Date 2021/7/27-10:07 PM
 * @Email ShiyihuiCloud@163.com
 * @Function 通用构建器helper
 */
open class BaseHelper {
    @LayoutRes
    var layoutId: Int = 0
    var gravity: Direction = Direction.LEFT_OR_TOP
    var scopeEnum: FxScopeEnum = FxScopeEnum.APP_SCOPE
    var clickTime: Long = FxHelper.clickDefaultTime
    var layoutParams: FrameLayout.LayoutParams? = null
    var fxAnimation: FxAnimation? = null

    var defaultY: Float = 0f
    var defaultX: Float = 0f
    var edgeOffset: Float = 0f
    var borderMargin: BorderMargin = BorderMargin()

    var enableFx: Boolean = false
    var enableFixLocation: Boolean = false
    var enableAbsoluteFix: Boolean = false
    var enableEdgeAdsorption: Boolean = true
    var enableScrollOutsideScreen: Boolean = true
    var enableAnimation: Boolean = false
    var enableSaveDirection: Boolean = false
    var enableDebugLog: Boolean = false
    var enableSizeViewDirection: Boolean = false

    var iFxScrollListener: IFxScrollListener? = null
    var iFxViewLifecycle: IFxViewLifecycle? = null
    var iFxConfigStorage: IFxConfigStorage? = null
    var clickListener: ((View) -> Unit)? = null

    abstract class Builder<T, B : BaseHelper> {
        private var context: Context? = null

        @LayoutRes
        private var layoutId: Int = 0
        private var gravity: Direction = Direction.LEFT_OR_TOP
        private var scopeEnum: FxScopeEnum = FxScopeEnum.APP_SCOPE
        private var clickTime: Long = FxHelper.clickDefaultTime
        private var layoutParams: FrameLayout.LayoutParams? = null
        private var fxAnimation: FxAnimation? = null

        private var defaultY: Float = 0f
        private var defaultX: Float = 0f
        private var edgeOffset: Float = 0f
        private var enableFx: Boolean = false
        private var borderMargin: BorderMargin = BorderMargin()

        private var enableFixLocation: Boolean = false
        private var enableEdgeAdsorption: Boolean = true
        private var enableScrollOutsideScreen: Boolean = true
        private var enableAnimation: Boolean = false
        private var enableDebugLog: Boolean = false
        private var enableSizeViewDirection: Boolean = false

        private var enableSaveDirection: Boolean = false
        private var enableDefaultSave: Boolean = false

        private var iFxConfigStorage: IFxConfigStorage? = null
        private var iFxScrollListener: IFxScrollListener? = null
        private var iFxViewLifecycle: IFxViewLifecycle? = null
        private var ifxClickListener: ((View) -> Unit)? = null

        protected abstract fun buildHelper(): B

        open fun build(): B =
            buildHelper().apply {
                enableFx = this@Builder.enableFx

                layoutId = this@Builder.layoutId
                gravity = this@Builder.gravity
                scopeEnum = this@Builder.scopeEnum
                clickTime = this@Builder.clickTime
                layoutParams = this@Builder.layoutParams
                fxAnimation = this@Builder.fxAnimation

                defaultY = this@Builder.defaultY
                defaultX = this@Builder.defaultX

                edgeOffset = this@Builder.edgeOffset
                enableFixLocation = this@Builder.enableFixLocation
                enableEdgeAdsorption = this@Builder.enableEdgeAdsorption
                enableScrollOutsideScreen = this@Builder.enableScrollOutsideScreen
                enableAnimation = this@Builder.enableAnimation
                borderMargin = this@Builder.borderMargin

                enableDebugLog = this@Builder.enableDebugLog
                enableSizeViewDirection = this@Builder.enableSizeViewDirection

                iFxScrollListener = this@Builder.iFxScrollListener
                iFxViewLifecycle = this@Builder.iFxViewLifecycle
                clickListener = this@Builder.ifxClickListener
            }

        /** 设置作用域 */
        fun setScopeType(type: FxScopeEnum): T {
            this.scopeEnum = type
            return this as T
        }

        /** 设置悬浮窗view的layout */
        fun setLayout(@LayoutRes layoutId: Int): T {
            this.layoutId = layoutId
            return this as T
        }

        /** 设置启用屏幕外滚动
         * 默认为true,即悬浮窗可以拖动到全屏任意位置(除了状态栏与导航栏禁止覆盖)
         * false时,可拖动范围受 borderMargin-边框偏移 与 moveEdge-边缘偏移 限制
         *  即可拖动范围=屏幕大小-(borderMargin+moveEdge+系统状态栏与导航栏(y轴))
         * */
        fun setEnableScrollOutsideScreen(isEnable: Boolean): T {
            this.enableScrollOutsideScreen = isEnable
            return this as T
        }

        /** 设置启用边缘自动吸附 */
        fun setEnableEdgeAdsorption(isEnable: Boolean): T {
            this.enableEdgeAdsorption = isEnable
            return this as T
        }

        /**
         * 启用位置修复
         * 用于 onConfigurationChanged 不能被正常调用的情况下,比如特定机型
         * 默认 false
         * 启用此开关,每一次onDraw,框架都会计算当前视图是否发生大小改变，如果改变，则强行修复当前错乱的位置
         * 理论上,当屏幕旋转或者小窗模式,view会收到onConfigurationChanged 调用,框架内部会进行一次修复
         * ps: 部分机型，在小窗模式缩放窗口大小时,并不会触发 onConfigurationChanged
         * ps: 此方法对性能有一定影响,如果 onConfigurationChanged 不能正常调用,检查Activity-manifest 是否添加了以下
         *    android:configChanges="orientation|screenSize"
         * */
        fun setEnableFixLocation(isEnable: Boolean): T {
            this.enableFixLocation = isEnable
            return this as T
        }

        /** 设置悬浮窗点击事件
         * [clickListener] 点击事件
         * [time] 重复时间-> default=500ms
         * */
        @JvmOverloads
        fun setOnClickListener(
            time: Long = FxHelper.clickDefaultTime,
            clickListener: ((View) -> Unit),
        ): T {
            this.ifxClickListener = clickListener
            this.clickTime = time
            return this as T
        }

        /** 设置悬浮窗的layoutParams */
        fun setLayoutParams(layoutParams: FrameLayout.LayoutParams): T {
            this.layoutParams = layoutParams
            return this as T
        }

        /** 设置边缘吸附的偏移量 */
        fun setEdgeOffset(edge: Float): T {
            this.edgeOffset = abs(edge)
            return this as T
        }

        /** 设置可移动范围内的相对屏幕顶部偏移量,不包含状态栏,框架会自行计算高度并减去
         * 即顶部偏移量最终=topMargin+框架计算好的状态栏+moveEdge
         * * */
        fun setTopMargin(t: Float): T {
            borderMargin.t = abs(t)
            return this as T
        }

        /** 设置可移动范围内相对屏幕左侧偏移量 */
        fun setLeftMargin(l: Float): T {
            borderMargin.l = abs(l)
            return this as T
        }

        /** 设置可移动范围内相对屏幕右侧偏移量 */
        fun setRightMargin(r: Float): T {
            borderMargin.r = abs(r)
            return this as T
        }

        /** 设置可移动范围内的相对屏幕底部偏移量,不包含导航栏,框架会自行计算高度并减去
         * 即底部偏移量最终=屏幕高度-bottomMargin-框架计算好的导航栏-moveEdge
         * */
        fun setBottomMargin(b: Float): T {
            borderMargin.b = abs(b)
            return this as T
        }

        @JvmOverloads
        fun setEnableLog(isLog: Boolean = true): T {
            this.enableDebugLog = isLog
            return this as T
        }

        /** 设置默认的x坐标,以用户可触摸视图开始，不包含系统导航栏与状态栏
         * 注意: 直接调用setX,框架将忽略对位置的优化,直接使用此x
         * */
        fun setX(x: Float): T {
            this.defaultX = x
            return this as T
        }

        /** 设置默认的y坐标,以用户可触摸视图开始，不包含系统导航栏与状态栏 */
        fun setY(y: Float): T {
            this.defaultY = y
            return this as T
        }

        /** 调用此方法,x,y的坐标将根据 传递进来的 [setGravity] 调整 相对坐标
         * 比如当gravity=LEFT_OR_BOTTOM，则相对应的(x,y) 只是相对于左下角的偏移量，而非全屏直接坐标
         * */
        fun setEnableAssistDirection(isEnable: Boolean): T {
            enableSizeViewDirection = isEnable
            return this as T
        }

        /** 设置是否启用动画 */
        fun setEnableAnimation(isEnable: Boolean): T {
            enableAnimation = isEnable
            return this as T
        }

        /** 设置悬浮窗视图默认位置，一般情况下，设置了方向，需要自行处理坐标关系,需谨慎设置 */
        fun setGravity(gravity: Direction): T {
            this.gravity = gravity
            return this as T
        }

        fun setAnimationListener(fxAnimation: FxAnimation): T {
            this.fxAnimation = fxAnimation
            this.enableAnimation = true
            return this as T
        }

        /** 设置悬浮窗view-lifecycle */
        fun setViewLifecycle(iFxViewLifecycle: IFxViewLifecycle): T {
            this.iFxViewLifecycle = iFxViewLifecycle
            return this as T
        }

        /** 设置悬浮窗view-移动监听 */
        fun setScrollListener(iFxScrollListener: IFxScrollListener): T {
            this.iFxScrollListener = iFxScrollListener
            return this as T
        }

        /** 设置存储坐标保存实现逻辑
         * @param [iFxConfigStorage] 传入IFxConfig对象, 也可自行实现接口，自定义具体实现逻辑
         * @sample FxConfigStorageToSpImpl 如果使用默认实现,需自行传入context
         * PS: 当启用并且存在历史坐标, gravity以及自定义的x,y坐标将会失效，优先使用历史坐标
         * 如果某次边框变动或者其他影响导致原视图范围改变,现有的历史坐标位置不准确，请先移除历史坐标信息
         *  -> 即调用外部的FloatingX.clearConfig()清除历史坐标信息
         * */
        @JvmOverloads
        fun setSaveDirectionImpl(iFxConfigStorage: IFxConfigStorage? = null): T {
            this.enableSaveDirection = true
            this.iFxConfigStorage = iFxConfigStorage
            return this as T
        }

        /**
         * 使用默认实现保存位置
         * */
        fun defaultSaveDirection(context: Context): T {
            this.enableSaveDirection = true
            iFxConfigStorage = FxConfigStorageToSpImpl.init(context)
            return this as T
        }

        /** 辅助坐标的实现
         * 采用相对坐标位置,框架自行计算合适的x,y */
        private fun sizeViewDirection() {
            defaultX = abs(defaultX)
            defaultY = abs(defaultY)
            val marginEdgeTox = defaultX + edgeOffset
            val marginEdgeToy = defaultY + edgeOffset
            when (gravity) {
                Direction.LEFT_OR_BOTTOM -> {
                    defaultY = -(marginEdgeToy + borderMargin.b)
                    defaultX = marginEdgeTox + borderMargin.l
                }
                Direction.RIGHT_OR_BOTTOM -> {
                    defaultY = -(marginEdgeToy + borderMargin.b)
                    defaultX = -(marginEdgeTox + borderMargin.r)
                }
                Direction.RIGHT_OR_TOP, Direction.RIGHT_OR_CENTER -> {
                    defaultX = -(marginEdgeTox + borderMargin.r)
                }
                Direction.LEFT_OR_TOP, Direction.LEFT_OR_CENTER -> {
                    defaultX = marginEdgeTox + borderMargin.l
                }
            }
        }
    }
}
