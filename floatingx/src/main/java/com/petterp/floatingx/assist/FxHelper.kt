package com.petterp.floatingx.assist

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.petterp.floatingx.ext.FxDebug
import com.petterp.floatingx.ext.lazyLoad
import com.petterp.floatingx.impl.FxConfigStorageToSpImpl
import com.petterp.floatingx.impl.FxLifecycleExpand
import com.petterp.floatingx.listener.IFxAnimation
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxScrollListener
import com.petterp.floatingx.listener.IFxViewLifecycle
import kotlin.math.abs

/**
 * @Author petterp
 * @Date 2021/5/20-4:21 下午
 * @Email ShiyihuiCloud@163.com
 * @Function 配置信息
 */
@SuppressLint("StaticFieldLeak")
class FxHelper(
    @LayoutRes internal var layoutId: Int,
    internal var context: Context,
    internal var gravity: Direction,
    internal var edgeOffset: Float,
    internal var enableFx: Boolean,
    internal val enableEdgeAdsorption: Boolean,
    internal val enableScrollOutsideScreen: Boolean,
    internal val enableAbsoluteFix: Boolean,
    internal val enableAnimation: Boolean,
    internal val enableAttachDialogF: Boolean,
    internal var y: Float,
    internal var x: Float,
    internal var clickListener: ((View) -> Unit)?,
    internal var clickTime: Long,
    internal val iFxViewLifecycle: IFxViewLifecycle?,
    internal val iFxScrollListener: IFxScrollListener?,
    internal val iFxAnimation: IFxAnimation?,
    internal val fxLifecycleExpand: FxLifecycleExpand?,
    internal val iFxConfigStorage: IFxConfigStorage?,
    internal var layoutParams: FrameLayout.LayoutParams?,
    internal val blackList: MutableList<Class<*>>,
    internal val borderMargin: BorderMargin,
) {

    companion object {
        internal const val clickDefaultTime = 500L

        @JvmStatic
        fun builder() = Builder()

        fun builder(obj: Builder.() -> Unit) = Builder().apply {
            obj.invoke(this)
        }.build()
    }

    class Builder {
        @LayoutRes
        private var mLayout: Int = 0

        private var context: Context? = null
        private var gravity: Direction = Direction.LEFT_OR_TOP
        private var clickTime: Long = clickDefaultTime
        private var layoutParams: FrameLayout.LayoutParams? = null

        private var defaultY: Float = 0f
        private var defaultX: Float = 0f
        private var edgeOffset: Float = 0f
        private var enableFx: Boolean = false
        private var enableFixLocation: Boolean = false
        private var enableEdgeAdsorption: Boolean = true
        private var enableScrollOutsideScreen: Boolean = true
        private var enableAttachDialogF: Boolean = false
        private var enableAnimation: Boolean = false
        private var borderMargin: BorderMargin = BorderMargin()

        // build时配置
        private var enableDebugLog: Boolean = false
        private var enableSaveDirection: Boolean = false
        private var enableSizeViewDirection: Boolean = false

        private var iFxConfigStorage: IFxConfigStorage? = null
        private var iFxAnimation: IFxAnimation? = null
        private var iFxScrollListener: IFxScrollListener? = null
        private var iFxViewLifecycle: IFxViewLifecycle? = null
        private var ifxClickListener: ((View) -> Unit)? = null
        private var fxLifecycleExpand: FxLifecycleExpand? = null
        private val blackList by lazyLoad {
            mutableListOf<Class<*>>()
        }

        fun build(): FxHelper {
            if (context == null) throw NullPointerException("context !=null !!!")
            // 只有是Application时才允许保存,即默认全局悬浮窗时
            if (iFxConfigStorage == null && enableSaveDirection && context is Application) {
                iFxConfigStorage = FxConfigStorageToSpImpl.init(context!!)
            }
            // 如果启用了辅助坐标计算,框架根据方向与偏移自行计算x,y
            if (enableSizeViewDirection) sizeViewDirection()
            FxDebug.updateMode(enableDebugLog)
            return FxHelper(
                this.mLayout,
                context!!,
                gravity,
                edgeOffset,
                enableFx,
                enableEdgeAdsorption,
                enableScrollOutsideScreen,
                enableAnimation,
                enableFixLocation,
                enableAttachDialogF,
                defaultY,
                defaultX,
                ifxClickListener,
                clickTime,
                iFxViewLifecycle,
                iFxScrollListener,
                iFxAnimation,
                fxLifecycleExpand,
                iFxConfigStorage,
                layoutParams,
                blackList,
                borderMargin,
            )
        }

        /** 显示悬浮窗,即启用悬浮窗 */
        fun show(): Builder {
            this.enableFx = true
            return this
        }

        /** 设置context,全局悬浮窗请使用Application */
        fun setContext(context: Context): Builder {
            this.context = context
            return this
        }

        /** 设置悬浮窗view的layout */
        fun setLayout(@LayoutRes layoutId: Int): Builder {
            this.mLayout = layoutId
            return this
        }

        /** 设置启用屏幕外滚动
         * 默认为true,即悬浮窗可以拖动到全屏任意位置(除了状态栏与导航栏禁止覆盖)
         * false时,可拖动范围受 borderMargin-边框偏移 与 moveEdge-边缘偏移 限制
         *  即可拖动范围=屏幕大小-(borderMargin+moveEdge+系统状态栏与导航栏(y轴))
         * */
        fun setEnableScrollOutsideScreen(isEnable: Boolean): Builder {
            this.enableScrollOutsideScreen = isEnable
            return this
        }

        /** 设置启用边缘自动吸附 */
        fun setEnableEdgeAdsorption(isEnable: Boolean): Builder {
            this.enableEdgeAdsorption = isEnable
            return this
        }

//        /**
//         *
//         * 设置启用DialogFragment安装悬浮窗
//         * 默认禁止，在dialogFragment安装涉及到过多操作,不能保证完全适配，暂时未提供实现
//         * */
//        @Deprecated("暂时未提供实现")
//        fun setEnableAttachDialogFragment(isEnable: Boolean): Builder {
//            this.enableAttachDialogF = isEnable
//            return this
//        }

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
        fun setEnableFixLocation(isEnable: Boolean): Builder {
            this.enableFixLocation = isEnable
            return this
        }

        /** 设置悬浮窗点击事件
         * [clickListener] 点击事件
         * [time] 重复时间-> default=500ms
         * */
        @JvmOverloads
        fun setOnClickListener(
            time: Long = clickDefaultTime,
            clickListener: ((View) -> Unit),
        ): Builder {
            this.ifxClickListener = clickListener
            this.clickTime = time
            return this
        }

        /** 添加允许显示悬浮窗的activity类 */
        fun addBlackClass(vararg c: Class<out Activity>): Builder {
            blackList.addAll(c)
            return this
        }

        /** 设置悬浮窗的layoutParams */
        fun setLayoutParams(layoutParams: FrameLayout.LayoutParams): Builder {
            this.layoutParams = layoutParams
            return this
        }

        /** 设置边缘吸附的偏移量 */
        fun setEdgeOffset(edge: Float): Builder {
            this.edgeOffset = abs(edge)
            return this
        }

        /** 设置可移动范围内的相对屏幕顶部偏移量,不包含状态栏,框架会自行计算高度并减去
         * 即顶部偏移量最终=topMargin+框架计算好的状态栏+moveEdge
         * * */
        fun setTopMargin(t: Float): Builder {
            borderMargin.t = abs(t)
            return this
        }

        /** 设置可移动范围内相对屏幕左侧偏移量 */
        fun setLeftMargin(l: Float): Builder {
            borderMargin.l = abs(l)
            return this
        }

        /** 设置可移动范围内相对屏幕右侧偏移量 */
        fun setRightMargin(r: Float): Builder {
            borderMargin.r = abs(r)
            return this
        }

        /** 设置可移动范围内的相对屏幕底部偏移量,不包含导航栏,框架会自行计算高度并减去
         * 即底部偏移量最终=屏幕高度-bottomMargin-框架计算好的导航栏-moveEdge
         * */
        fun setBottomMargin(b: Float): Builder {
            borderMargin.b = abs(b)
            return this
        }

        @JvmOverloads
        fun setEnableLog(isLog: Boolean = true): Builder {
            this.enableDebugLog = isLog
            return this
        }

        /** 设置默认的x坐标,以用户可触摸视图开始，不包含系统导航栏与状态栏
         * 注意: 直接调用setX,框架将忽略对位置的优化,直接使用此x
         * */
        fun setX(x: Float): Builder {
            this.defaultX = x
            return this
        }

        /** 设置默认的y坐标,以用户可触摸视图开始，不包含系统导航栏与状态栏 */
        fun setY(y: Float): Builder {
            this.defaultY = y
            return this
        }

        /** 调用此方法,x,y的坐标将根据 传递进来的 [setGravity] 调整 相对坐标
         * 比如当gravity=LEFT_OR_BOTTOM，则相对应的(x,y) 只是相对于左下角的偏移量，而非全屏直接坐标
         * */
        fun setEnableAssistDirection(isEnable: Boolean): Builder {
            enableSizeViewDirection = isEnable
            return this
        }

        /** 设置是否启用动画 */
        fun setEnableAnimation(isEnable: Boolean): Builder {
            enableAnimation = isEnable
            return this
        }

        /** 设置悬浮窗视图默认位置，一般情况下，设置了方向，需要自行处理坐标关系,需谨慎设置 */
        fun setGravity(gravity: Direction): Builder {
            this.gravity = gravity
            return this
        }

        /**
         * 设置显示悬浮窗的Activity生命周期回调
         * */
        fun setTagActivityLifecycle(lifecycleExpand: FxLifecycleExpand): Builder {
            this.fxLifecycleExpand = lifecycleExpand
            return this
        }

        fun setTagActivityLifecycle(obj: FxLifecycleExpand.() -> Unit): Builder {
            this.fxLifecycleExpand = FxLifecycleExpand().apply(obj)
            return this
        }

        fun setAnimationListener(iFxAnimation: IFxAnimation): Builder {
            this.iFxAnimation = iFxAnimation
            return this
        }

        /** 设置悬浮窗view-lifecycle */
        fun setViewLifecycle(iFxViewLifecycle: IFxViewLifecycle): Builder {
            this.iFxViewLifecycle = iFxViewLifecycle
            return this
        }

        /** 设置悬浮窗view-移动监听 */
        fun setScrollListener(iFxScrollListener: IFxScrollListener): Builder {
            this.iFxScrollListener = iFxScrollListener
            return this
        }

        /** 设置启用存储坐标信息,存储方式默认采用 sp
         * [iFxConfigStorage] 传入IFxConfig对象, 也可自行实现接口，自定义具体实现逻辑
         *
         * PS: 当启用并且存在历史坐标, gravity以及自定义的x,y坐标将会失效，优先使用历史坐标
         * 如果某次边框变动或者其他影响导致原视图范围改变,现有的历史坐标位置不准确，请先移除历史坐标信息
         *  -> 即调用外部的FloatingX.clearConfig()清除历史坐标信息
         * */
        @JvmOverloads
        fun setEnableConfig(iFxConfigStorage: IFxConfigStorage? = null): Builder {
            this.enableSaveDirection = true
            this.iFxConfigStorage = iFxConfigStorage
            return this
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
