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
    internal var enableEdgeRebound: Boolean,
    internal var enableScrollXOutsideScreen: Boolean,
    internal var enableAttachDialogF: Boolean,
    internal var y: Float,
    internal var x: Float,
    internal var clickListener: ((View) -> Unit)?,
    internal var clickTime: Long,
    internal var iFxViewLifecycle: IFxViewLifecycle?,
    internal var iFxScrollListener: IFxScrollListener?,
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
        private var enableDebugLog: Boolean = false
        private var enableSaveDirection: Boolean = false
        private var enableSizeViewDirection: Boolean = false
        private var enableEdgeRebound: Boolean = true
        private var enableEdgeAdsorption: Boolean = true
        private var enableAttachDialogF: Boolean = false
        private var borderMargin: BorderMargin = BorderMargin()

        private var iFxConfigStorage: IFxConfigStorage? = null
        private var iFxScrollListener: IFxScrollListener? = null
        private var iFxViewLifecycle: IFxViewLifecycle? = null
        private var ifxClickListener: ((View) -> Unit)? = null
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
                enableEdgeRebound,
                enableAttachDialogF,
                defaultY,
                defaultX,
                ifxClickListener,
                clickTime,
                iFxViewLifecycle,
                iFxScrollListener,
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

        /** 设置启用边缘自动吸附 */
        fun setEnableEdgeAdsorption(isEnable: Boolean): Builder {
            this.enableEdgeAdsorption = isEnable
            return this
        }

        /** 设置启用边缘回弹,y轴状态栏与底部导航栏禁止回弹 */
        fun setEnableEdgeRebound(isEnable: Boolean): Builder {
            this.enableEdgeRebound = isEnable
            return this
        }

        /**
         * todo 暂时没法处理边界
         * 设置启用DialogFragment安装悬浮窗
         * 默认禁止，在dialogFragment安装涉及到过多操作,不能保证完全适配
         * */
        fun setEnableAttachDialogFragment(isEnable: Boolean): Builder {
            this.enableAttachDialogF = isEnable
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

        /** 设置边缘吸附的距离 */
        fun setMoveEdge(edge: Float): Builder {
            this.edgeMargin = abs(edge)
            return this
        }

        /** 设置可移动范围内的top位置,不包含导航栏与状态栏,以用户视图为范围* */
        fun setTopBorder(t: Float): Builder {
            borderMargin.t = abs(t)
            return this
        }

        /** 设置可移动范围内的left位置 */
        fun setLeftBorder(l: Float): Builder {
            borderMargin.l = abs(l)
            return this
        }

        /** 设置可移动范围内的right位置 */
        fun setRightBorder(r: Float): Builder {
            borderMargin.r = abs(r)
            return this
        }

        /** 设置可移动范围内的bottom偏移量 */
        fun setBottomBorder(b: Float): Builder {
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
