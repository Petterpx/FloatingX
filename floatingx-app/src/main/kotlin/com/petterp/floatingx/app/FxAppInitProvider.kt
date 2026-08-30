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
 * ContentProvider 的 onCreate 跑在 `Application.onCreate` 之前、任何 Activity 生命周期之前，
 * 所以只要本 provider 生效，install 写在哪个时机都能拿到当前前台 Activity。
 *
 * 两种拿不到的情况，需要自行在 `Application.onCreate` 里调用 `FxActivityTracker.init(app)`：
 * 1. 在 App 的清单里用
 *    `<provider android:name="com.petterp.floatingx.app.FxAppInitProvider" tools:node="remove" />`
 *    移除了它；
 * 2. **多进程**：本 provider 没有声明 `android:process`，只会在默认进程创建。
 *    非默认进程里 tracker 仍然要等 `AppHost.bind()` 才注册，于是又退回到「install 晚于当前页 resume
 *    就拿不到 topActivity」的情形。
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
