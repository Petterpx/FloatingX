package com.petterp.floatingx.system.permission

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.SparseArray
import androidx.annotation.MainThread

/**
 * 悬浮窗权限：检查 + 从任意 context 申请（#192）。回调按 requestId 分发，多次并发申请互不干扰。
 *
 * 所有方法都只能在主线程调用（内部的请求表没有加锁），回调同样在主线程派发。
 */
public object FxPermission {

    internal const val EXTRA_REQUEST_ID: String = "fx_request_id"

    private val callbacks = SparseArray<FxPermissionCallback>()
    private var nextId = 1

    /** 是否已有悬浮窗权限；M 以下系统无需申请，恒为 true */
    @JvmStatic
    public fun isGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    /** 已有权限立即回调 true；否则启动透明页，结果在设置页返回后回调（主线程） */
    @JvmStatic
    @MainThread
    public fun request(context: Context, callback: FxPermissionCallback) {
        if (isGranted(context)) {
            callback.onResult(true)
            return
        }
        val id = nextId++
        callbacks.put(id, callback)
        val intent = Intent(context, FxPermissionActivity::class.java)
            .putExtra(EXTRA_REQUEST_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** 由 [FxPermissionActivity] 回调：按 id 找到对应请求并消费，未知 id（或已消费）直接忽略 */
    @MainThread
    internal fun dispatch(id: Int, granted: Boolean) {
        val cb = callbacks.get(id) ?: return
        callbacks.remove(id)
        cb.onResult(granted)
    }
}
