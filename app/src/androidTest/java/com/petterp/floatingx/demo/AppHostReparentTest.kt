package com.petterp.floatingx.demo

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petterp.floatingx.app.attachedActivity
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.demo.pages.AppHostActivity
import com.petterp.floatingx.demo.pages.BlackActivity
import com.petterp.floatingx.demo.pages.SecondActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * spec §10「A→B→back 挂载顺序与位置」。
 *
 * AppHost 换页时把**同一个容器**从旧 Activity 的 decorView 静默挪到新的：
 * engine 状态不重来、位置由 translation 保留；被黑名单命中的页面则整体卸下，返回后恢复。
 */
@RunWith(AndroidJUnit4::class)
class AppHostReparentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(AppHostActivity::class.java)

    @Before
    fun setUp() {
        // 页面 onResume 里的 ensureApp 可能已经装了一个，先清干净再各自安装，用例之间互不影响
        uninstallAll()
        clearStoredAnchors()
    }

    @After
    fun tearDown() = uninstallAll()

    @Test
    fun position_and_state_survive_activity_switch() {
        val control = installShownAppWindow()
        onMain { control.moveTo(MOVE_X, MOVE_Y, animate = false) }
        idle()
        val before = onMainGet { control.position }

        val second = ActivityScenario.launch(SecondActivity::class.java)
        try {
            await("浮窗没有跟到第二页") { control.attachedActivity is SecondActivity }
            // 换页只是换父 view：状态与坐标都不该重来
            assertEquals(FxState.SHOWN, onMainGet { control.state })
            val onSecond = onMainGet { control.position }
            assertEquals(before.x, onSecond.x, POSITION_TOLERANCE)
            assertEquals(before.y, onSecond.y, POSITION_TOLERANCE)
        } finally {
            second.close()
        }

        await("返回后浮窗没有回到首页") { control.attachedActivity is AppHostActivity }
        assertEquals(FxState.SHOWN, onMainGet { control.state })
        val back = onMainGet { control.position }
        assertEquals(before.x, back.x, POSITION_TOLERANCE)
        assertEquals(before.y, back.y, POSITION_TOLERANCE)
    }

    @Test
    fun blacklisted_activity_detaches_and_back_restores() {
        val control = installShownAppWindow()
        // BlackActivity 继承 BaseBlackActivity，黑名单按 isInstance 命中（#221）
        val black = ActivityScenario.launch(BlackActivity::class.java)
        try {
            // 容器被卸下 → 回到 INSTALLED；show 的意图（desiredVisible）保留着
            await("黑名单页上浮窗没有被卸下") { control.state == FxState.INSTALLED }
            assertNull(onMainGet { control.attachedActivity })
        } finally {
            black.close()
        }
        await("离开黑名单页后浮窗没有恢复显示") { control.state == FxState.SHOWN }
        assertTrue(onMainGet { control.attachedActivity } is AppHostActivity)
    }

    private companion object {
        const val MOVE_X = 200f
        const val MOVE_Y = 400f

        /** 位置换算里有一次 float→int 的取整，留 1px 容差 */
        const val POSITION_TOLERANCE = 1f
    }
}
