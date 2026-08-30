package com.petterp.floatingx.compose

import androidx.compose.runtime.Composable
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.config.FxConfigScope

/**
 * DSL：`FloatingX.install("tag") { compose { ctrl -> … }; appHost(app) }`。
 *
 * 重复调用是幂等的：内容取最后一次，ComposeOwnerFeature 始终只有一个。
 * 已有 feature 会被复用而不是换成新实例——换了实例，旧内容的 owner 就没人负责 destroy 了
 * （`control.update { compose { … } }` 会把旧配置里的 feature 一起带过来）。
 *
 * 配置里意外存在多个 ComposeOwnerFeature（手动 addFeature 过）时，只保留**第一个**复用，
 * 其余的摘掉；被摘掉的实例会经由 `FxFeature.onRemove()` 自己释放持有的 owner，不会漏。
 */
@JvmSynthetic
public fun FxConfigScope.compose(content: @Composable (FxControl) -> Unit) {
    val next = FxComposeContent(content)
    this.content(next)
    var owned: ComposeOwnerFeature? = null
    removeFeatures { feature ->
        val hit = feature as? ComposeOwnerFeature
        if (hit != null && owned == null) owned = hit
        hit != null
    }
    addFeature(owned?.also { it.declare(next) } ?: ComposeOwnerFeature(next))
}
