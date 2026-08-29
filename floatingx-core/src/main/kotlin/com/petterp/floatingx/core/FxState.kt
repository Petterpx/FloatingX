package com.petterp.floatingx.core

/** 浮窗生命周期状态（spec §2.2） */
public enum class FxState {
    /** 已创建，容器尚未挂到任何 host（或 host 暂不可用） */
    INSTALLED,

    /** 容器已挂载但内容不可见 */
    ATTACHED,

    /** 内容可见 */
    SHOWN,

    /** 已销毁，终态 */
    CANCELLED,
}
