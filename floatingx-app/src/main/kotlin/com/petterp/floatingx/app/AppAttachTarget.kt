package com.petterp.floatingx.app

/** AppHost 把容器挂到 Activity 的哪一层 */
public enum class AppAttachTarget {
    /** Window 的 DecorView（默认）：拖动范围是整个窗口，不受状态栏/导航栏裁剪（spec §3） */
    DECOR,

    /** android.R.id.content：只覆盖内容区 */
    CONTENT,
}
