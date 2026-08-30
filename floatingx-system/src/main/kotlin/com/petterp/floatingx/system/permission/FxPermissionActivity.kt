package com.petterp.floatingx.system.permission

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings

/**
 * 透明页：只负责打开系统"显示在其他应用上层"设置并把结果交回 [FxPermission]。
 *
 * 它跑在自己的任务栈里（启动时 `FLAG_ACTIVITY_NEW_TASK` + manifest 里 `taskAffinity=""`），
 * 拿到结果后自己 `finish()`，并用 `excludeFromRecents` 保证不出现在最近任务里。
 *
 * **不能加 `android:noHistory`**：被系统设置页覆盖（onStop）时框架会立刻 finish 掉 noHistory 页
 * （`ActivityRecord.stopIfPossible` → `finishIfPossible("stop-no-history")`），[onActivityResult]
 * 就永远不会来，调用方的回调会一直悬空。真被系统销毁时由 [onDestroy] 兜底派发。
 */
internal class FxPermissionActivity : Activity() {

    private var requestId = 0

    /** 结果是否已经交回 [FxPermission]，避免 [onDestroy] 兜底时重复派发 */
    private var dispatched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestId = intent?.getIntExtra(FxPermission.EXTRA_REQUEST_ID, 0) ?: 0
        if (savedInstanceState != null) return // 进程恢复：设置页仍在栈上，等 onActivityResult
        try {
            // InlinedApi：ACTION_MANAGE_OVERLAY_PERMISSION 是 M 引入的常量，但 M 以下 isGranted() 恒为 true，走不到这里
            @SuppressLint("InlinedApi")
            val settings = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            @Suppress("DEPRECATION")
            startActivityForResult(settings, REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            // 部分定制 ROM 没有该设置页，直接以当前真实权限状态回调
            dispatchOnce()
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE) return
        // 设置页没有 resultCode 语义，以真实权限状态为准
        dispatchOnce()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 兜底：没等到结果就被销毁（用户划掉任务栈、系统回收等），否则调用方的回调永远不会来。
        // 只在 isFinishing 时兜底——配置变更导致的销毁不派发，重建后的实例继续等 onActivityResult。
        if (isFinishing) dispatchOnce()
    }

    /** 以当前真实权限状态交回结果，只生效一次 */
    private fun dispatchOnce() {
        if (dispatched) return
        dispatched = true
        FxPermission.dispatch(requestId, FxPermission.isGranted(this))
    }

    private companion object {
        const val REQUEST_CODE = 0x5001
    }
}
