package com.petterp.floatingx.demo

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petterp.floatingx.app.attachedActivity
import com.petterp.floatingx.compose.FxComposeContent
import com.petterp.floatingx.compose.FxComposeOwner
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.demo.pages.ComposeActivity
import com.petterp.floatingx.demo.pages.ComposeSecondActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * spec §10「Compose owner 跨 Activity 存活」（#210/#239）。
 *
 * FxComposeOwner 归 control 所有：换页只是把容器挪个父 view，owner 不重建、也不降级，
 * 所以 `viewModel()` 拿到的实例一路不变；只有 control.cancel() 才 destroy 它。
 */
@RunWith(AndroidJUnit4::class)
class ComposeOwnerSurvivalTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ComposeActivity::class.java)

    @Before
    fun setUp() = uninstallAll()

    @After
    fun tearDown() = uninstallAll()

    @Test
    fun owner_and_viewmodel_survive_activity_switch_and_die_on_cancel() {
        val control = onMainGet { DemoWindows.installCompose(app).also { it.show() } }.awaitPositioned()
        val owner = onMainGet { control.composeOwner() }
        val viewModel = onMainGet { owner.counterViewModel() }
        assertEquals(Lifecycle.State.RESUMED, onMainGet { owner.lifecycle.currentState })

        activityRule.scenario.navigateTo(ComposeSecondActivity::class.java)
        try {
            await("compose 浮窗没有跟到第二页") { control.attachedActivity is ComposeSecondActivity }
            // 换页没有走 detach，owner 连 STARTED 都不该降到
            assertSame("owner 不该随页面重建", owner, onMainGet { control.composeOwner() })
            assertEquals(Lifecycle.State.RESUMED, onMainGet { owner.lifecycle.currentState })
            assertSame("ViewModel 不该随页面重建", viewModel, onMainGet { owner.counterViewModel() })
        } finally {
            pressBack()
        }

        await("compose 浮窗没有回到首页") { control.attachedActivity is ComposeActivity }
        assertSame(owner, onMainGet { control.composeOwner() })
        assertSame(viewModel, onMainGet { owner.counterViewModel() })
        assertEquals(Lifecycle.State.RESUMED, onMainGet { owner.lifecycle.currentState })
        assertFalse(onMainGet { owner.isDestroyed })

        // cancel 是终态：owner DESTROYED，ViewModelStore 一并清空
        onMain { control.cancel() }
        assertTrue("cancel 之后 owner 必须 destroy", onMainGet { owner.isDestroyed })
    }

    private fun FxControl.composeOwner(): FxComposeOwner = (config.content as FxComposeContent).owner

    /** 与 compose 内容里的 `viewModel()` 落在同一个 ViewModelStore、同一个默认 key */
    private fun FxComposeOwner.counterViewModel(): DemoWindows.CounterViewModel =
        ViewModelProvider.create(this)[DemoWindows.CounterViewModel::class.java]
}
