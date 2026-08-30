package com.petterp.floatingx.system

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.system.permission.FxPermission
import com.petterp.floatingx.system.permission.FxPermissionActivity
import com.petterp.floatingx.system.permission.FxPermissionStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSettings
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class FxPermissionTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `isGranted follows Settings canDrawOverlays`() {
        ShadowSettings.setCanDrawOverlays(false)
        assertFalse(FxPermission.isGranted(app))
        ShadowSettings.setCanDrawOverlays(true)
        assertTrue(FxPermission.isGranted(app))
    }

    /**
     * 原计划用 `@Config(sdk = [22])`，但 Robolectric 4.16.1 支持的最低 sandbox 是 SDK 23
     * （`API level 22 is not available`），只能直接改写 [Build.VERSION.SDK_INT] 覆盖 M 以下分支：
     * 即使系统说没有权限，M 以下也必须恒为 true（不会走到 Settings.canDrawOverlays）。
     */
    @Test
    fun `isGranted is always true below M`() {
        ShadowSettings.setCanDrawOverlays(false)
        val real = Build.VERSION.SDK_INT
        try {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 22)
            assertTrue(FxPermission.isGranted(app))
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", real)
        }
    }

    @Test
    fun `request already granted answers immediately without starting anything`() {
        ShadowSettings.setCanDrawOverlays(true)
        var result: Boolean? = null
        FxPermission.request(app) { result = it }
        assertEquals(true, result)
        assertEquals(null, shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `request launches the transparent activity from a non activity context`() {
        ShadowSettings.setCanDrawOverlays(false)
        FxPermission.request(app) { }
        val intent = shadowOf(app).nextStartedActivity
        assertNotNull(intent)
        assertEquals(FxPermissionActivity::class.java.name, intent!!.component!!.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `activity opens the overlay settings page and reports the result to the right callback`() {
        ShadowSettings.setCanDrawOverlays(false)
        val results = mutableListOf<Pair<String, Boolean>>()
        FxPermission.request(app) { results += "a" to it }
        val launch = shadowOf(app).nextStartedActivity!!
        FxPermission.request(app) { results += "b" to it }
        shadowOf(app).nextStartedActivity // 消费掉第二个启动 intent

        val controller = Robolectric.buildActivity(FxPermissionActivity::class.java, launch).create()
        val activity = controller.get()
        val settings = shadowOf(activity).nextStartedActivityForResult
        assertNotNull(settings)
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, settings!!.intent.action)
        assertEquals("package:${app.packageName}", settings.intent.data.toString())

        ShadowSettings.setCanDrawOverlays(true)
        shadowOf(activity).receiveResult(settings.intent, Activity.RESULT_OK, null)
        assertEquals(listOf("a" to true), results) // 只回调请求 a，b 仍在等自己的 Activity
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `strategy factories are java friendly`() {
        assertTrue(FxPermissionStrategy.auto() is FxPermissionStrategy.Auto)
        assertTrue(FxPermissionStrategy.skip() is FxPermissionStrategy.Skip)
        val manual = FxPermissionStrategy.manual { it.deny() }
        assertTrue(manual is FxPermissionStrategy.Manual)
    }
}
