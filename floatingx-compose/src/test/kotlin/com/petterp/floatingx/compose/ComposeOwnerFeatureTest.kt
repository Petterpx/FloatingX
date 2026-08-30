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
     *   false 时是裸 android.app.Activity：decor 上什么都没有，靠 ComposeOwnerFeature 的根 view 兜底。
     * @param readyOnBind false 时 host 永远不 ready，浮窗从未 attach 过。
     */
    private fun parentInWindow(hostOwners: Boolean = true, readyOnBind: Boolean = true): Pair<FrameLayout, TestHost> {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        // 窗口级 Recomposer 从 android.R.id.content 的直接子 view 往上找 LifecycleOwner
        if (hostOwners) hostOwner.attachTo(activity.window.decorView)
        val parent = FrameLayout(activity)
        activity.findViewById<ViewGroup>(android.R.id.content).addView(parent, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return parent to TestHost(parent, readyOnBind)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun owner(c: FxControl): FxComposeOwner = (c.config.content as FxComposeContent).owner

    @After
    fun tearDown() {
        control?.takeIf { it.state != FxState.CANCELLED }?.cancel()
        control = null
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

    /** 裸 Activity 的 decor 上没有任何 owner：bind 时把浮窗自己的补到根 view 上，组合才跑得起来 */
    @Test
    fun `composition runs when the host root has no owner and the fallback is removed on cancel`() {
        val (parent, host) = parentInWindow(hostOwners = false)
        var compositions = 0
        val c = FloatingX.create("c") {
            compose { SideEffect { compositions++ }; Box(Modifier.size(10.dp)) }
            this.host = host
        }.also { control = it }
        c.show()
        idle()
        assertTrue(compositions >= 1)
        assertSame(owner(c), parent.rootView.findViewTreeLifecycleOwner())
        c.cancel()
        assertNull(parent.rootView.findViewTreeLifecycleOwner())   // 不留一个已 destroy 的 owner 在宿主 decor 上
    }

    /** detach 期间换内容：core 不会回调 onConfigChanged，下一次 attach 必须自己对账 */
    @Test
    fun `content replaced while detached is reconciled on the next attach`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") { compose { Box(Modifier.size(10.dp)) }; this.host = host }.also { control = it }
        val old = owner(c)
        host.lose()
        c.setContent(FxComposeContent { Box(Modifier.size(20.dp)) })
        host.ready()
        assertTrue(old.isDestroyed)
        val new = owner(c)
        assertNotSame(old, new)
        assertEquals(Lifecycle.State.STARTED, new.lifecycle.currentState)
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
