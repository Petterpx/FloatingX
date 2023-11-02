package com.petterp.floatingx.assist

/** Fx插入的不同位置 */
enum class FxScopeEnum(val tag: String) {
    SYSTEM("system"),
    APP_SCOPE("app"),
    ACTIVITY_SCOPE("activity"),
    FRAGMENT_SCOPE("fragment"),
    VIEW_GROUP_SCOPE("view")
}
