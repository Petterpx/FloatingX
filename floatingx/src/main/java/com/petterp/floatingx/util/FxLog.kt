package com.petterp.floatingx.util

import android.util.Log

/**
 * Fx日志查看器,开启之后，将查看到Fx整个运行轨迹
 * */
class FxLog private constructor(private val enable: Boolean, private val tag: String) {

    companion object {
        private var TAG = "Fx"

        fun builder(enable: Boolean, tag: String) =
            FxLog(enable, "$TAG-$tag")
    }

    fun d(message: String) {
        if (!enable) return
        Log.d(tag, message)
    }

    fun v(message: String) {
        if (!enable) return
        Log.v(tag, message)
    }

    fun e(message: String) {
        if (!enable) return
        Log.e(tag, message)
    }
}
