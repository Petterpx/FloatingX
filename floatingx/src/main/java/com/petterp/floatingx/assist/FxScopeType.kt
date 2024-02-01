package com.petterp.floatingx.assist

/** Fx插入的不同位置 */
enum class FxScopeType {
    APP, // 在应用内展示，无需申请权限
    SYSTEM, // 在系统中展示，必须申请权限，不会主动降级
    SYSTEM_AUTO; // 优先System，没有权限时自动降级为应用内浮窗

    val hasPermission: Boolean
        get() = this != APP
}
