package com.petterp.floatingx.app.kotlin

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.test.MultipleFxActivity
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
            setLayoutView(
                ComposeView(context).apply {
                    setContent {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(100.dp)
                                .background(Color.Yellow)
                        ) {
                            Text(text = "compose", modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            )
            setEnableLog(true)
            setEdgeOffset(20f)
            setEnableEdgeAdsorption(true)
        }.show()
    }
}