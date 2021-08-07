package com.petterp.floatingx.util

import android.util.Log

/**
 * Fx日志查看器,开启之后，将查看到Fx整个运行轨迹
 * */
internal class FxDebug private constructor() {

    companion object {
        private var TAG = "FloatingX"

        private var isDebug = false

        fun updateMode(isDebug: Boolean, tag: String = TAG) {
            Companion.isDebug = isDebug
            TAG = tag
        }

        fun d(message: String) {
            if (isDebug)
                Log.d(TAG, message)
        }

        fun v(message: String) {
            if (isDebug)
                Log.v(TAG, message)
        }

        fun e(message: String) {
            if (isDebug)
                Log.e(TAG, message)
        }
    }
}
