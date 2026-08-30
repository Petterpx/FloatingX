package com.petterp.floatingx.app.internal

import android.app.Activity
import com.petterp.floatingx.app.AppActivityFilter

/**
 * AppHost 的 Activity 过滤规则。判定顺序：白名单（非空时必须命中）→ 黑名单 → 自定义过滤。
 * Class 条目用 isInstance 匹配（含子类，#221），String 条目与类全名精确相等。
 */
internal class ActivityRules(
    private val blackClasses: List<Class<out Activity>>,
    private val blackNames: Set<String>,
    private val whiteClasses: List<Class<out Activity>>,
    private val whiteNames: Set<String>,
    private val filters: List<AppActivityFilter>,
) {

    fun accept(activity: Activity): Boolean {
        val name = activity.javaClass.name
        val hasWhitelist = whiteClasses.isNotEmpty() || whiteNames.isNotEmpty()
        if (hasWhitelist && name !in whiteNames && whiteClasses.none { it.isInstance(activity) }) return false
        if (name in blackNames || blackClasses.any { it.isInstance(activity) }) return false
        for (f in filters) if (!f.accept(activity)) return false
        return true
    }

    companion object {
        val ACCEPT_ALL = ActivityRules(emptyList(), emptySet(), emptyList(), emptySet(), emptyList())
    }
}
