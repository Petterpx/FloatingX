package com.petterp.floatingx.util

import android.util.Log

/**
 * Fx日志查看器,开启之后，将查看到Fx整个运行轨迹
 * */
class FxLog private constructor(private val tag: String) {

    companion object {
        private var TAG = "FloatingX"

        fun builder(tag: String) =
            FxLog("$TAG-$tag")
    }

    fun d(message: String) {
        Log.d(tag, message)
    }

    fun v(message: String) {
        Log.v(tag, message)
    }

    fun e(message: String) {
        Log.e(tag, message)
    }
}
