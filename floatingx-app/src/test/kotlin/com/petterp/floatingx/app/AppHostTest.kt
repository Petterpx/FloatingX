package com.petterp.floatingx.app

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxActivityTracker
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope
import com.petterp.floatingx.core.layout.FxGravity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AppHostTest {

    class BlackActivity : Activity()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val controllers = mutableListOf<ActivityController<out Activity>>()

    private class Counting : FxListener {
        var attach = 0
        var detach = 0
        override fun onAttach(control: FxControl) { attach++ }
        override fun onDetach(control: FxControl) { detach++ }
    }

    /** 直接数 core 收到多少次 onBoundsChanged——这正是父布局监听要过滤的东西 */
    private class BoundsCounting : FxFeature {
        var bounds = 0
        override fun onAttach(scope: FxFeatureScope) = Unit
        override fun onDetach() = Unit
        override fun onBoundsChanged() { bounds++ }
    }

    private fun content(): FxContent = FxContent.provider { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }

    private fun config(): FxConfig = FxConfig.builder(content()).anchor(FxGravity.TOP_START).build()

    /** 走完 resume + postResume：API 29+ 的挂载时机 */
    private fun <T : Activity> launch(cls: Class<T>): ActivityController<T> =
        Robolectric.buildActivity(cls).create().start().resume().postResume().visible().also { controllers += it }

    private fun decor(a: Activity): ViewGroup = a.window.decorView as ViewGroup

    private fun layoutDecor(a: Activity, w: Int = 1080, h: Int = 1920) {
        decor(a).measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
        decor(a).layout(0, 0, w, h)
    }

    private fun layerParent(control: FxControl): ViewGroup? = control.contentView?.parent?.parent as? ViewGroup

    private fun install(host: AppHost = AppHost.builder(app).build(), config: FxConfig = config()): FxControl =
        FloatingX.install("app-test", config, host)

    /** 对应线上 FxAppInitProvider 在进程启动时做的事：tracker 必须早于第一个 Activity resume 注册 */
    @Before
    fun setUp() = FxActivityTracker.init(app)

    @After
    fun tearDown() {
        FloatingX.uninstallAll()
        controllers.forEach { runCatching { it.pause().stop().destroy() } }
        controllers.clear()
    }

    @Test
    fun `binds to the current top activity`() {
        val a = launch(Activity::class.java).get()
        val control = install()
        control.show()
        assertEquals(FxState.SHOWN, control.state)
        assertSame(decor(a), layerParent(control))
        assertSame(a, (control.host as AppHost).attachedActivity)
    }

    @Test
    fun `installed before any activity attaches on first post resume`() {
        val control = install()
        control.show()
        assertEquals(FxState.INSTALLED, control.state)
        val a = launch(Activity::class.java).get()
        assertEquals(FxState.SHOWN, control.state)
        assertSame(decor(a), layerParent(control))
    }

    @Test
    fun `re-parents silently when another activity resumes`() {
        launch(Activity::class.java)
        val control = install()
        val counting = Counting().also(control::addListener)
        control.show()
        val b = launch(Activity::class.java).get()
        assertSame(decor(b), layerParent(control))
        assertEquals(FxState.SHOWN, control.state)
        // 监听器是 install 之后才加的，看不到 install 期那次 attach；静默换父又不派发任何事件，所以都是 0
        assertEquals(0, counting.attach)
        assertEquals(0, counting.detach)
        assertSame(b, (control.host as AppHost).attachedActivity)
    }

    @Test
    fun `follows back navigation and ignores destroy of a non attached activity`() {
        val ctrlA = launch(Activity::class.java)
        val control = install()
        val counting = Counting().also(control::addListener)
        control.show()
        val ctrlB = launch(Activity::class.java)
        ctrlA.resume().postResume() // 返回 A
        ctrlB.pause().stop().destroy() // B 之后才销毁
        controllers.remove(ctrlB)
        assertSame(decor(ctrlA.get()), layerParent(control))
        // 同上：install 期的 attach 监听器没看到，去 B 再回 A 全是静默换父，不派发事件
        assertEquals(0, counting.attach)
        assertEquals(0, counting.detach)
    }

    @Test
    fun `destroying the attached activity detaches until the next resume`() {
        val ctrlA = launch(Activity::class.java)
        val control = install()
        val counting = Counting().also(control::addListener)
        control.show()
        val ctrlB = launch(Activity::class.java)
        ctrlB.pause().stop().destroy() // 当前挂载的 B 先销毁
        controllers.remove(ctrlB)
        assertEquals(FxState.INSTALLED, control.state)
        assertNull(layerParent(control))
        assertNull((control.host as AppHost).attachedActivity)
        ctrlA.resume().postResume()
        assertEquals(FxState.SHOWN, control.state)
        assertSame(decor(ctrlA.get()), layerParent(control))
        // install 期那次 attach 监听器没看到；这里的 1/1 是 B 销毁的 detach + 回到 A 的重新 attach
        assertEquals(1, counting.attach)
        assertEquals(1, counting.detach)
    }

    @Test
    fun `blacklisted activity detaches and coming back restores`() {
        val ctrlA = launch(Activity::class.java)
        val control = install(AppHost.builder(app).blacklist(BlackActivity::class.java).build())
        control.show()
        launch(BlackActivity::class.java)
        assertEquals(FxState.INSTALLED, control.state)
        assertNull(layerParent(control))
        ctrlA.resume().postResume()
        assertEquals(FxState.SHOWN, control.state)
        assertSame(decor(ctrlA.get()), layerParent(control))
    }

    @Test
    fun `installing on a blacklisted top activity stays installed`() {
        launch(BlackActivity::class.java)
        val control = install(AppHost.builder(app).blacklist(BlackActivity::class.java.name).build())
        control.show()
        assertEquals(FxState.INSTALLED, control.state)
    }

    @Test
    fun `position survives re-parenting`() {
        val a = launch(Activity::class.java).get()
        val control = install()
        control.show()
        layoutDecor(a)
        control.moveTo(300f, 400f, animate = false)
        assertEquals(300f, control.position.x, 0f)
        assertEquals(400f, control.position.y, 0f)
        val b = launch(Activity::class.java).get()
        layoutDecor(b)
        assertSame(decor(b), layerParent(control))
        assertEquals(300f, control.position.x, 0f)
        assertEquals(400f, control.position.y, 0f)
    }

    @Test
    fun `parent relayout with unchanged bounds is not forwarded to core`() {
        val counting = BoundsCounting()
        val a = launch(Activity::class.java).get()
        val control = install(config = FxConfig.builder(content()).anchor(FxGravity.TOP_START).addFeature(counting).build())
        control.show()
        layoutDecor(a)
        control.moveTo(300f, 400f, animate = false)
        val dispatched = counting.bounds
        // 模拟页面里某个子 view requestLayout 引发的父容器重排：尺寸与 insets 都没变，不该惊动 core
        // （core 的 LocationFeature.onBoundsChanged 会清掉 dragInput，拖动中收到就会卡住手势）
        decor(a).requestLayout()
        layoutDecor(a)
        assertEquals(dispatched, counting.bounds)
        assertEquals(300f, control.position.x, 0f)
        assertEquals(400f, control.position.y, 0f)
        // 真的变了（旋转/分屏/insets）仍要派发。不断言具体次数：core 自己还有一条
        // FxLayerContainer.onSizeChanged -> engine.onBoundsChanged 的通路，尺寸真变时两条都会走
        layoutDecor(a, 720, 1280)
        assertTrue("可用区真变化时必须转达给 core", counting.bounds > dispatched)
    }

    @Test
    fun `bounds follow the decor size`() {
        val a = launch(Activity::class.java).get()
        val control = install()
        layoutDecor(a, 720, 1280)
        val b = control.host.bounds()
        assertEquals(720f, b.rect.width, 0f)
        assertEquals(1280f, b.rect.height, 0f)
    }

    @Test
    fun `content target mounts on android R id content`() {
        val a = launch(Activity::class.java).get()
        val control = install(AppHost.builder(app).attachTo(AppAttachTarget.CONTENT).build())
        control.show()
        assertSame(a.findViewById<ViewGroup>(android.R.id.content), layerParent(control))
    }

    @Test
    @Config(sdk = [28])
    fun `below api 29 mounts after the resume post runs`() {
        val control = install()
        control.show()
        val ctrl = Robolectric.buildActivity(Activity::class.java).create().start().resume().visible().also { controllers += it }
        shadowOf(Looper.getMainLooper()).idle() // 执行 onActivityResumed 排的 post
        assertEquals(FxState.SHOWN, control.state)
        assertSame(decor(ctrl.get()), layerParent(control))
    }

    @Test
    fun `cancel releases the tracker observer`() {
        launch(Activity::class.java)
        val control = install()
        val host = control.host as AppHost
        val layer = checkNotNull(control.contentView?.parent as? View) { "install 后容器应已挂上" }
        control.cancel()
        assertNull(layer.parent) // 容器已从 decor 上摘掉
        launch(Activity::class.java)
        assertNull(host.attachedActivity)
    }

    @Test
    fun `theme wraps the application context`() {
        val host = AppHost.builder(app).theme(android.R.style.Theme_Material_Light).build()
        assertEquals(true, host.context is android.view.ContextThemeWrapper)
        assertSame(app, AppHost.builder(app).build().context)
    }
}
