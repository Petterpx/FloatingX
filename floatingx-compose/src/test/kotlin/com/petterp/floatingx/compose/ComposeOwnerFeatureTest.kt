package com.petterp.floatingx.compose

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.update
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ComposeOwnerFeatureTest {

    class ProbeViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    private var control: FxControl? = null

    /** 模拟宿主 Activity 自带的 ViewTree owner（ComponentActivity / AppCompatActivity 的既有行为）；浮窗仍应当用自己的 owner */
    private val hostOwner = FxComposeOwner().apply { moveTo(Lifecycle.State.RESUMED) }

    /**
     * @param hostOwners 模拟宿主已经装好 ViewTree owner（ComponentActivity 的常态）。
     *   false 时是裸 android.app.Activity：decor 上什么都没有——浮窗自带 Recomposer，照样能组合。
     * @param readyOnBind false 时 host 永远不 ready，浮窗从未 attach 过。
     */
    private fun parentInWindow(hostOwners: Boolean = true, readyOnBind: Boolean = true): Pair<FrameLayout, TestHost> {
        val parent = windowParent(hostOwners)
        return parent to TestHost(parent, readyOnBind)
    }

    /** 一个真实 Activity 窗口里的父容器；[hostOwners] 决定 decor 上有没有宿主自己的 ViewTree owner */
    private fun windowParent(hostOwners: Boolean = true): FrameLayout {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        if (hostOwners) hostOwner.attachTo(activity.window.decorView)
        val parent = FrameLayout(activity)
        activity.findViewById<ViewGroup>(android.R.id.content).addView(parent, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return parent
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun owner(c: FxControl): FxComposeOwner = (c.config.content as FxComposeContent).owner

    @After
    fun tearDown() {
        control?.takeIf { it.state != FxState.CANCELLED }?.cancel()
        control = null
        hostOwner.destroy()
    }

    @Test
    fun `dsl installs content and feature and the owner follows the control lifecycle`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") { compose { Box(Modifier.size(10.dp)) }; this.host = host }.also { control = it }
        assertTrue(c.config.content is FxComposeContent)
        assertEquals(1, c.config.features.count { it is ComposeOwnerFeature })
        val o = owner(c)
        assertEquals(Lifecycle.State.STARTED, o.lifecycle.currentState) // attached，未 show
        c.show()
        assertEquals(Lifecycle.State.RESUMED, o.lifecycle.currentState)
        c.hide()
        assertEquals(Lifecycle.State.STARTED, o.lifecycle.currentState)
        host.lose()
        assertEquals(Lifecycle.State.CREATED, o.lifecycle.currentState)
        host.ready()
        assertSame(o, owner(c)) // 同一个 owner
        assertEquals(Lifecycle.State.STARTED, o.lifecycle.currentState)
        c.cancel()
        assertEquals(Lifecycle.State.DESTROYED, o.lifecycle.currentState)
    }

    @Test
    fun `composition runs against the real window and LocalFxControl is provided`() {
        val (_, host) = parentInWindow()
        var seen: FxControl? = null
        var local: FxControl? = null
        var lifecycleOwner: LifecycleOwner? = null
        var vmOwner: ViewModelStoreOwner? = null
        var compositions = 0
        val c = FloatingX.create("c") {
            compose { ctrl ->
                seen = ctrl
                local = LocalFxControl.current
                lifecycleOwner = LocalLifecycleOwner.current
                vmOwner = LocalViewModelStoreOwner.current
                SideEffect { compositions++ }
                Box(Modifier.size(10.dp))
            }
            this.host = host
        }.also { control = it }
        c.show()
        idle()
        assertSame(c, seen)
        assertSame(c, local)
        assertTrue(compositions >= 1)
        // 容器上装的是浮窗自己的 owner，尽管 decor 上还有宿主的 hostOwner 在竞争
        val container = c.contentView!!.parent as View
        assertSame(owner(c), container.findViewTreeLifecycleOwner())
        assertSame(owner(c), lifecycleOwner)
        assertSame(owner(c), vmOwner)
    }

    /**
     * 裸 Activity 的 decor 上没有任何 owner：内容自带 Recomposer 作为 parentCompositionContext，
     * 根本不去找窗口级 Recomposer，所以照样组合，且**不往宿主根 view 上写任何东西**。
     */
    @Test
    fun `composition runs when the host root has no owner and nothing is written onto it`() {
        val (parent, host) = parentInWindow(hostOwners = false)
        var compositions = 0
        val c = FloatingX.create("c") {
            compose { SideEffect { compositions++ }; Box(Modifier.size(10.dp)) }
            this.host = host
        }.also { control = it }
        c.show()
        idle()
        assertTrue(compositions >= 1)
        assertNull(parent.rootView.findViewTreeLifecycleOwner())   // 宿主 decor 保持原样
        c.cancel()
        assertNull(parent.rootView.findViewTreeLifecycleOwner())
    }

    /**
     * CRITICAL 回归：AppHost 换页是**静默换父**（removeView + addView，不发 session 事件）。
     * 新窗口的 decor 上没有 ViewTreeLifecycleOwner，旧实现会在 ComposeView.attachedToWindow 里
     * 去查窗口级 Recomposer 而崩（ViewTreeLifecycleOwner not found from DecorView）。
     */
    @Test
    fun `container re-parented into another window keeps composing`() {
        val (_, host) = parentInWindow(hostOwners = false)
        val second = windowParent(hostOwners = false)
        var compositions = 0
        var restored = -1
        val c = FloatingX.create("c") {
            compose {
                var count by rememberSaveable { mutableIntStateOf(0) }
                SideEffect {
                    compositions++
                    if (count == 0) count = 7 else restored = count
                }
                Box(Modifier.size(10.dp))
            }
            this.host = host
        }.also { control = it }
        c.show()
        idle()
        val first = compositions
        assertTrue(first >= 1)

        host.moveSilently(second)   // 复刻 AppHost.moveTo：不抛异常
        idle()

        assertTrue("换窗口后应重新组合，compositions=$compositions", compositions > first)
        assertEquals(7, restored)                       // rememberSaveable 过桥存活
        assertSame(second, (c.contentView!!.parent as View).parent)
        assertEquals(Lifecycle.State.RESUMED, owner(c).lifecycle.currentState)   // 生命周期没被打断
    }

    /** 容器本身就是窗口根 view（系统浮窗形态）：根 view 上只有浮窗自己的 owner，组合照跑 */
    @Test
    fun `composition runs when the container itself is the window root`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        var compositions = 0
        val c = FloatingX.create("c") {
            compose { SideEffect { compositions++ }; Box(Modifier.size(10.dp)) }
            this.host = WindowTestHost(activity)
        }.also { control = it }
        c.show()
        idle()
        assertTrue(compositions >= 1)
    }

    /** detach 期间换内容：core 无条件广播 onConfigChanged，旧 owner 当场销毁，绑定推迟到下一次 attach */
    @Test
    fun `content replaced while detached destroys the old owner and binds on the next attach`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") { compose { Box(Modifier.size(10.dp)) }; this.host = host }.also { control = it }
        val old = owner(c)
        host.lose()
        c.setContent(FxComposeContent { Box(Modifier.size(20.dp)) })
        assertTrue(old.isDestroyed)   // 不用等到 attach
        host.ready()
        val new = owner(c)
        assertNotSame(old, new)
        assertEquals(Lifecycle.State.STARTED, new.lifecycle.currentState)
    }

    /** detach 期间连换两次：中间那份（B）的 owner 也必须被销毁，不能漏 */
    @Test
    fun `two content swaps while detached destroy every intermediate owner`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") { compose { Box(Modifier.size(10.dp)) }; this.host = host }.also { control = it }
        val a = owner(c)
        host.lose()
        val b = FxComposeContent { Box(Modifier.size(20.dp)) }
        c.setContent(b)
        val cc = FxComposeContent { Box(Modifier.size(30.dp)) }
        c.setContent(cc)
        assertTrue(a.isDestroyed)
        assertTrue("中间那份内容的 owner 泄漏了", b.owner.isDestroyed)
        assertFalse(cc.owner.isDestroyed)
        host.ready()
        assertSame(cc.owner, owner(c))
        assertEquals(Lifecycle.State.STARTED, cc.owner.lifecycle.currentState)
    }

    /** 手动 removeFeature 摘掉 feature：onRemove 里把 owner 释放掉，不留给谁都不管 */
    @Test
    fun `removing the feature destroys the owner it holds`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") { compose { Box(Modifier.size(10.dp)) }; this.host = host }.also { control = it }
        val feature = c.config.features.first { it is ComposeOwnerFeature }
        val o = owner(c)
        val vm = ViewModelProvider(o)[ProbeViewModel::class.java]
        val container = c.contentView!!.parent as View
        assertSame(o, container.findViewTreeLifecycleOwner())
        c.removeFeature(feature)
        assertTrue(o.isDestroyed)
        assertTrue(vm.cleared)
        // 容器还活着，不能给它留一个已 destroy 的 owner
        assertSame(hostOwner, container.findViewTreeLifecycleOwner())
    }

    /** host 始终没 ready（从未 attach）就 cancel：owner 也必须销毁，否则 ViewModel 永远不 clear */
    @Test
    fun `cancel destroys the owner even when the control never attached`() {
        val (_, host) = parentInWindow(readyOnBind = false)
        val c = FloatingX.create("c") { compose { Box(Modifier.size(10.dp)) }; this.host = host }.also { control = it }
        val o = owner(c)
        val vm = ViewModelProvider(o)[ProbeViewModel::class.java]
        assertEquals(FxState.INSTALLED, c.state)
        c.cancel()
        assertTrue(o.isDestroyed)
        assertTrue(vm.cleared)
    }

    /** 换成非 compose 内容：旧 owner 销毁，容器上的 ViewTree owner 也摘掉（否则新内容会捡到已销毁的 owner） */
    @Test
    fun `swapping to non compose content clears the owner from the container`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") { compose { Box(Modifier.size(10.dp)) }; this.host = host }.also { control = it }
        c.show()
        val old = owner(c)
        val container = c.contentView!!.parent as View
        assertSame(old, container.findViewTreeLifecycleOwner())
        c.setContent(FxContent.provider { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(10, 10) } })
        assertTrue(old.isDestroyed)
        assertSame(hostOwner, container.findViewTreeLifecycleOwner())   // 摘干净了，只剩宿主自己的
    }

    /** 同一个配置里 compose {} 调两次：内容取最后一次，feature 只留一个 */
    @Test
    fun `calling compose twice keeps a single feature and the last content`() {
        val (_, host) = parentInWindow()
        var seen = 0
        val c = FloatingX.create("c") {
            compose { Box(Modifier.size(10.dp)) }
            compose { SideEffect { seen++ }; Box(Modifier.size(20.dp)) }
            this.host = host
        }.also { control = it }
        assertEquals(1, c.config.features.count { it is ComposeOwnerFeature })
        c.show()
        idle()
        assertTrue(seen >= 1)   // 生效的是后一次的内容
    }

    /** update { compose { } } 会把旧配置里的 feature 带过来：复用它，旧 owner 才有人 destroy */
    @Test
    fun `compose inside update reuses the feature and destroys the previous owner`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") { compose { Box(Modifier.size(10.dp)) }; this.host = host }.also { control = it }
        val old = owner(c)
        c.update { compose { Box(Modifier.size(20.dp)) } }
        assertEquals(1, c.config.features.count { it is ComposeOwnerFeature })
        assertTrue(old.isDestroyed)
        assertNotSame(old, owner(c))
        assertEquals(Lifecycle.State.STARTED, owner(c).lifecycle.currentState)
    }

    @Test
    fun `view models and rememberSaveable survive host loss, only cancel clears them`() {
        val (_, host) = parentInWindow()
        var restored = -1
        val c = FloatingX.create("c") {
            compose {
                var count by rememberSaveable { mutableIntStateOf(0) }
                SideEffect { if (count == 0) count = 7 else restored = count }
                Box(Modifier.size(10.dp))
            }
            this.host = host
        }.also { control = it }
        val vm = ViewModelProvider(owner(c))[ProbeViewModel::class.java]
        c.show()
        idle()
        host.lose() // 容器卸下：组合被 dispose，状态进 SavedStateRegistry
        idle()
        host.ready() // 重新挂上：重新组合，rememberSaveable 恢复为 7
        idle()
        assertEquals(7, restored)
        assertSame(vm, ViewModelProvider(owner(c))[ProbeViewModel::class.java])
        assertFalse(vm.cleared)
        c.cancel()
        assertTrue(vm.cleared)
    }

    @Test
    fun `replacing the content destroys the old owner and binds the new one`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") { compose { Box(Modifier.size(10.dp)) }; this.host = host }.also { control = it }
        val old = owner(c)
        c.show()
        c.setContent(FxComposeContent { Box(Modifier.size(20.dp)) })
        assertTrue(old.isDestroyed)
        val new = owner(c)
        assertEquals(Lifecycle.State.RESUMED, new.lifecycle.currentState)
    }

    @Test
    fun `feature ignores non compose content`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") {
            view { ctx -> android.view.View(ctx).apply { layoutParams = ViewGroup.LayoutParams(10, 10) } }
            addFeature(ComposeOwnerFeature())
            this.host = host
        }.also { control = it }
        c.show()
        c.cancel() // 不抛异常即可
        assertEquals(FxState.CANCELLED, c.state)
    }
}
