package com.petterp.floatingx.ext

import android.util.Log

/**
 * @Author petterp
 * @Date 2021/5/31-3:08 下午
 * @Email ShiyihuiCloud@163.com
 * @Function Fx日志查看器,开启之后，将查看到Fx整个运行轨迹
 */
internal class FxDebug private constructor() {

    companion object {
        private const val TAG = "FloatingX"

        private var isDebug = false

        fun updateMode(isDebug: Boolean) {
            FxDebug.isDebug = isDebug
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
