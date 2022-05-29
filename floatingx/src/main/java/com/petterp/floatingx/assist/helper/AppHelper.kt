package com.petterp.floatingx.assist.helper

import android.app.Activity
import com.petterp.floatingx.listener.IFxProxyTagActivityLifecycle
import com.petterp.floatingx.util.FxScopeEnum
import com.petterp.floatingx.util.navigationBarHeight
import com.petterp.floatingx.util.statusBarHeight

/** AppHelper构建器 */
class AppHelper(
    val blackList: MutableList<Class<*>>?,
    val filterList: MutableList<Class<*>>?,
    val enableAllBlackClass: Boolean,
    val fxLifecycleExpand: IFxProxyTagActivityLifecycle?
) : BasisHelper() {

    @JvmName(" updateNavigationBar")
    internal fun updateNavigationBar(activity: Activity?) {
        navigationBarHeight = activity?.navigationBarHeight ?: navigationBarHeight
        fxLog?.v("system-> navigationBar-$navigationBarHeight")
    }

    @JvmName(" updateStatsBar")
    internal fun updateStatsBar(activity: Activity?) {
        statsBarHeight = activity?.statusBarHeight ?: statsBarHeight
        fxLog?.v("system-> statusBarHeight-$statsBarHeight")
    }

    class Builder : BasisHelper.Builder<Builder, AppHelper>() {
        private var insertList: MutableList<Class<*>>? = null
        private var filterList: MutableList<Class<*>>? = null
        private var fxLifecycleExpand: IFxProxyTagActivityLifecycle? = null
        private var enableAllBlackClass: Boolean = false

        /**
         * 设置显示悬浮窗的Activity生命周期回调
         * @param tagActivityLifecycle 生命周期实现类回调
         * @sample [com.petterp.floatingx.impl.lifecycle.FxTagActivityLifecycleImpl] 空实现,便于直接object:XXX
         * */
        fun setTagActivityLifecycle(tagActivityLifecycle: IFxProxyTagActivityLifecycle): Builder {
            this.fxLifecycleExpand = tagActivityLifecycle
            return this
        }

        /** 添加允许显示悬浮窗的activity类
         * @param c 允许显示的activity类
         * */
        fun addBlackClass(vararg c: Class<out Activity>): Builder {
            if (insertList == null) insertList = mutableListOf()
            insertList?.addAll(c)
            return this
        }

        /**
         * 允许所有activity都显示全局悬浮窗,支持增加过滤列表
         * @param isEnable 默认false
         * @param filterClass 需要过滤掉的activity,即不会在以下activity中插入
         * */
        @JvmOverloads
        fun setEnableAllBlackClass(
            isEnable: Boolean,
            vararg filterClass: Class<out Activity> = emptyArray()
        ): Builder {
            if (filterClass.isNotEmpty()) {
                if (filterList == null) filterList = mutableListOf()
                filterList?.addAll(filterClass)
            }
            enableAllBlackClass = isEnable
            return this
        }

        override fun buildHelper(): AppHelper =
            AppHelper(
                insertList,
                filterList,
                enableAllBlackClass,
                fxLifecycleExpand
            )

        override fun build(): AppHelper {
            val helper = super.build()
            helper.initLog(FxScopeEnum.APP_SCOPE.tag)
            return helper
        }
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}
