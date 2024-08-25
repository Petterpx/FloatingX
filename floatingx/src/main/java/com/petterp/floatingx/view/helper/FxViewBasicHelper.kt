package com.petterp.floatingx.view.helper

import android.content.res.Configuration
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.view.FxBasicContainerView

/**
 * 基础类的辅助助手,用于分发基础逻辑
 * @author petterp
 */
abstract class FxViewBasicHelper {
    protected var basicView: FxBasicContainerView? = null
    protected lateinit var config: FxBasisHelper

    open fun initConfig(parentView: FxBasicContainerView) {
        this.basicView = parentView
        this.config = parentView.helper
    }

    open fun onInit() {}

    open fun onPreCancel() {}

    open fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {}

    open fun onConfigurationChanged(config: Configuration) {}
}
