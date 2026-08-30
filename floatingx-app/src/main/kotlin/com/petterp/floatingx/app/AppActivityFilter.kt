package com.petterp.floatingx.app

import android.app.Activity

/** 自定义 Activity 过滤（#221：父类、包名、任意规则）。返回 true 表示允许浮窗出现在该 Activity 上 */
public fun interface AppActivityFilter {
    public fun accept(activity: Activity): Boolean
}
