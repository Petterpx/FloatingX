package com.petterp.floatingx.app

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.petterp.floatingx.core.FxActivityTracker

/**
 * 进程启动时自动初始化 [FxActivityTracker]，除此之外不提供任何数据能力。
 *
 * 为什么需要它：`registerActivityLifecycleCallbacks` 不会重放已经发生的回调，
 * 若等到 `AppHost.bind()`（即 `FloatingX.install()`）才注册，而此时当前页面早已 resume，
 * `FxActivityTracker.topActivity` 就是 null，浮窗要等用户跳到下一个 Activity 才出现。
 * ContentProvider 的 onCreate 早于 `Application.onCreate` 之后的一切 Activity 生命周期，
 * 在这里注册即可保证任何时机的 install 都能拿到当前前台 Activity。
 *
 * 如果用不惯这种隐式初始化，可以在 App 的清单里用
 * `<provider android:name="com.petterp.floatingx.app.FxAppInitProvider" tools:node="remove" />`
 * 移除它，但**必须**自行在 `Application.onCreate` 里调用 `FxActivityTracker.init(app)`。
 */
public class FxAppInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        (context?.applicationContext as? Application)?.let(FxActivityTracker::init)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
