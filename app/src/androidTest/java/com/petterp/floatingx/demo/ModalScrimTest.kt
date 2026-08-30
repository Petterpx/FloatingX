package com.petterp.floatingx.demo

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.update
import com.petterp.floatingx.demo.pages.ModalActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * spec §10「Layer 容器 modal scrim」（#212/#151）。
 *
 * modal 打开时 Layer 容器把落在内容之外的 DOWN 整段吃掉（下层按钮点不动），
 * 并且 dismissOnOutsideTouch 会顺手 hide 浮窗；关掉 modal 后触摸照常透传。
 */
@RunWith(AndroidJUnit4::class)
class ModalScrimTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ModalActivity::class.java)

    @Before
    fun setUp() = uninstallAll()

    @After
    fun tearDown() {
        // 局部浮窗不在注册表里，得自己收（页面 onDestroy 也会收，这里是提前兜底）
        runCatching {
            activityRule.scenario.onActivity { activity ->
                activity.modalControl?.takeIf { it.state != FxState.CANCELLED }?.cancel()
            }
        }
        uninstallAll()
    }

    @Test
    fun modal_swallows_outside_click_and_dismisses_then_passes_through_when_off() {
        onView(withText("显示")).perform(scrollTo(), click())
        val control = requireNotNull(withActivity { it.modalControl }) { "点了「显示」之后浮窗仍未创建" }
        control.awaitPositioned()
        assertTrue("浮窗应处于显示中", onMainGet { control.isShowing })
        assertEquals(0, withActivity { it.outsideClicks })

        // modal 开着：点浮窗外部的按钮 → 事件被容器吃掉，按钮的 OnClickListener 不该跑
        onView(withText(OUTSIDE_BUTTON)).perform(scrollTo(), click())
        await("dismissOnOutsideTouch 没有把浮窗隐藏") { !control.isShowing }
        assertEquals("modal 开着时下方按钮不该被点到", 0, withActivity { it.outsideClicks })

        // 关掉 modal：容器不再拦截，触摸透传回页面
        onMain { control.update { modal(false) } }
        idle()
        onView(withText(OUTSIDE_BUTTON)).perform(scrollTo(), click())
        assertEquals("关掉 modal 后下方按钮应正常收到点击", 1, withActivity { it.outsideClicks })
    }

    /** [androidx.test.core.app.ActivityScenario.onActivity] 本身就跑在主线程，不能再套 [onMain] */
    private fun <T> withActivity(block: (ModalActivity) -> T): T {
        val holder = arrayOfNulls<Any?>(1)
        activityRule.scenario.onActivity { holder[0] = block(it) }
        @Suppress("UNCHECKED_CAST")
        return holder[0] as T
    }

    private companion object {
        const val OUTSIDE_BUTTON = "modal 下方按钮"
    }
}
