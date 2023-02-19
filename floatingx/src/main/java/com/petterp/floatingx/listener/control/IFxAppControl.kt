package com.petterp.floatingx.listener.control

import android.app.Activity

/** App特有的控制方法 */
interface IFxAppControl : IFxControl {

    /**
     * 在当前activity中显示浮窗
     *
     * @param activity 当前要显示浮窗的activity
     *
     * 第一次调用该方法时，我们会插入一个AppLifecycle，用于监听activity的变化。当后续浮窗被cancel()时，我们会根据浮窗个数(=0)，自动清空该lifecycle的绑定
     *
     * ps:尽管我们可以做到不传递activity，但是这种方式需要以性能作为牺牲，比如需要永久维护一个顶级activity与AppLifecycle监听器
     */
    fun show(activity: Activity)

    /** 从当前activity中移除 */
    fun detach(activity: Activity)

    /** 获得当前绑定的activity,不要手动保留此activity,以避免泄漏 */
    fun getBindActivity(): Activity?
}
