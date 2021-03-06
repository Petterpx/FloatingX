package com.petterp.floatingx.listener.control

import android.app.Activity

/** App特有的控制方法 */
interface IFxAppControl : IFxControl {
    fun show(activity: Activity)

    fun detach(activity: Activity)

    /** 获得当前绑定的activity,不要手动保留此activity,以避免泄漏 */
    fun getBindActivity(): Activity?
}
