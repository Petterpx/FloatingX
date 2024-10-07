package com.petterp.floatingx.app.test

import android.app.Application
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.FxScopeType
import com.petterp.floatingx.compose.enableComposeSupport

object FloatingCompose {

    fun install(context: Application) {
        FloatingX.install {
            setContext(context)
            setTag("compose")
            enableComposeSupport()
            setScopeType(FxScopeType.SYSTEM)
            setLayoutView(
                ComposeView(context).apply {
                    setContent {
                        FloatingWindow()
                    }
                }
            )
            setEnableLog(true)
            setEdgeOffset(20f)
        }.show()
    }
}

@Composable
fun FloatingWindow() {
    var isOpen by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        targetValue = if (isOpen) 400.dp else 40.dp,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing), label = ""
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.LightGray)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Blue)
                    .clickable {
                        isOpen = !isOpen
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isOpen) "关" else "开",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
