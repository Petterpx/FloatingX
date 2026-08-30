package com.petterp.floatingx.app

import com.petterp.floatingx.core.layout.FxInsets
import org.junit.Assert.assertEquals
import org.junit.Test

class AppHostInsetsTest {

    /** 状态栏 63、导航栏 126 */
    private fun insets(target: AppAttachTarget, offsetX: Int, offsetY: Int, parentWidth: Int, parentHeight: Int, rootWidth: Int, rootHeight: Int) =
        AppHost.contentInsets(0, 63, 0, 126, target, offsetX, offsetY, parentWidth, parentHeight, rootWidth, rootHeight)

    @Test
    fun `decor target uses system bar insets as is`() {
        val r = insets(AppAttachTarget.DECOR, offsetX = 0, offsetY = 0, parentWidth = 1080, parentHeight = 1920, rootWidth = 1080, rootHeight = 1920)
        assertEquals(FxInsets(0f, 63f, 0f, 126f), r)
    }

    @Test
    fun `content target subtracts the offset the system bars already pushed it by`() {
        // content 已被状态栏挤下 63、被导航栏挤上 126：剩余 insets 应为 0
        val r = insets(AppAttachTarget.CONTENT, offsetX = 0, offsetY = 63, parentWidth = 1080, parentHeight = 1731, rootWidth = 1080, rootHeight = 1920)
        assertEquals(FxInsets.NONE, r)
    }

    @Test
    fun `content target keeps insets when edge to edge`() {
        val r = insets(AppAttachTarget.CONTENT, offsetX = 0, offsetY = 0, parentWidth = 1080, parentHeight = 1920, rootWidth = 1080, rootHeight = 1920)
        assertEquals(FxInsets(0f, 63f, 0f, 126f), r)
    }

    @Test
    fun `content target never goes negative`() {
        val r = insets(AppAttachTarget.CONTENT, offsetX = 0, offsetY = 100, parentWidth = 1080, parentHeight = 1600, rootWidth = 1080, rootHeight = 1920)
        assertEquals(FxInsets.NONE, r)
    }
}
