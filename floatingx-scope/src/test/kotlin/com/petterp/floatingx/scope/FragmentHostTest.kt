package com.petterp.floatingx.scope

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FragmentHostTest {

    /** 在 onCreate（view 尚不存在）就调用 fxScope，复现 #244 */
    class EarlyFragment : Fragment() {
        lateinit var control: FxControl
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            control = fxScope(tag = "frag") { view { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } } }
            control.show()
        }
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FrameLayout(requireContext())
    }

    /** 在自己的 onDestroy 里调 fxScope：此时 fragment 仍 attach，但 lifecycle 已是 DESTROYED */
    class DestroyTimeFragment : Fragment() {
        var error: IllegalStateException? = null
        override fun onDestroy() {
            super.onDestroy()
            error = runCatching { fxScope("destroyed") { view { ctx -> View(ctx) } } }.exceptionOrNull() as? IllegalStateException
        }
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FrameLayout(requireContext())
    }

    /** 根 view 不是 ViewGroup */
    class PlainViewFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = View(requireContext())
    }

    private fun activity(): FragmentActivity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()

    @Test
    fun `fxScope called before the view exists attaches once the view is created`() {
        val activity = activity()
        val fragment = EarlyFragment()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commitNow()
        assertEquals(FxState.SHOWN, fragment.control.state)
        val root = fragment.requireView() as ViewGroup
        assertEquals(1, root.childCount)
    }

    @Test
    fun `view destroy loses host and view recreate readies again on the new root`() {
        val activity = activity()
        val fragment = EarlyFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(android.R.id.content, fragment).commitNow()
        val firstRoot = fragment.requireView()

        fm.beginTransaction().detach(fragment).commitNow() // 只销毁 view，fragment 存活
        assertEquals(FxState.INSTALLED, fragment.control.state)

        fm.beginTransaction().attach(fragment).commitNow() // 重新创建 view
        assertEquals(FxState.SHOWN, fragment.control.state)
        val secondRoot = fragment.requireView() as ViewGroup
        assertNotSame(firstRoot, secondRoot)
        assertEquals(1, secondRoot.childCount)
        assertSame(secondRoot, (fragment.control.contentView!!.parent as View).parent)
    }

    @Test
    fun `fragment destroy cancels the control`() {
        val activity = activity()
        val fragment = EarlyFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(android.R.id.content, fragment).commitNow()
        fm.beginTransaction().remove(fragment).commitNow()
        assertEquals(FxState.CANCELLED, fragment.control.state)
    }

    @Test
    fun `non view group root is rejected`() {
        val activity = activity()
        val fragment = PlainViewFragment()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commitNow()
        assertThrows(IllegalStateException::class.java) {
            fragment.fxScope { view { ctx -> View(ctx) } }
        }
    }

    @Test
    fun `fxScope before attach is rejected with a clear message`() {
        val ex = assertThrows(IllegalStateException::class.java) {
            EarlyFragment().fxScope { view { ctx -> View(ctx) } }
        }
        assertTrue(ex.message!!.contains("尚未 attach"))
    }

    @Test
    fun `fxScope after the fragment was removed is rejected`() {
        val activity = activity()
        val fragment = EarlyFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(android.R.id.content, fragment).commitNow()
        fm.beginTransaction().remove(fragment).commitNow()
        // 注意：FragmentManager 销毁 fragment 后会 initState()，lifecycle 被重置回 INITIALIZED，
        // 所以这里挡住它的是 host 的 context 检查（fragment 已 detach），不是 DESTROYED 检查
        assertThrows(IllegalStateException::class.java) {
            fragment.fxScope("removed") { view { ctx -> View(ctx) } }
        }
    }

    @Test
    fun `fxScope inside onDestroy is rejected`() {
        val activity = activity()
        val fragment = DestroyTimeFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(android.R.id.content, fragment).commitNow()
        fm.beginTransaction().remove(fragment).commitNow()
        val ex = checkNotNull(fragment.error) { "onDestroy 里创建浮窗应当抛异常" }
        assertTrue(ex.message, ex.message!!.contains("已 destroy"))
    }
}
