package com.petterp.floatingx.core

import android.util.Log

/** 日志接口；未配置时为 null，所有调用点都要 `logger?.d { }`，保证零开销 */
public interface FxLogger {
    /** 惰性拼接，只有启用日志时才会执行 lambda */
    public fun d(message: () -> String)
    public fun e(message: String, error: Throwable? = null)
}

/** 默认 Logcat 实现，tag 形如 `Fx-<tag>` */
public class FxLogcatLogger(tag: String) : FxLogger {
    private val tag: String = if (tag.startsWith("Fx-")) tag else "Fx-$tag"
    override fun d(message: () -> String) {
        Log.d(tag, message())
    }
    override fun e(message: String, error: Throwable?) {
        Log.e(tag, message, error)
    }
}
