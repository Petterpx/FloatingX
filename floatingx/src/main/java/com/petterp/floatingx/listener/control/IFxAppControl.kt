package com.petterp.floatingx.listener.control

import android.app.Activity

/** App特有的控制方法 */
interface IFxAppControl : IFxControl {

    /** 获得当前浮窗绑定的activity,不要手动保留此activity,以避免泄漏
     * ps: system浮窗时，将始终返回栈顶 activity
     * */
    fun getBindActivity(): Activity?
}
