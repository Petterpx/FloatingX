package com.petterp.floatingx.compose

import android.app.Activity
import android.os.Looper
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun parentInWindow(): Pair<FrameLayout, TestHost> {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        // 窗口级 Recomposer 从 android.R.id.content 的直接子 view 往上找 LifecycleOwner；
        // 裸 android.app.Activity 不挂 owner（androidx 的 ComponentActivity 才挂），这里补上
        hostOwner.attachTo(activity.window.decorView)
        val parent = FrameLayout(activity)
        activity.findViewById<ViewGroup>(android.R.id.content).addView(parent, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return parent to TestHost(parent)
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
        var compositions = 0
        val c = FloatingX.create("c") {
            compose { ctrl ->
                seen = ctrl
                local = LocalFxControl.current
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
