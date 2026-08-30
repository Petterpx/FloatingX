package com.petterp.floatingx.app

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FxActivityTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * 这里刻意不像 AppHostTest 那样在 @Before 里 seed tracker：
 * 全靠 provider 的 onCreate 完成注册，才能证明「进程启动即初始化」这条路径是通的。
 */
@RunWith(RobolectricTestRunner::class)
class FxAppInitProviderTest {

    @Test
    fun `provider registers the tracker before the first activity resumes`() {
        Robolectric.setupContentProvider(FxAppInitProvider::class.java)
        val controller = Robolectric.buildActivity(Activity::class.java).create().start().resume().postResume()
        assertSame(controller.get(), FxActivityTracker.topActivity)
        controller.pause().stop().destroy()
    }

    /** provider 只有写进清单才会被系统创建：这条用例守的是 AndroidManifest.xml 里那段声明 */
    @Test
    fun `provider is declared in the merged manifest`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.resolveContentProvider("${context.packageName}.floatingx.app.init", 0)
        assertNotNull("清单里没有 FxAppInitProvider 的 <provider> 声明", info)
        assertEquals(FxAppInitProvider::class.java.name, info!!.name)
    }
}
