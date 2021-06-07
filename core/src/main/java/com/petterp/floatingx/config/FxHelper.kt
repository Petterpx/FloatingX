package com.petterp.floatingx.config

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.petterp.floatingx.ext.FxDebug
import com.petterp.floatingx.ext.lazyLoad
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
    internal var isEnable: Boolean,
    internal var isEdgeEnable: Boolean,
    internal var y: Float,
    internal var x: Float,
    internal var clickListener: ((View) -> Unit)?,
    internal var iFxViewLifecycle: IFxViewLifecycle?,
    internal var iFxScrollListener: IFxScrollListener?,
    internal var layoutParams: FrameLayout.LayoutParams?,
    internal val blackList: MutableList<Class<*>>,
    internal val borderMargin: BorderMargin,
    internal var sp: SharedPreferences?
) {

    companion object {

        private const val FX_SP_NAME = "floating_x_direction"

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

        // 起始坐标
        private var defaultY: Float = 0f
        private var defaultX: Float = 0f
        private var edgeMargin: Float = 0f
        private var isEnable: Boolean = false
        private var isDebugLog: Boolean = false
        private var isSaveDirection: Boolean = false
        private var isSizeViewDirection: Boolean = false
        private var isEdgeEnable: Boolean = true
        private var borderMargin: BorderMargin = BorderMargin()
        private var context: Context? = null
        private var sp: SharedPreferences? = null
        private var gravity: Direction = Direction.LEFT_OR_TOP
        private var iFxScrollListener: IFxScrollListener? = null
        private var iFxViewLifecycle: IFxViewLifecycle? = null
        private var layoutParams: FrameLayout.LayoutParams? = null
        private var fxClickListener: ((View) -> Unit)? = null
        private val blackList by lazyLoad {
            mutableListOf<Class<*>>()
        }

        fun build(): FxHelper {
            // 当开启了自动吸附,默认x坐标=marginEdge
            if (context == null) throw NullPointerException("context !=null !!!")
            // 只有是Application时才允许保存,即默认全局悬浮窗时
            if (isSaveDirection && context is Application) sp =
                context!!.getSharedPreferences(FX_SP_NAME, MODE_PRIVATE)
            if (isSizeViewDirection) sizeViewDirection()
            FxDebug.updateMode(isDebugLog)
            return FxHelper(
                this.mLayout,
                context!!,
                gravity,
                edgeMargin,
                isEnable,
                isEdgeEnable,
                defaultY,
                defaultX,
                fxClickListener,
                iFxViewLifecycle,
                iFxScrollListener,
                layoutParams,
                blackList,
                borderMargin, sp
            )
        }

        fun context(context: Context): Builder {
            this.context = context
            return this
        }

        fun show(): Builder {
            this.isEnable = true
            return this
        }

        fun layout(@LayoutRes layoutId: Int): Builder {
            this.mLayout = layoutId
            return this
        }

        fun isEdgeEnable(isEnable: Boolean): Builder {
            this.isEdgeEnable = isEnable
            return this
        }

        fun setViewLifecycle(iFxViewLifecycle: IFxViewLifecycle): Builder {
            this.iFxViewLifecycle = iFxViewLifecycle
            return this
        }

        fun setScrollListener(iFxScrollListener: IFxScrollListener): Builder {
            this.iFxScrollListener = iFxScrollListener
            return this
        }

        fun setOnClickListener(clickListener: ((View) -> Unit)): Builder {
            this.fxClickListener = clickListener
            return this
        }

        fun addBlackClass(vararg c: Class<*>): Builder {
            blackList.addAll(c)
            return this
        }

        fun layoutParams(layoutParams: FrameLayout.LayoutParams): Builder {
            this.layoutParams = layoutParams
            return this
        }

        fun moveEdge(edge: Float): Builder {
            this.edgeMargin = abs(edge)
            return this
        }

        fun tBorder(t: Float): Builder {
            borderMargin.t = abs(t)
            return this
        }

        fun lBorder(l: Float): Builder {
            borderMargin.l = abs(l)
            return this
        }

        fun logEnable(isLog: Boolean = true): Builder {
            this.isDebugLog = isLog
            return this
        }

        fun rBorder(r: Float): Builder {
            borderMargin.r = abs(r)
            return this
        }

        fun bBorder(b: Float): Builder {
            borderMargin.b = abs(b)
            return this
        }

        fun x(x: Float): Builder {
            this.defaultX = abs(x)
            return this
        }

        fun y(y: Float): Builder {
            this.defaultY = abs(y)
            return this
        }

        /** 调用此方法,x,y的坐标将根据 传递进来的 [gravity] 调整 相对坐标
         * 比如当gravity=LEFT_OR_BOTTOM，则相对应的(x,y) 只是相对于左下角的偏移量，而非全屏直接坐标
         * */
        fun defaultSizeViewDirection(): Builder {
            isSizeViewDirection = true
            return this
        }

        fun gravity(gravity: Direction): Builder {
            this.gravity = gravity
            return this
        }

        fun saveDirectionEnable(): Builder {
            this.isSaveDirection = true
            return this
        }

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
