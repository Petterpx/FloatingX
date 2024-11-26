package com.petterp.floatingx.app.kotlin

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.dp
import com.petterp.floatingx.app.dpF
import com.petterp.floatingx.app.test.MultipleFxActivity
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.assist.FxScopeType
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxProxyTagActivityLifecycle
import com.petterp.floatingx.listener.IFxTouchListener
import com.petterp.floatingx.view.IFxInternalHelper

/**
 * App浮窗
 * @author petterp
 */
object FxAppSimple {
    fun install(context: Application) {
        FloatingX.install {
            setContext(context)
            setScopeType(FxScopeType.APP)
            setGravity(FxGravity.LEFT_OR_BOTTOM)
            setOffsetXY(10f, 10f)
            setLayoutView(
                CardView(context).apply {
                    setCardBackgroundColor(Color.GRAY)
                    radius = 30.dpF
                    addView(
                        TextView(this.context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                -2,
                                60.dp,
                            )
                            gravity = Gravity.CENTER
                            text = "浮窗2-act"
                            setTextColor(Color.WHITE)
                            textSize = 15f
                        },
                    )
                },
            )
            setTouchListener(object : IFxTouchListener {
                override fun onTouch(event: MotionEvent, control: IFxInternalHelper?): Boolean {
                    return false
                }
            })
            setOnClickListener {
                Toast.makeText(context, "浮窗2被点击", Toast.LENGTH_SHORT).show()
            }
            setTag(MultipleFxActivity.TAG_2)
            setEnableLog(true)
            setEdgeOffset(20f)
            setEnableEdgeAdsorption(true)
        }.show()
    }
}