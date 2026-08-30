package com.petterp.floatingx.demo

import android.view.Gravity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.demo.pages.SystemHostActivity
import com.petterp.floatingx.demo.ui.DemoContent
import com.petterp.floatingx.system.SystemHost
import com.petterp.floatingx.system.systemHost
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * spec §10「WM 窗口 resize 时 LayoutParams 序列无跳变」（#187）。
 *
 * 系统浮窗的位置不是靠 LayoutParams.x/y 记左上角，而是把锚点映射成 LayoutParams.gravity
 * （见 WindowLayoutMath）：内容变大时窗口从锚定边生长，x/y 一个数都不用改，
 * 所以不会出现"先跳到旧坐标再被修正"的抖动。
 */
@RunWith(AndroidJUnit4::class)
class SystemWindowResizeTest {

    /** 系统浮窗本身不需要 Activity，这里只是让 demo 处于前台，贴近真实使用 */
    @get:Rule
    val activityRule = ActivityScenarioRule(SystemHostActivity::class.java)

    @Before
    fun setUp() = uninstallAll()

    @After
    fun tearDown() = uninstallAll()

    @Test
    fun window_layout_params_stay_put_when_content_grows() {
        assumeOverlayPermission()

        val control = onMainGet {
            FloatingX.install(TAG) {
                view { ctx -> DemoContent.resizable(ctx) }
                // 贴右下角：内容变大时右下角不动，只往左上方向长
                anchor(FxGravity.BOTTOM_END)
                enableLog("Fx-demo")
                // 不配 fallback：权限已由 assume 保证，host 必须一直是 SystemHost
                systemHost(app) { theme(R.style.Theme_FloatingX) }
            }.also { it.show() }
        }.awaitPositioned()

        val host = onMainGet { control.host as SystemHost }
        val before = onMainGet { host.windowLayoutParams }
        val widthBefore = onMainGet { control.contentView!!.width }
        val positionBefore = onMainGet { control.position }
        assertEquals("BOTTOM_END 应映射成 BOTTOM|RIGHT", Gravity.BOTTOM or Gravity.RIGHT, before.gravity)

        onMain {
            val content = control.contentView!!
            val lp = content.layoutParams
            lp.width = content.width + DELTA_PX
            lp.height = content.height + DELTA_PX
            content.layoutParams = lp
            content.requestLayout()
        }
        await("内容宽度没有变大 ${DELTA_PX}px") { (control.contentView?.width ?: 0) == widthBefore + DELTA_PX }
        idle()

        val after = onMainGet { host.windowLayoutParams }
        // 锚定边偏移一个都没动：右下角由 WindowManager 自己保持
        assertEquals("gravity 不该变", before.gravity, after.gravity)
        assertEquals("锚定边到屏幕右边的距离不该变", before.x, after.x)
        assertEquals("锚定边到屏幕下边的距离不该变", before.y, after.y)
        // 内容左上角则相应地往左上各挪了 DELTA_PX
        val positionAfter = onMainGet { control.position }
        assertEquals(positionBefore.x - DELTA_PX, positionAfter.x, POSITION_TOLERANCE)
        assertEquals(positionBefore.y - DELTA_PX, positionAfter.y, POSITION_TOLERANCE)
    }

    private companion object {
        const val TAG = "test-system-resize"
        const val DELTA_PX = 80
        const val POSITION_TOLERANCE = 1f
    }
}
