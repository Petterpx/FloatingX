package com.petterp.floatingx.impl

import android.app.Activity
import android.os.Bundle

/**
 * @Author petterp
 * @Date 2021/6/9-9:56 上午
 * @Email ShiyihuiCloud@163.com
 * @Function lifecycle扩展
 * PS: 只有显示悬浮窗的Activity的才会被回调相应生命周期
 */
class FxLifecycleExpand {

    var onActivityPostCreated: ((Activity) -> Unit)? = null
    var onActivityCreated: ((Activity, Bundle?) -> Unit)? = null
    var onActivityPreCreated: ((Activity, Bundle?) -> Unit)? = null

    var onActivityPostStarted: ((Activity) -> Unit)? = null
    var onActivityStarted: ((Activity) -> Unit)? = null
    var onActivityPreStarted: ((Activity) -> Unit)? = null

    var onActivityPostResumed: ((Activity) -> Unit)? = null
    var onActivityResumed: ((Activity) -> Unit)? = null
    var onActivityPreResumed: ((Activity) -> Unit)? = null

    var onActivityPostPaused: ((Activity) -> Unit)? = null
    var onActivityPaused: ((Activity) -> Unit)? = null
    var onActivityPrePaused: ((Activity) -> Unit)? = null

    var onActivityPostStopped: ((Activity) -> Unit)? = null
    var onActivityStopped: ((Activity) -> Unit)? = null
    var onActivityPreStopped: ((Activity) -> Unit)? = null

    var onActivityPostSaveInstanceState: ((Activity, Bundle) -> Unit?)? = null
    var onActivityPreSaveInstanceState: ((Activity, Bundle) -> Unit?)? = null
    var onActivitySaveInstanceState: ((Activity, Bundle) -> Unit?)? = null

    var onActivityPostDestroyed: ((Activity) -> Unit)? = null
    var onActivityDestroyed: ((Activity) -> Unit)? = null
    var onActivityPreDestroyed: ((Activity) -> Unit)? = null
}
