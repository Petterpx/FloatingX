package com.petterp.floatingx.demo

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import org.junit.Assume.assumeTrue

/**
 * instrumentation 用例共用的小工具。
 *
 * 三条纪律：
 * 1. FloatingX 的写操作必须在主线程调用（[com.petterp.floatingx.core.internal.FxControlImpl] 的 mutator 会 check）；
 *    getter 虽然没有 check，但在别的线程读可能读到半帧状态，所以读写一律走 [onMain] / [onMainGet]；
 * 2. 换页、旋转、布局都是异步的，断言前用 [await] 等条件成立，而不是裸 sleep 一个固定时长；
 * 3. 二级跳页用 [navigateTo] + [pressBack]，**不要**用 `ActivityScenario.launch` / `close` 来模拟
 *    「跳到第二页 / 返回」，原因见 [navigateTo] 的注释。
 */

val app: Application get() = ApplicationProvider.getApplicationContext()

fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

/** 在主线程取一个值（读 position / state / attachedActivity 都该走这里） */
fun <T> onMainGet(block: () -> T): T {
    val holder = arrayOfNulls<Any?>(1)
    onMain { holder[0] = block() }
    @Suppress("UNCHECKED_CAST")
    return holder[0] as T
}

fun idle() = InstrumentationRegistry.getInstrumentation().waitForIdleSync()

/**
 * 从 scenario 里的 Activity 用普通 Intent 跳页（同任务栈，第一页保留在返回栈里）。
 *
 * 不能用 `ActivityScenario.launch(B::class.java)` 来模拟「跳到第二页」：它发的 intent 带
 * `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`，会把任务栈清空——第一页在跳转的那一刻
 * 就被销毁了，返回栈里什么也不剩；随后的 `close()` 还会拉起测试包自己的 EmptyActivity，
 * 于是前台永远回不到第一页，「返回后…」那半段断言必然超时。
 */
fun ActivityScenario<out Activity>.navigateTo(target: Class<out Activity>) =
    onActivity { it.startActivity(Intent(it, target)) }

/** 按系统返回键，等价于用户「返回上一页」。 */
fun pressBack() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()

/** 没有悬浮窗权限时跳过（CI 用 appops 授权） */
fun assumeOverlayPermission() = assumeTrue("需要 SYSTEM_ALERT_WINDOW 权限", Settings.canDrawOverlays(app))

/**
 * 全仓唯一的轮询原语：条件在主线程求值，成立即返回，超时抛 AssertionError。
 * 这是本目录里唯一允许出现 Thread.sleep 的地方，且有上界。
 */
fun await(desc: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS, condition: () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < end) {
        if (onMainGet(condition)) return
        Thread.sleep(POLL_INTERVAL_MS)
    }
    throw AssertionError("$desc（等待 ${timeoutMs}ms 超时）")
}

/** 等到浮窗有有效位置（首帧布局完成） */
fun FxControl.awaitPositioned(timeoutMs: Long = DEFAULT_TIMEOUT_MS): FxControl {
    await("浮窗[$tag]未完成首帧布局", timeoutMs) { (contentView?.width ?: 0) > 0 }
    return this
}

/** 每个用例都从"干净的注册表"开始/结束：全局浮窗按 tag 全部 cancel */
fun uninstallAll() = onMain { FloatingX.uninstallAll() }

/** 装一个 App 级全局浮窗（[DemoWindows.TAG_APP]）并显示，等到内容完成首帧布局 */
fun installShownAppWindow(): FxControl =
    onMainGet { DemoWindows.installApp(app).also { it.show() } }.awaitPositioned()

/**
 * 清掉 [com.petterp.floatingx.core.storage.FxSpStorage] 存下来的锚点。
 * 位置是按 `tag:orientation` 持久化的，不清的话上一个用例拖到的位置会带到下一个用例里。
 */
fun clearStoredAnchors() {
    app.getSharedPreferences(ANCHOR_SP_NAME, Context.MODE_PRIVATE).edit().clear().commit()
}

/** FxSpStorage 的默认 SharedPreferences 名 */
private const val ANCHOR_SP_NAME = "floatingx_anchor"
private const val POLL_INTERVAL_MS = 16L

/** 模拟器上换页 / 旋转都可能几百毫秒起步，给足余量；条件成立就立即返回，不影响正常耗时 */
const val DEFAULT_TIMEOUT_MS = 5_000L

/** 旋转要走 Activity 重建，比换页慢得多 */
const val ROTATION_TIMEOUT_MS = 15_000L
