package com.petterp.floatingx.core.config

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxContentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val parent = FrameLayout(context)

    @Test
    fun `layout content inflates without attaching`() {
        val view = FxContent.layout(android.R.layout.simple_list_item_1).create(context, parent)
        assertTrue(view is TextView)
        assertNull(view.parent)
        assertEquals(0, parent.childCount)
    }

    @Test
    fun `static content returns the same instance`() {
        val v = View(context)
        assertSame(v, FxContent.view(v).create(context, parent))
    }

    @Test
    fun `provider content is invoked with context`() {
        var seen: Context? = null
        val view = FxContent.provider { ctx -> seen = ctx; View(ctx) }.create(context, parent)
        assertSame(context, seen)
        assertSame(context, view.context)
    }
}
