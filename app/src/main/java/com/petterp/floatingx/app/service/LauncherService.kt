package com.petterp.floatingx.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 *
 * @author petterp
 */
import com.petterp.floatingx.app.kotlin.FxSystemSimple

class LauncherService : Service() {

    override fun onCreate() {
        super.onCreate()
        FxSystemSimple.install(application)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}