package com.petterp.floatingx.app

import android.app.Activity
import com.petterp.floatingx.app.internal.ActivityRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActivityRulesTest {

    open class BaseActivity : Activity()
    class SubActivity : BaseActivity()
    class OtherActivity : Activity()

    private fun <T : Activity> make(cls: Class<T>): T = Robolectric.buildActivity(cls).get()

    private fun rules(
        blackClasses: List<Class<out Activity>> = emptyList(),
        blackNames: Set<String> = emptySet(),
        whiteClasses: List<Class<out Activity>> = emptyList(),
        whiteNames: Set<String> = emptySet(),
        filters: List<AppActivityFilter> = emptyList(),
    ) = ActivityRules(blackClasses, blackNames, whiteClasses, whiteNames, filters)

    @Test
    fun `accept all by default`() {
        assertTrue(ActivityRules.ACCEPT_ALL.accept(make(OtherActivity::class.java)))
    }

    @Test
    fun `blacklist by class also matches subclasses`() {
        val r = rules(blackClasses = listOf(BaseActivity::class.java))
        assertFalse(r.accept(make(BaseActivity::class.java)))
        assertFalse(r.accept(make(SubActivity::class.java)))
        assertTrue(r.accept(make(OtherActivity::class.java)))
    }

    @Test
    fun `blacklist by name is exact`() {
        val r = rules(blackNames = setOf(BaseActivity::class.java.name))
        assertFalse(r.accept(make(BaseActivity::class.java)))
        assertTrue(r.accept(make(SubActivity::class.java)))
    }

    @Test
    fun `whitelist rejects everything not listed`() {
        val r = rules(whiteClasses = listOf(BaseActivity::class.java))
        assertTrue(r.accept(make(SubActivity::class.java)))
        assertFalse(r.accept(make(OtherActivity::class.java)))
    }

    @Test
    fun `whitelist by name is exact and does not admit subclasses`() {
        val r = rules(whiteNames = setOf(BaseActivity::class.java.name))
        assertTrue(r.accept(make(BaseActivity::class.java)))
        assertFalse(r.accept(make(SubActivity::class.java)))
        assertFalse(r.accept(make(OtherActivity::class.java)))
    }

    @Test
    fun `blacklist wins over whitelist`() {
        val r = rules(whiteClasses = listOf(BaseActivity::class.java), blackClasses = listOf(SubActivity::class.java))
        assertTrue(r.accept(make(BaseActivity::class.java)))
        assertFalse(r.accept(make(SubActivity::class.java)))
    }

    @Test
    fun `custom filters must all accept`() {
        val r = rules(filters = listOf(AppActivityFilter { true }, AppActivityFilter { it !is OtherActivity }))
        assertTrue(r.accept(make(BaseActivity::class.java)))
        assertFalse(r.accept(make(OtherActivity::class.java)))
    }

    @Test
    fun `whitelist then blacklist then custom filter are applied in order`() {
        // 白名单放行 BaseActivity 全家；黑名单按名字踢掉 SubActivity；自定义规则再踢掉 finishing 的页面
        val r = rules(
            whiteClasses = listOf(BaseActivity::class.java),
            blackNames = setOf(SubActivity::class.java.name),
            filters = listOf(AppActivityFilter { !it.isFinishing }),
        )
        assertTrue(r.accept(make(BaseActivity::class.java)))
        assertFalse("白名单内但被黑名单命中", r.accept(make(SubActivity::class.java)))
        assertFalse("不在白名单里", r.accept(make(OtherActivity::class.java)))
        assertFalse("白名单+黑名单都过了，仍要过自定义规则", r.accept(make(BaseActivity::class.java).also { it.finish() }))
    }
}
