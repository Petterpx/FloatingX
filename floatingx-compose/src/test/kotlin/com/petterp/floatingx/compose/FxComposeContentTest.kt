package com.petterp.floatingx.compose

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxComposeContentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `create returns a ComposeView owned by the content's owner`() {
        val content = FxComposeContent { Box(Modifier.size(10.dp)) }
        val view = content.create(context, FrameLayout(context))
        assertTrue(view is ComposeView)
        assertSame(content.owner, view.findViewTreeLifecycleOwner())
        assertSame(content.owner, view.findViewTreeViewModelStoreOwner())
        assertSame(content.owner, view.findViewTreeSavedStateRegistryOwner())
    }
}
