package com.petterp.floatingx.assist

/** Fx插入的不同位置 */
enum class FxScopeType {
    SYSTEM, // 在系统中展示，必须有权限
    AUTO_APP, // 根据用户的权限授予状态，Windows||Activity 自适应处理
    APP_WINDOWS, // 在应用内展示，需要权限
    APP_ACTIVITY, // 在应用内展示，无需权限
}
