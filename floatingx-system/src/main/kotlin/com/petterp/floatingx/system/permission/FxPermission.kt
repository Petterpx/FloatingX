package com.petterp.floatingx.system.permission

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.util.SparseArray
import androidx.annotation.MainThread

/**
 * 悬浮窗权限：检查 + 从任意 context 申请（#192）。回调按 requestId 分发，多次并发申请互不干扰。
 *
 * 所有方法都只能在主线程调用（内部的请求表没有加锁），回调同样在主线程派发。
 */
public object FxPermission {

    internal const val EXTRA_REQUEST_ID: String = "fx_request_id"

    private const val TAG = "Fx-system"

    private val callbacks = SparseArray<FxPermissionCallback>()
    private var nextId = 1

    /** 是否已有悬浮窗权限；M 以下系统无需申请，恒为 true */
    @JvmStatic
    public fun isGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    /**
     * 已有权限立即回调 true；否则启动透明页，结果在设置页返回后回调（主线程）。
     * 透明页起不来（后台限制、组件被裁、context 不合法…）时立刻回调 false，不会留下悬空的请求。
     *
     * **Android 10（Q）起系统禁止后台启动 Activity**：应用在后台（或从 Service）调用时，
     * 这个透明页很可能**什么都不弹**——系统直接丢掉这次启动，用户毫无感知。
     * 需要后台安装浮窗时，请用 [FxPermissionStrategy.Manual] / [FxPermissionStrategy.Skip] 把申请推迟，
     * 等应用回到前台再申请（`SystemHost.retryPermission()`）。
     */
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
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 起不来就当场收口：留着回调条目只会让调用方永远等不到结果
            callbacks.remove(id)
            Log.w(TAG, "权限申请页启动失败（Q+ 后台无法启动 Activity / 组件不可用），按拒绝处理", e)
            callback.onResult(false)
        }
    }

    /** 由 [FxPermissionActivity] 回调：按 id 找到对应请求并消费，未知 id（或已消费）直接忽略 */
    @MainThread
    internal fun dispatch(id: Int, granted: Boolean) {
        val cb = callbacks.get(id) ?: return
        callbacks.remove(id)
        cb.onResult(granted)
    }
}
