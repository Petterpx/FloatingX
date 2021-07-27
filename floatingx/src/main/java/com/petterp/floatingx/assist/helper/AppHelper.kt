package com.petterp.floatingx.assist.helper

import android.app.Application

/**
 * @Author petterp
 * @Date 2021/7/27-10:22 PM
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
class AppHelper(val application: Application, val helper: BaseHelper) : BaseHelper() {

    class AppHelperBuilder : BaseHelper.BaseHelperBuilder<AppHelperBuilder, AppHelper>() {
        private var application: Application? = null
        fun context(application: Application): AppHelperBuilder {
            this.application = application
            return this
        }

        override fun build(): AppHelper {
            val baseHelper = buildHelper()
            return AppHelper(application!!, baseHelper)
        }
    }
}
