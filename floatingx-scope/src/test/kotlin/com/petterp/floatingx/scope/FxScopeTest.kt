package com.petterp.floatingx.scope

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.layout.FxGravity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class FxScopeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val created = mutableListOf<FxControl>()

    private fun box(ctx: Context) = View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) }

    @After
    fun tearDown() {
        created.filter { it.state != FxState.CANCELLED }.forEach { it.cancel() }
        created.clear()
    }

    @Test
    fun `view group fxScope mounts inside that view group`() {
        val parent = FrameLayout(context)
        val control = parent.fxScope { view(::box); anchor(FxGravity.BOTTOM_END) }.also(created::add)
        control.show()
        assertTrue(control.host is ViewGroupHost)
        assertSame(parent, (control.host as ViewGroupHost).viewGroup)
        assertEquals(1, parent.childCount)
        assertEquals(FxState.SHOWN, control.state)
    }

    @Test
    fun `activity fxScope mounts on android R id content`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val control = activity.fxScope { view(::box) }.also(created::add)
        control.show()
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        assertSame(content, (control.host as ViewGroupHost).viewGroup)
        assertEquals(1, content.childCount)
    }

    @Test
    fun `activity fxScope cancels automatically when the activity is destroyed`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val control = controller.get().fxScope { view(::box) }.also(created::add)
        control.show()
        controller.pause().stop().destroy()
        assertEquals(FxState.CANCELLED, control.state)
    }

    @Test
    @Config(sdk = [28])
    fun `activity fxScope below api 29 only loses host on destroy`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val control = controller.get().fxScope { view(::box) }.also(created::add)
        control.show()
        controller.pause().stop().destroy()
        assertEquals(FxState.INSTALLED, control.state) // 没有 per-Activity 回调，靠 window detach → lost
    }

    @Test
    fun `viewGroupHost dsl sets host on install scope`() {
        val parent = FrameLayout(context)
        val control = FloatingX.create("dsl") { view(::box); viewGroupHost(parent) }.also(created::add)
        assertSame(parent, (control.host as ViewGroupHost).viewGroup)
    }

    @Test
    fun `tag is forwarded for persistence`() {
        val parent = FrameLayout(context)
        val control = parent.fxScope(tag = "local-a") { view(::box) }.also(created::add)
        assertEquals("local-a", control.tag)
    }
}
