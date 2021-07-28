package com.petterp.floatingx.assist.helper

import android.app.Activity
import android.app.Application
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.FxLifecycleExpand

/**
 * @Author petterp
 * @Date 2021/7/27-10:22 PM
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
class AppHelper(
    val application: Application,
    val blackList: MutableList<Class<*>>,
    val fxLifecycleExpand: FxLifecycleExpand?
) : BaseHelper() {

    /** 获取全局静态控制器,提供这样的能力 */
//    override fun toControl() = FloatingX.init(this)

    class Builder : BaseHelper.Builder<Builder, AppHelper>() {
        private var application: Application? = null
        private var blackList = mutableListOf<Class<*>>()
        private var fxLifecycleExpand: FxLifecycleExpand? = null

        fun setContext(application: Application): Builder {
            this.application = application
            return this
        }

        /**
         * 设置显示悬浮窗的Activity生命周期回调
         * @param tagActivityLifecycle 生命周期实现类回调
         * */
        fun setTagActivityLifecycle(tagActivityLifecycle: FxLifecycleExpand.() -> Unit): Builder {
            this.fxLifecycleExpand = FxLifecycleExpand().apply(tagActivityLifecycle)
            return this
        }

        /** 添加允许显示悬浮窗的activity类
         * @param c 允许显示的activity类
         * */
        fun addBlackClass(vararg c: Class<out Activity>): Builder {
            blackList.addAll(c)
            return this
        }

        override fun buildHelper(): AppHelper =
            if (application == null) throw NullPointerException("To build AppHelper, you must set application!") else AppHelper(
                application!!, blackList, fxLifecycleExpand
            )
    }
}
