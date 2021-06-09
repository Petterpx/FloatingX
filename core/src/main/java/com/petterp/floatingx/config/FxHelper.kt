package com.petterp.floatingx.config

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
    internal var marginEdge: Float,
    internal var enableFx: Boolean,
    internal var enableEdgeAdsorption: Boolean,
    internal var enableScrollOutsideScreen: Boolean,
    internal var enableAbsoluteFix: Boolean,
    internal var enableAttachDialogF: Boolean,
    internal var y: Float,
    internal var x: Float,
    internal var clickListener: ((View) -> Unit)?,
    internal var clickTime: Long,
    internal var iFxViewLifecycle: IFxViewLifecycle?,
    internal var iFxScrollListener: IFxScrollListener?,
    internal var fxLifecycleExpand: FxLifecycleExpand?,
    internal var layoutParams: FrameLayout.LayoutParams?,
    internal val blackList: MutableList<Class<*>>,
    internal val borderMargin: BorderMargin,
    internal var iFxConfigStorage: IFxConfigStorage?
) {

    companion object {
        internal const val clickDefaultTime = 500L

        @JvmStatic
        fun builder() = Builder()

        // kt->dsl写法
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
        private var edgeMargin: Float = 0f
        private var enableFx: Boolean = false
        private var enableFixLocation: Boolean = false
        private var enableEdgeAdsorption: Boolean = true
        private var enableScrollOutsideScreen: Boolean = true
        private var enableAttachDialogF: Boolean = false
        private var borderMargin: BorderMargin = BorderMargin()

        // build时配置
        private var enableDebugLog: Boolean = false
        private var enableSaveDirection: Boolean = false
        private var enableSizeViewDirection: Boolean = false

        private var iFxConfigStorage: IFxConfigStorage? = null
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
            if (enableSizeViewDirection) sizeViewDirection()
            FxDebug.updateMode(enableDebugLog)
            return FxHelper(
                this.mLayout,
                context!!,
                gravity,
                edgeMargin,
                enableFx,
                enableEdgeAdsorption,
                enableScrollOutsideScreen,
                enableFixLocation,
                enableAttachDialogF,
                defaultY,
                defaultX,
                ifxClickListener,
                clickTime,
                iFxViewLifecycle,
                iFxScrollListener,
                fxLifecycleExpand,
                layoutParams,
                blackList,
                borderMargin, iFxConfigStorage
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

        /** 设置启用屏幕外滚动
         * 默认为true,即悬浮窗可以拖动到全屏任意位置(除了状态栏与导航栏禁止覆盖)
         * false时,可拖动范围受borderMargin与moveEdge限制
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

        /**
         * todo 暂时没法处理边界
         * 设置启用DialogFragment安装悬浮窗
         * 默认禁止，在dialogFragment安装涉及到过多操作,不能保证完全适配，暂时未提供实现
         * */
        fun setEnableAttachDialogFragment(isEnable: Boolean): Builder {
            this.enableAttachDialogF = isEnable
            return this
        }

        /**
         * 启用位置修复
         * 用于 onConfigurationChanged 不能被正常调用的情况下
         * 默认 false
         * 启用此开关,每一次onDraw,框架都会计算当前视图是否发生大小改变，如果改变，则强行修复当前错乱的位置
         * 理论上,当屏幕旋转或者小窗模式,view会收到onConfigurationChanged 调用,框架内部会进行一次修复
         * PS: 此方法对性能有所影响,如果 onConfigurationChanged 不能正常调用,检查Activity-manifest 是否添加了以下
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
        fun setMoveEdgeMargin(edge: Float): Builder {
            this.edgeMargin = abs(edge)
            return this
        }

        /** 设置可移动范围内的相对屏幕顶部偏移量,不包含状态栏,框架会自行计算高度并减去
         * 即顶部偏移量最终=topMargin+框架计算好的状态栏+moveEdge
         * * */
        fun setTopMargin(t: Float): Builder {
            borderMargin.t = abs(t)
            return this
        }

        /** 设置可移动范围内的相对屏幕左侧偏移量 */
        fun setLeftMargin(l: Float): Builder {
            borderMargin.l = abs(l)
            return this
        }

        /** 设置可移动范围内的right位置 */
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

        /** 设置默认的x坐标,以用户可触摸视图开始，不包含系统导航栏与状态栏 */
        fun setX(x: Float): Builder {
            this.defaultX = abs(x)
            return this
        }

        /** 设置默认的y坐标,以用户可触摸视图开始，不包含系统导航栏与状态栏 */
        fun setY(y: Float): Builder {
            this.defaultY = abs(y)
            return this
        }

        /** 调用此方法,x,y的坐标将根据 传递进来的 [setGravity] 调整 相对坐标
         * 比如当gravity=LEFT_OR_BOTTOM，则相对应的(x,y) 只是相对于左下角的偏移量，而非[全屏]直接坐标
         * */
        fun setEnableAssistDirection(isEnable: Boolean): Builder {
            enableSizeViewDirection = isEnable
            return this
        }

        /** 设置视图默认位置，一般情况下，如果不使用辅助定位，此方法应避免调用，否则需要自行处理坐标 */
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
            val marginEdgeTox = defaultX + edgeMargin
            val marginEdgeToy = defaultY + edgeMargin
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
