package com.petterp.floatingx.listener.control

import android.app.Activity

/** App特有的控制方法 */
interface IFxAppControl {
    fun show(activity: Activity)

    fun detach(activity: Activity)
}
