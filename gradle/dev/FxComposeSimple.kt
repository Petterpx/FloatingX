package com.petterp.floatingx.app.kotlin

import android.app.Application
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.assist.FxScopeType
import com.petterp.floatingx.compose.enableComposeSupport

/**
 *
 * @author petterp
 */
object FxComposeSimple {
    fun install(context: Application) {
        FloatingX.install {
            setContext(context)
            setTag("compose")
            //system浮窗必须调用此方法,才可以启用Compose支持
            enableComposeSupport()
            setScopeType(FxScopeType.SYSTEM)
            setGravity(FxGravity.RIGHT_OR_BOTTOM)
            setOffsetXY(10f, 10f)
            setEnableLog(true)
            setEdgeOffset(20f)
            setEnableEdgeAdsorption(true)
        }.show()
    }
}