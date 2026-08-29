package com.petterp.floatingx.core.storage

import android.content.Context
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity

/** 基于 SharedPreferences 的默认实现，值格式 `GRAVITY,dx,dy` */
public class FxSpStorage @JvmOverloads constructor(
    context: Context,
    name: String = "floatingx_anchor",
) : FxStorage {

    private val sp = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun save(key: String, anchor: FxAnchor) {
        sp.edit().putString(key, "${anchor.gravity.name},${anchor.dx},${anchor.dy}").apply()
    }

    override fun load(key: String): FxAnchor? {
        val raw = sp.getString(key, null) ?: return null
        val parts = raw.split(',')
        if (parts.size != 3) return null
        val gravity = runCatching { FxGravity.valueOf(parts[0]) }.getOrNull() ?: return null
        val dx = parts[1].toFloatOrNull() ?: return null
        val dy = parts[2].toFloatOrNull() ?: return null
        return FxAnchor(gravity, dx, dy)
    }

    override fun clear(key: String) {
        sp.edit().remove(key).apply()
    }
}
