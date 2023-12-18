package com.petterp.floatingx.view.system

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.util.screenHeight
import com.petterp.floatingx.util.screenWidth
import com.petterp.floatingx.view.basic.FxBasicParentView

/** 基础悬浮窗View */
@SuppressLint("ViewConstructor")
class FxSystemContainerView @JvmOverloads constructor(
    override val helper: FxAppHelper,
    private val wm: WindowManager,
    context: Context,
    attrs: AttributeSet? = null,
) : FxBasicParentView(helper, context, attrs) {

    private lateinit var wl: WindowManager.LayoutParams
    private var cX = 0f
    private var cY = 0f

    val isAttachToWM: Boolean
        get() = windowToken != null

    override fun initView() {
        super.initView()
        initChildView() ?: return
        initWLParams()
    }

    internal fun registerWM(wm: WindowManager) {
        if (isAttachToWM) return
        wm.addView(this, wl)
    }

    override fun moveLocation(x: Float, y: Float, useAnimation: Boolean) {
        wl.x = x.toInt()
        wl.y = y.toInt()
        wm.updateViewLayout(this, wl)
    }

    override fun onLayoutInit() {
        val width = helper.context.screenWidth
        val height = helper.context.screenHeight
        val viewH = this.height
        val viewW = this.width
        val (defaultX, defaultY) = helper.defaultXY(width, height, viewW, viewH)
        wl.x = defaultX
        wl.y = defaultY
        wm.updateViewLayout(this, wl)
    }

    override fun onTouchDown(event: MotionEvent) {
        cX = event.rawX
        cY = event.rawY
    }

    override fun onTouchMove(event: MotionEvent) {
        val nowX = event.rawX
        val nowY = event.rawY
        val movedX = nowX - cX
        val movedY = nowY - cY
        cX = nowX
        cY = nowY
        wl.x += movedX.toInt()
        wl.y += movedY.toInt()
        wm.updateViewLayout(this, wl)
    }

    override fun onTouchCancel(event: MotionEvent) {
        cX = 0f
        cY = 0f
    }

    override fun interceptTouchEvent(ev: MotionEvent): Boolean {
        return false
    }

    private fun initWLParams() {
        wl = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            format = PixelFormat.RGBA_8888
            gravity = Gravity.TOP or Gravity.START
            flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
        }
    }
}
