package com.petterp.floatingx.demo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.petterp.floatingx.demo.DemoWindows

/**
 * 从前台 Service 安装系统浮窗（#192）：3.0 的 SystemHost 只要一个 application context，
 * 不再依赖 Activity，所以 Service 里也能装。
 *
 * 注意两条系统限制：
 * 1. Android 10（Q）起后台起不了 Activity——应用退到后台时 Auto 策略的权限申请页可能什么都不弹。
 *    本示例是在前台点按钮启动 Service 的，所以能弹；真实后台场景请用 Manual/Skip 策略，
 *    回到前台后再 `SystemHost.retryPermission()`。
 * 2. Android 13（T）起通知要用户授权，没授权时 startForeground 照样成功，只是通知不显示，属预期。
 */
class DemoService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        // 浮窗归全局注册表管，Service 只负责把它装起来
        DemoWindows.ensureSystem(application).show()
        return START_NOT_STICKY
    }

    /** onDestroy 故意不 cancel 浮窗：它的生命周期属于注册表（tag=demo-system），不跟着 Service 走 */
    override fun onDestroy() {
        super.onDestroy()
    }

    private fun startAsForeground() {
        createChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("FloatingX")
            .setContentText("前台 Service 正在托管系统浮窗")
            .setOngoing(true)
            .build()
        // API 34（U）起前台 Service 必须声明类型，且要与 manifest 里的 foregroundServiceType 一致
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFY_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFY_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "FloatingX 示例", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private companion object {
        const val CHANNEL_ID = "fx"
        const val NOTIFY_ID = 1
    }
}
