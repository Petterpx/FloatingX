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
        demoPage("系统级浮窗") {
            section("权限")
            custom(permissionView)
            button("Auto（默认）安装并显示") { DemoWindows.installSystem(application).show() }
            button("Manual：弹对话框再申请") {
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
            note("Manual 的对话框里：去开启=proceed()、降级=useFallback()、取消=deny()；本页配了 fallback，所以 deny 也会降级，不配才停在 INSTALLED。")
            button("Skip（不检查权限，type 由 customizer 决定）") {
                DemoWindows.installSystem(application, FxPermissionStrategy.skip(), fallback = false).show()
            }
            note("Skip 压根不查权限：没权限时 addView 会失败（日志 Fx-system），拿到权限后点 retryPermission() 即可恢复。")
            button("retryPermission()") {
                // 拿到权限后（或从后台回到前台）重新挂载；已降级成 AppHost 时 host 就不是 SystemHost 了
                (s.host as? SystemHost)?.retryPermission()
                    ?: DemoContent.toast(this@SystemHostActivity, "当前不是系统浮窗（已降级为 AppHost）")
            }
            button("打开系统设置页") {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }

            section("LayoutParams")
            note("customizer 在默认 LayoutParams 之后执行，type / flags / alpha 等字段都能覆盖。「alpha 0.5」按最小配置重装，想换回默认配置点上面的安装按钮。")
            button("alpha 0.5（重新安装）") { installWithAlpha() }
            button("读取当前 LP") {
                val host = s.host as? SystemHost
                if (host == null) {
                    DemoContent.toast(this@SystemHostActivity, "当前不是系统浮窗（已降级为 AppHost）")
                } else {
                    val text = host.windowLayoutParams.let { "type=${it.type} flags=${it.flags} x=${it.x} y=${it.y}" }
                    DemoContent.toast(this@SystemHostActivity, text)
                }
            }

            section("触摸")
            note("touchable 关闭后窗口加 FLAG_NOT_TOUCHABLE，触摸整体透传给下层应用。")
            toggle("touchable", true) { enabled -> s.update { gesture { touchable = enabled } } }

            section("键盘 / 返回键")
            button("安装带 EditText 的系统浮窗") { DemoWindows.installSystem(application, keyboard = true).show() }
            note("点 EditText 弹键盘；按返回收起键盘，不会触发 onBackPressed")

            section("Service")
            note("系统浮窗只要 application context，Service 里也能装（#192）。API 33+ 未授予通知权限时前台 Service 照样起得来，只是通知看不到。")
            button("从前台 Service 安装（#192）") {
                ContextCompat.startForegroundService(this@SystemHostActivity, Intent(this@SystemHostActivity, DemoService::class.java))
            }
            button("停止 Service") { stopService(Intent(this@SystemHostActivity, DemoService::class.java)) }

            section("降级")
            note("拒绝权限时看降级：默认配了 fallback(AppHost)，被拒后浮窗会自动换成 App 级实现继续显示；不配 fallback 则停在 INSTALLED，等 retryPermission()。")
            button("卸载系统浮窗") { FloatingX.uninstall(DemoWindows.TAG_SYSTEM) }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从系统设置页回来时刷新
        refreshPermission()
    }

    private fun refreshPermission() {
        permissionView.text = "悬浮窗权限：" + if (FxPermission.isGranted(this)) "已授予" else "未授予"
    }

    /** Manual 策略：三个方法只应调用一个，所以对话框不可取消，必须点一个按钮 */
    private fun showPermissionDialog(request: FxPermissionRequest) {
        AlertDialog.Builder(this)
            .setTitle("需要悬浮窗权限")
            .setMessage("Manual 策略把决定权交给业务方：可以先解释用途，再决定申请、降级还是放弃。")
            .setCancelable(false)
            .setPositiveButton("去开启") { _, _ -> request.proceed() }
            .setNeutralButton("降级") { _, _ -> request.useFallback() }
            .setNegativeButton("取消") { _, _ -> request.deny() }
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
