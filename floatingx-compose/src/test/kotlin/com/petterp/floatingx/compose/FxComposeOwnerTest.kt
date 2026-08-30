package com.petterp.floatingx.compose

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxComposeOwnerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    class ProbeViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    @Test
    fun `starts created with a restored saved state registry`() {
        val owner = FxComposeOwner()
        assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState)
        assertTrue(owner.savedStateRegistry.isRestored)
    }

    @Test
    fun `moveTo walks the lifecycle and destroy clears view models`() {
        val owner = FxComposeOwner()
        val vm = ViewModelProvider(owner)[ProbeViewModel::class.java]
        owner.moveTo(Lifecycle.State.RESUMED)
        assertEquals(Lifecycle.State.RESUMED, owner.lifecycle.currentState)
        owner.moveTo(Lifecycle.State.CREATED)
        assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState)
        assertSame(vm, ViewModelProvider(owner)[ProbeViewModel::class.java])   // detach 不清 ViewModel
        owner.destroy()
        assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
        assertTrue(vm.cleared)
        assertTrue(owner.isDestroyed)
        owner.moveTo(Lifecycle.State.RESUMED)                                   // destroy 后忽略
        assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
        owner.destroy()                                                          // 幂等
    }

    @Test
    fun `attachTo installs all three view tree owners`() {
        val owner = FxComposeOwner()
        val view = View(context)
        owner.attachTo(view)
        assertSame(owner, view.findViewTreeLifecycleOwner())
        assertSame(owner, view.findViewTreeViewModelStoreOwner())
        assertSame(owner, view.findViewTreeSavedStateRegistryOwner())
    }
}
