package com.petterp.floatingx.core.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxSpStorageTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val storage = FxSpStorage(context, "test_fx")

    @Test
    fun `save then load round trips`() {
        val anchor = FxAnchor(FxGravity.BOTTOM_END, 12.5f, -3f)
        storage.save("tag:1", anchor)
        assertEquals(anchor, storage.load("tag:1"))
    }

    @Test
    fun `missing key returns null`() {
        assertNull(storage.load("nope"))
    }

    @Test
    fun `corrupt value returns null`() {
        context.getSharedPreferences("test_fx", Context.MODE_PRIVATE).edit().putString("bad", "garbage").commit()
        assertNull(storage.load("bad"))
    }

    @Test
    fun `clear removes key`() {
        storage.save("k", FxAnchor(FxGravity.CENTER))
        storage.clear("k")
        assertNull(storage.load("k"))
    }
}
