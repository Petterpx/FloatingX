package com.petterp.floatingx.core.feature

import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.FxLogger
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxLayoutInput

/** feature 能看到的全部东西；由 FxControlImpl 实现 */
public interface FxFeatureScope {
    public val control: FxControl
    public val config: FxConfig
    public val container: FxContainer
    public val host: FxHost
    public val logger: FxLogger?

    /** 当前布局输入；内容尺寸尚无效时为 null */
    public fun layoutInput(): FxLayoutInput?

    /** 提交新锚点：更新 control.anchor、持久化、回调 onPositionChanged */
    public fun commitAnchor(anchor: FxAnchor)

    /** 向所有 FxListener 广播 */
    public fun dispatch(block: (FxListener) -> Unit)

    /** 请求按当前锚点重新定位 */
    public fun requestRelayout()
}
