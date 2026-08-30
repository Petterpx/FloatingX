package com.petterp.floatingx.compose

import androidx.compose.runtime.Composable
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.config.FxConfigScope

/** DSL：`FloatingX.install("tag") { compose { ctrl -> … }; appHost(app) }`。同一配置里只调用一次（Task 5 加去重） */
@JvmSynthetic
public fun FxConfigScope.compose(content: @Composable (FxControl) -> Unit) {
    content(FxComposeContent(content))
    addFeature(ComposeOwnerFeature())
}
