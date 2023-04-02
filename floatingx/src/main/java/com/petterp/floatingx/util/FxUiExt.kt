package com.petterp.floatingx.util

import android.app.Activity
import android.widget.FrameLayout
import com.petterp.floatingx.impl.lifecycle.FxLifecycleCallbackImpl

/** App级当前设置了tag的栈顶Activity */
internal val topActivity: Activity?
    get() = FxLifecycleCallbackImpl.getTopActivity()

internal val Activity.decorView: FrameLayout?
    get() = try {
        window.decorView as FrameLayout
    } catch (_: Exception) {
        null
    }

internal val Activity.contentView: FrameLayout?
    get() = try {
        window.decorView.findViewById(android.R.id.content)
    } catch (_: Exception) {
        null
    }
