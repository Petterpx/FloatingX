package com.petterp.floatingx.demo

import android.content.pm.ActivityInfo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.petterp.floatingx.app.attachedActivity
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.demo.pages.AppHostActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * spec §10「旋转」。
 *
 * demo 的页面一律不配 configChanges，所以旋转 = Activity 重建：
 * - control 归注册表所有，活得比页面久，旋转后还是同一个实例，只是挂到了新的 Activity 上；
 * - 位置的真值是锚点而不是坐标，旋转不改锚点的 gravity；
 * - 锚点按 `tag:orientation` 持久化（[com.petterp.floatingx.core.storage.FxSpStorage]），
 *   所以转回竖屏时会把竖屏那份锚点读回来，位置回到旋转前。
 */
@RunWith(AndroidJUnit4::class)
class RotationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(AppHostActivity::class.java)

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        // 页面 onResume 里的 ensureApp 可能已经装了一个，先清干净再安装，用例之间互不影响
        uninstallAll()
        clearStoredAnchors()
    }

    @After
    fun tearDown() {
        // 先把屏幕转回自然方向再收浮窗，避免把横屏状态带给下一个用例
        device.setOrientationNatural()
        device.unfreezeRotation()
        uninstallAll()
    }

    @Test
    fun control_survives_rotation_and_position_restores_on_the_way_back() {
        val control = installShownAppWindow()
        onMain { control.moveTo(MOVE_X, MOVE_Y, animate = false) }
        idle()
        val portraitPosition = onMainGet { control.position }
        val portraitGravity = onMainGet { control.anchor.gravity }
        // 只留 identityHashCode：绝不能持有旧 Activity 的引用（旋转后它就该被回收了）
        val portraitActivityId = onMainGet { System.identityHashCode(control.attachedActivity) }

        rotateTo(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        awaitReattached(control, portraitActivityId)
        assertEquals(FxState.SHOWN, onMainGet { control.state })
        assertTrue(onMainGet { control.attachedActivity } is AppHostActivity)
        // 横屏里可用区变了、坐标会被重算，但锚定的那条边不变
        assertEquals(portraitGravity, onMainGet { control.anchor.gravity })

        val landscapeActivityId = onMainGet { System.identityHashCode(control.attachedActivity) }
        rotateTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        awaitReattached(control, landscapeActivityId)
        await("转回竖屏后位置没有回到旋转前", ROTATION_TIMEOUT_MS) {
            val p = control.position
            abs(p.x - portraitPosition.x) <= POSITION_TOLERANCE &&
                abs(p.y - portraitPosition.y) <= POSITION_TOLERANCE
        }
        assertEquals(FxState.SHOWN, onMainGet { control.state })
        assertEquals(portraitGravity, onMainGet { control.anchor.gravity })
        val restored = onMainGet { control.position }
        assertEquals(portraitPosition.x, restored.x, POSITION_TOLERANCE)
        assertEquals(portraitPosition.y, restored.y, POSITION_TOLERANCE)
    }

    private fun rotateTo(orientation: Int) {
        activityRule.scenario.onActivity { it.requestedOrientation = orientation }
    }

    /** 等 Activity 重建完成并把浮窗重新挂上：新实例 + 已挂载 */
    private fun awaitReattached(control: FxControl, previousActivityId: Int) {
        await("旋转后浮窗没有挂到重建出来的 Activity 上", ROTATION_TIMEOUT_MS) {
            val activity = control.attachedActivity
            activity is AppHostActivity && System.identityHashCode(activity) != previousActivityId
        }
        control.awaitPositioned(ROTATION_TIMEOUT_MS)
        assertNotEquals(previousActivityId, onMainGet { System.identityHashCode(control.attachedActivity) })
    }

    private companion object {
        const val MOVE_X = 200f
        const val MOVE_Y = 400f
        const val POSITION_TOLERANCE = 1f
    }
}
