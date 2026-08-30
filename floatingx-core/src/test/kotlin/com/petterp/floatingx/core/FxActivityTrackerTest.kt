package com.petterp.floatingx.core

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxActivityTrackerTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = FxActivityTracker.init(app)

    @Test
    fun `tracks resumed activity and clears on destroy`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        assertSame(controller.get(), FxActivityTracker.topActivity)
        controller.pause().stop().destroy()
        assertNull(FxActivityTracker.topActivity)
    }

    @Test
    fun `observers receive lifecycle events`() {
        val seen = mutableListOf<String>()
        val observer = object : FxActivityTracker.Observer {
            override fun onActivityResumed(activity: Activity) { seen += "resumed" }
            override fun onActivityPaused(activity: Activity) { seen += "paused" }
            override fun onActivityDestroyed(activity: Activity) { seen += "destroyed" }
        }
        FxActivityTracker.addObserver(observer)
        Robolectric.buildActivity(Activity::class.java).setup().pause().stop().destroy()
        FxActivityTracker.removeObserver(observer)
        assertEquals(listOf("resumed", "paused", "destroyed"), seen)
    }

    @Test
    fun `post resumed is forwarded to observers`() {
        val seen = mutableListOf<String>()
        val observer = object : FxActivityTracker.Observer {
            override fun onActivityResumed(activity: Activity) { seen += "resumed" }
            override fun onActivityPostResumed(activity: Activity) { seen += "postResumed" }
        }
        FxActivityTracker.addObserver(observer)
        val controller = Robolectric.buildActivity(Activity::class.java).create().start().resume().postResume()
        FxActivityTracker.removeObserver(observer)
        assertEquals(listOf("resumed", "postResumed"), seen)
        assertSame(controller.get(), FxActivityTracker.topActivity)
    }

    @Test
    fun `init is idempotent`() {
        FxActivityTracker.init(app)
        FxActivityTracker.init(app)
        var resumed = 0
        val observer = object : FxActivityTracker.Observer {
            override fun onActivityResumed(activity: Activity) { resumed++ }
        }
        FxActivityTracker.addObserver(observer)
        Robolectric.buildActivity(Activity::class.java).setup()
        FxActivityTracker.removeObserver(observer)
        assertEquals(1, resumed)
    }
}
