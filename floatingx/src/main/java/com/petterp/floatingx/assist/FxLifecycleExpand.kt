package com.petterp.floatingx.assist

import android.app.Activity
import android.os.Bundle

/**
 * @Author petterp
 * @Date 2021/6/9-9:56 上午
 * @Email ShiyihuiCloud@163.com
 * @Function Fx-lifecycle扩展
 * PS: 只有显示悬浮窗的Activity的才会被回调相应生命周期
 */
class FxLifecycleExpand {
    var onActivityCreated: ((Activity, Bundle?) -> Unit)? = null
    var onActivityStarted: ((Activity) -> Unit)? = null
    var onActivityResumed: ((Activity) -> Unit)? = null
    var onActivityPaused: ((Activity) -> Unit)? = null
    var onActivityStopped: ((Activity) -> Unit)? = null
    var onActivitySaveInstanceState: ((Activity, Bundle) -> Unit?)? = null
    var onActivityDestroyed: ((Activity) -> Unit)? = null
}
