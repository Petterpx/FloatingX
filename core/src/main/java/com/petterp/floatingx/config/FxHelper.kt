package com.petterp.floatingx.config

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.petterp.floatingx.ext.BarExt
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
    @LayoutRes var layoutId: Int,
    var context: Context,
    var gravity: Direction,
    var marginEdge: Float,
    var isEnable: Boolean,
    var isEdgeEnable: Boolean,
    var y: Float,
    var x: Float,
    var clickListener: ((View) -> Unit)?,
    var iFxViewLifecycle: IFxViewLifecycle?,
    var iFxScrollListener: IFxScrollListener?,
    var layoutParams: FrameLayout.LayoutParams?,
    val blackList: MutableList<Class<*>>,
    var lScrollEdge: Int,
    var tScrollEdge: Int,
    var rScrollEdge: Int,
    var bScrollEdge: Int
) {

    companion object {

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
        private var defaultY: Float = 10f
        private var defaultX: Float = defaultY
        private var marginEdge: Float = 10f
        private var isEnable: Boolean = true
        private var isEdgeEnable: Boolean = true
        private var lScrollEdge: Int = 0
        private var tScrollEdge: Int = BarExt.getStatusBarHeight()
        private var rScrollEdge: Int = 0
        private var bScrollEdge: Int = BarExt.getNavBarHeight()
        private var context: Context? = null
        private var isDefaultDirection: Boolean = false
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
            if (isEdgeEnable) defaultX = marginEdge
            if (isDefaultDirection) sizeViewPosition()
            if (context == null) throw NullPointerException("context !=null !!!")
            return FxHelper(
                this.mLayout,
                context!!,
                gravity,
                marginEdge,
                isEnable,
                isEdgeEnable,
                defaultY,
                defaultX,
                fxClickListener,
                iFxViewLifecycle,
                iFxScrollListener,
                layoutParams,
                blackList,
                lScrollEdge, tScrollEdge, rScrollEdge, bScrollEdge
            )
        }

        fun context(context: Context): Builder {
            this.context = context
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

        fun layout(@LayoutRes layoutId: Int): Builder {
            this.mLayout = layoutId
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

        fun marginEdge(edge: Float): Builder {
            this.marginEdge = edge
            return this
        }

        fun x(x: Float): Builder {
            this.defaultX = x
            return this
        }

        fun y(y: Float): Builder {
            this.defaultY = y
            return this
        }

        /** 辅助坐标配置,调用此方法，则使用辅助方向来决定默认x,y坐标
         * 即以 gravity 作为基础定位，传入的x,y都用其定位
         * */
        fun defaultDirection(gravity: Direction): Builder {
            this.gravity = gravity
            isDefaultDirection = true
            return this
        }

        private fun sizeViewPosition() {
            when (gravity) {
                Direction.LEFT_OR_BOTTOM
                -> {
                    defaultY = -abs(defaultY)
                    defaultX = abs(defaultX)
                }
                Direction.RIGHT_OR_BOTTOM -> {
                    defaultY = -abs(defaultY)
                    defaultX = -abs(defaultX)
                }
                Direction.RIGHT_OR_TOP, Direction.RIGHT_OR_CENTER -> {
                    defaultY = abs(defaultY)
                    defaultX = -abs(defaultX)
                }
                Direction.LEFT_OR_TOP, Direction.LEFT_OR_CENTER -> {
                    defaultY = abs(defaultY)
                    defaultX = abs(defaultX)
                }
            }
        }
    }
}
