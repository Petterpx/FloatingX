package com.petterp.floatingx.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 *
 * @author petterp
 */
class LauncherReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_BOOT) {
            val intent = Intent(context, LauncherService::class.java)
            context.startService(intent)
        }
    }

    companion object {
        private const val ACTION_BOOT: String = "android.intent.action.BOOT_COMPLETED"
    }
}