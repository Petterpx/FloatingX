package com.petterp.floatingx.core.host

/** host → engine 的事件通道 */
public interface FxHostSession {
    /** 已有可挂载的父容器且尺寸有效 */
    public fun onHostReady()

    /** 父容器消失（Activity destroy / ViewGroup detach / 权限撤销） */
    public fun onHostLost()

    /** 旋转、insets、分屏等导致可用区变化 */
    public fun onBoundsChanged()

    /** 当前 host 不可用且有替代方案（如 system 权限被拒降级到 app） */
    public fun requestSwap(fallback: FxHost)
}
