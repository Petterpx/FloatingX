package com.petterp.floatingx.view.basic

import com.petterp.floatingx.assist.helper.FxBasisHelper

/**
 * 基础类的辅助助手,用于分发基础逻辑
 * @author petterp
 */
abstract class FxBasicViewHelper {
    protected var basicView: FxBasicParentView? = null
    protected lateinit var config: FxBasisHelper

    open fun initConfig(parentView: FxBasicParentView) {
        this.basicView = parentView
        this.config = parentView.helper
    }
}
