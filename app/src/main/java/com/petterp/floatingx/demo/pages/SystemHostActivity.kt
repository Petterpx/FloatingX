package com.petterp.floatingx.demo.pages

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.update
import com.petterp.floatingx.demo.DemoWindows
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.service.DemoService
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.demo.ui.demoPage
import com.petterp.floatingx.system.SystemHost
import com.petterp.floatingx.system.permission.FxPermission
import com.petterp.floatingx.system.permission.FxPermissionRequest
import com.petterp.floatingx.system.permission.FxPermissionStrategy
import com.petterp.floatingx.system.systemHost
import java.lang.ref.WeakReference

/** 系统级浮窗页：三种权限策略、LayoutParams 定制、键盘/返回键、前台 Service 安装（#192） */
class SystemHostActivity : AppCompatActivity() {

    /** 每次取都保证浮窗存在；被 uninstall 之后再点其它按钮会按默认配置重新安装 */
    private val s: FxControl get() = DemoWindows.ensureSystem(application)

    private lateinit var permissionView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionView = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.DKGRAY)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 4, pad, pad / 2)
        }
        demoPage(R.string.page_system_host_title) {
            section(R.string.section_permission)
            custom(permissionView)
            button(R.string.btn_perm_auto) { DemoWindows.installSystem(application).show() }
            button(R.string.btn_perm_manual) {
                // strategy 会被 SystemHost 一直持有，直接捕获 Activity 会泄漏，这里走弱引用
                val ref = WeakReference(this@SystemHostActivity)
                DemoWindows.installSystem(
                    application,
                    FxPermissionStrategy.manual { request ->
                        val activity = ref.get()
                        if (activity == null) request.deny() else activity.showPermissionDialog(request)
                    },
                ).show()
            }
            note(R.string.note_perm_manual)
            button(R.string.btn_perm_skip) {
                DemoWindows.installSystem(application, FxPermissionStrategy.skip(), fallback = false).show()
            }
            note(R.string.note_perm_skip)
            button(R.string.btn_retry_permission) {
                // 拿到权限后（或从后台回到前台）重新挂载；已降级成 AppHost 时 host 就不是 SystemHost 了
                (s.host as? SystemHost)?.retryPermission()
                    ?: DemoContent.toast(this@SystemHostActivity, R.string.toast_not_system_host)
            }
            button(R.string.btn_open_settings) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }

            section(R.string.section_layout_params)
            note(R.string.note_layout_params)
            button(R.string.btn_alpha_half) { installWithAlpha() }
            button(R.string.btn_read_lp) {
                val host = s.host as? SystemHost
                if (host == null) {
                    DemoContent.toast(this@SystemHostActivity, R.string.toast_not_system_host)
                } else {
                    val text = host.windowLayoutParams.let { "type=${it.type} flags=${it.flags} x=${it.x} y=${it.y}" }
                    DemoContent.toast(this@SystemHostActivity, text)
                }
            }

            section(R.string.section_touch)
            note(R.string.note_touchable_system)
            toggle(R.string.toggle_touchable, true) { enabled -> s.update { gesture { touchable = enabled } } }

            section(R.string.section_keyboard_back)
            button(R.string.btn_install_keyboard_window) { DemoWindows.installSystem(application, keyboard = true).show() }
            note(R.string.note_keyboard)

            section(R.string.section_service)
            note(R.string.note_service)
            button(R.string.btn_install_from_service) {
                ContextCompat.startForegroundService(this@SystemHostActivity, Intent(this@SystemHostActivity, DemoService::class.java))
            }
            button(R.string.btn_stop_service) { stopService(Intent(this@SystemHostActivity, DemoService::class.java)) }

            section(R.string.section_fallback)
            note(R.string.note_fallback)
            button(R.string.btn_uninstall_system) { FloatingX.uninstall(DemoWindows.TAG_SYSTEM) }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从系统设置页回来时刷新
        refreshPermission()
    }

    private fun refreshPermission() {
        val state = getString(if (FxPermission.isGranted(this)) R.string.state_granted else R.string.state_not_granted)
        permissionView.text = getString(R.string.label_overlay_permission, state)
    }

    /** Manual 策略：三个方法只应调用一个，所以对话框不可取消，必须点一个按钮 */
    private fun showPermissionDialog(request: FxPermissionRequest) {
        // 页面正在销毁时弹不出对话框，request 会永远悬着：直接 deny()（停在 INSTALLED，可 retryPermission 恢复）
        if (isFinishing || isDestroyed) {
            request.deny()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_permission_title)
            .setMessage(R.string.dialog_permission_message)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_permission_positive) { _, _ -> request.proceed() }
            .setNeutralButton(R.string.dialog_permission_neutral) { _, _ -> request.useFallback() }
            .setNegativeButton(R.string.dialog_permission_negative) { _, _ -> request.deny() }
            .show()
    }

    /** 直接安装到同一个 tag：演示 layoutParams customizer（这里只改 alpha） */
    private fun installWithAlpha() {
        FloatingX.install(DemoWindows.TAG_SYSTEM) {
            view { ctx -> DemoContent.card(ctx, "Sys") }
            anchor(FxGravity.TOP_START, dx = 24f, dy = 200f)
            enableLog("Fx-demo")
            systemHost(application) {
                theme(R.style.Theme_FloatingX)
                layoutParams { it.alpha = 0.5f }
            }
        }.show()
    }
}
