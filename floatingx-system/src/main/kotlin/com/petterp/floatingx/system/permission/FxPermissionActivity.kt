package com.petterp.floatingx.system.permission

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings

/** 透明页：只负责打开系统"显示在其他应用上层"设置并把结果交回 FxPermission。manifest 里 noHistory + excludeFromRecents */
internal class FxPermissionActivity : Activity() {

    private var requestId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestId = intent?.getIntExtra(FxPermission.EXTRA_REQUEST_ID, 0) ?: 0
        if (savedInstanceState != null) return // 进程恢复：设置页仍在栈上，等 onActivityResult
        try {
            val settings = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            @Suppress("DEPRECATION")
            startActivityForResult(settings, REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            // 部分定制 ROM 没有该设置页，直接以当前真实权限状态回调
            FxPermission.dispatch(requestId, FxPermission.isGranted(this))
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE) return
        // 设置页没有 resultCode 语义，以真实权限状态为准
        FxPermission.dispatch(requestId, FxPermission.isGranted(this))
        finish()
    }

    private companion object {
        const val REQUEST_CODE = 0x5001
    }
}
