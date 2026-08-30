package com.petterp.floatingx.system

/** 系统窗口收到返回键（仅窗口可聚焦时会收到，如 KeyboardFeature 唤起键盘期间）。返回 true 表示已消费 */
public fun interface SystemBackListener {
    public fun onBackPressed(): Boolean
}
