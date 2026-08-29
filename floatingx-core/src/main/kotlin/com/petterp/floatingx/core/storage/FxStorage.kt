package com.petterp.floatingx.core.storage

import com.petterp.floatingx.core.layout.FxAnchor

/**
 * 锚点持久化。key 由框架生成（`"$tag:$orientation"`），横竖屏分别记忆。
 * 写入点只有三个：拖动/吸附结束、moveTo 完成、update { anchor }。
 */
public interface FxStorage {
    public fun save(key: String, anchor: FxAnchor)
    public fun load(key: String): FxAnchor?
    public fun clear(key: String)
}
