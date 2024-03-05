package com.petterp.floatingx.view.helper

import android.content.res.Configuration
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.view.FxBasicContainerViewHelper

/**
 * 基础类的辅助助手,用于分发基础逻辑
 * @author petterp
 */
abstract class FxViewBasicHelper {
    protected var basicView: FxBasicContainerViewHelper? = null
    protected lateinit var config: FxBasisHelper

    open fun initConfig(parentView: FxBasicContainerViewHelper) {
        this.basicView = parentView
        this.config = parentView.helper
    }

    open fun onInit() {}

    open fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {}

    open fun onConfigurationChanged(config: Configuration) {}
}
