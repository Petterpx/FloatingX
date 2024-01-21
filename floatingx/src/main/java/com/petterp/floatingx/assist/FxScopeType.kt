package com.petterp.floatingx.assist

/** Fx插入的不同位置 */
enum class FxScopeType {
    SYSTEM, // 在系统中展示，必须申请权限，不会主动降级
    AUTO_APP, // 根据用户的权限授予状态，Windows || Activity 自适应处理
    AUTO_SYSTEM, // 根据用户的权限授予状态，系统 || Activity 自适应处理
    APP_WINDOWS, // 在应用内展示，需要申请权限
    APP_ACTIVITY; // 在应用内展示，无需申请权限

    val hasPermission: Boolean
        get() = this != APP_ACTIVITY

    val enableAuto: Boolean
        get() = this == AUTO_APP || this == AUTO_SYSTEM
}
