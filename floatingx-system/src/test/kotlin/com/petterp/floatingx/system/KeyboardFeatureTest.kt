package com.petterp.floatingx.system

import android.app.Application
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.system.container.FxWindowContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class KeyboardFeatureTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val editId = View.generateViewId()

    private fun content(): FxContent = FxContent.provider { ctx ->
        FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(300, 100)
            addView(EditText(ctx).apply { id = editId })
        }
    }

    @After
    fun tearDown() = FloatingX.uninstallAll()

    @Test
    fun `touching the edit text makes the window focusable and back restores it`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = FloatingX.install("kb", FxConfig.builder(content()).build(), SystemHost.builder(app).keyboard(editId).build())
        control.show()
        val window = control.contentView!!.parent as FxWindowContainer
        assertTrue(window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)

        val edit = control.contentView!!.findViewById<EditText>(editId)
        edit.dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0))
        assertEquals(0, window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        window.dispatchKeyEventPreIme(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        assertTrue(window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
    }

    /**
     * 换内容后旧的 EditText 已经被摘掉，必须在新内容上重新绑定，
     * 否则 keyboard(id) 静默失效——触摸新 EditText 再也唤不起键盘。
     */
    @Test
    fun `replacing the content rebinds the edit text`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = FloatingX.install("kb3", FxConfig.builder(content()).build(), SystemHost.builder(app).keyboard(editId).build())
        control.show()
        val window = control.contentView!!.parent as FxWindowContainer
        val oldEdit = control.contentView!!.findViewById<EditText>(editId)

        control.setContent(content())
        val newEdit = control.contentView!!.findViewById<EditText>(editId)
        assertNotSame(oldEdit, newEdit)
        assertTrue(window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)

        newEdit.dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0))
        assertEquals(0, window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    @Test
    fun `detach restores the not focusable flag`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = FloatingX.install("kb2", FxConfig.builder(content()).build(), SystemHost.builder(app).keyboard(editId).build())
        control.show()
        val window = control.contentView!!.parent as FxWindowContainer
        control.contentView!!.findViewById<EditText>(editId).dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0))
        assertEquals(0, window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        control.cancel()
        assertTrue(window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
    }
}
