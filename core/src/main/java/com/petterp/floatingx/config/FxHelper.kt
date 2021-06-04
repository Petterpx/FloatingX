package com.petterp.floatingx.config

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
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
    var lScrollEdge: Float,
    var tScrollEdge: Float,
    var rScrollEdge: Float,
    var bScrollEdge: Float
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
        private var defaultY: Float = 0f
        private var defaultX: Float = 0f
        private var edgeMargin: Float = 0f
        private var isEnable: Boolean = true
        private var isEdgeEnable: Boolean = true
        private var lBorderMargin: Float = 0f
        private var tBorderMargin: Float = 0f
        private var rBorderMargin: Float = 0f
        private var bBorderMargin: Float = 0f
        private var context: Context? = null
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
            sizeViewDirection()
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
                lBorderMargin, tBorderMargin, rBorderMargin, bBorderMargin
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

        fun moveEdge(edge: Float): Builder {
            this.edgeMargin = abs(edge)
            return this
        }

        fun tBorder(t: Float): Builder {
            this.tBorderMargin = abs(t)
            return this
        }

        fun lBorder(l: Float): Builder {
            this.lBorderMargin = abs(l)
            return this
        }

        fun rBorder(r: Float): Builder {
            this.rBorderMargin = abs(r)
            return this
        }

        fun bBorder(b: Float): Builder {
            this.bBorderMargin = abs(b)
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

        /** 辅助坐标配置,调用此方法，则使用辅助方向来决定默认x,y坐标
         * 即以 gravity 作为基础定位，传入的x,y都用其定位
         * */
        fun gravity(gravity: Direction): Builder {
            this.gravity = gravity
            return this
        }

        private fun sizeViewDirection() {
            val marginEdgeTox = defaultX + edgeMargin
            val marginEdgeToy = defaultY + edgeMargin
            when (gravity) {
                Direction.LEFT_OR_BOTTOM -> {
                    defaultY = -(marginEdgeToy + bBorderMargin)
                    defaultX = marginEdgeTox + lBorderMargin
                }
                Direction.RIGHT_OR_BOTTOM -> {
                    defaultY = -(marginEdgeToy + bBorderMargin)
                    defaultX = -(marginEdgeTox + rBorderMargin)
                }
                Direction.RIGHT_OR_TOP, Direction.RIGHT_OR_CENTER -> {
                    defaultX = -(marginEdgeTox + rBorderMargin)
                }
                Direction.LEFT_OR_TOP, Direction.LEFT_OR_CENTER -> {
                    defaultX = marginEdgeTox + lBorderMargin
                }
            }
        }
    }
}
