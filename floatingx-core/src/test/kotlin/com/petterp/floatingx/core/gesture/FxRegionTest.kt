package com.petterp.floatingx.core.gesture

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** dragRegion 的两个内置实现（#165）。坐标都是相对内容 view 左上角的 */
@RunWith(RobolectricTestRunner::class)
class FxRegionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val content = FrameLayout(context)
    private val direct = View(context)
    private val middle = FrameLayout(context)
    private val nested = View(context)

    private val contentId = View.generateViewId()
    private val directId = View.generateViewId()
    private val nestedId = View.generateViewId()
    private val missingId = View.generateViewId()

    @Before
    fun setUp() {
        content.id = contentId
        direct.id = directId
        nested.id = nestedId
        content.addView(direct)
        content.addView(middle)
        middle.addView(nested)
        // 先摆父再摆子：FrameLayout.onLayout 会把未测量的子 view 归零
        content.layout(0, 0, 200, 300)
        direct.layout(10, 20, 50, 60)
        middle.layout(100, 100, 200, 300)
        nested.layout(5, 5, 25, 45)
    }

    @Test
    fun `child matches the direct child bounds`() {
        val region = FxRegion.child(directId)
        assertTrue(region.contains(10f, 20f, content))
        assertTrue(region.contains(30f, 40f, content))
        assertFalse(region.contains(9f, 20f, content))
        assertFalse(region.contains(30f, 61f, content))
    }

    @Test
    fun `child matches a nested child in content coordinates`() {
        val region = FxRegion.child(nestedId)
        // nested 在 middle 里偏移 (5,5)，middle 又偏移 (100,100)
        assertTrue(region.contains(105f, 105f, content))
        assertTrue(region.contains(120f, 140f, content))
        assertFalse(region.contains(104f, 104f, content))
        assertFalse(region.contains(125f, 145f, content))
    }

    @Test
    fun `child equal to content matches everywhere`() {
        val region = FxRegion.child(contentId)
        assertTrue(region.contains(0f, 0f, content))
        assertTrue(region.contains(9999f, 9999f, content))
    }

    @Test
    fun `child with an unknown id never matches`() {
        val region = FxRegion.child(missingId)
        assertFalse(region.contains(30f, 40f, content))
        // 内容不是 ViewGroup 时也不能崩
        assertFalse(FxRegion.child(directId).contains(30f, 40f, View(context)))
    }

    @Test
    fun `rect includes its own edges`() {
        val region = FxRegion.rect(10f, 20f, 50f, 60f)
        assertTrue(region.contains(10f, 20f, content))
        assertTrue(region.contains(50f, 60f, content))
        assertTrue(region.contains(30f, 40f, content))
        assertFalse(region.contains(9.9f, 40f, content))
        assertFalse(region.contains(30f, 60.1f, content))
    }
}
