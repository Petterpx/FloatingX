package com.petterp.floatingx.ext

import android.util.Log
import com.petterp.floatingx.BuildConfig

/**
 * @Author petterp
 * @Date 2021/5/31-3:08 下午
 * @Email ShiyihuiCloud@163.com
 * @Function Fx日志查看器
 */
object FxDebug {

    private const val TAG = "FloatingX"

    var isDebug = BuildConfig.DEBUG

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
