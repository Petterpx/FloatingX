package com.petterp.floatingx.view

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

/** 系统悬浮窗View */
@SuppressLint("ViewConstructor")
class FxSystemContainerView @JvmOverloads constructor(
    override val helper: FxAppHelper,
    private val wm: WindowManager,
    context: Context,
    attrs: AttributeSet? = null,
) : FxBasicContainerView(helper, context, attrs) {

    private lateinit var wl: WindowManager.LayoutParams

    private var downTouchX = 0f
    private var downTouchY = 0f

    val isAttachToWM: Boolean
        get() = windowToken != null

    override fun initView() {
        super.initView()
        installChildView() ?: return
        initWLParams()
    }

    internal fun registerWM(wm: WindowManager) {
        try {
            if (isAttachToWM) return
            wm.addView(this, wl)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun currentX(): Float {
        return wl.x.toFloat()
    }

    override fun currentY(): Float {
        return wl.y.toFloat()
    }

    override fun preCheckPointerDownTouch(event: MotionEvent): Boolean {
        // 当前屏幕存在手指时，check当前手势是否真的在浮窗之上
        return checkPointerDownTouch(this, event)
    }

    override fun onTouchDown(event: MotionEvent) {
        downTouchX = wl.x.minus(event.rawX)
        downTouchY = wl.y.minus(event.rawY)
    }

    override fun onTouchMove(event: MotionEvent) {
        val x = downTouchX.plus(event.rawX)
        val y = downTouchY.plus(event.rawY)
        safeUpdatingXY(x, y)
    }

    override fun onTouchCancel(event: MotionEvent) {
        downTouchX = 0f
        downTouchY = 0f
    }

    override fun updateXY(x: Float, y: Float) {
        wl.x = x.toInt()
        wl.y = y.toInt()
        wm.updateViewLayout(this, wl)
    }

    override fun parentSize(): Pair<Int, Int> {
        return helper.context.screenWidth to helper.context.screenHeight
    }

    internal fun updateFlags(enableHalfHide: Boolean) {
        wl.flags = findFlags(enableHalfHide)
        wm.updateViewLayout(this, wl)
    }

    private fun findFlags(enableHalfHide: Boolean): Int {
        var flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        if (enableHalfHide) {
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        return flags
    }

    private fun initWLParams() {
        wl = WindowManager.LayoutParams().apply {
            width = helper.layoutParams?.width ?: WindowManager.LayoutParams.WRAP_CONTENT
            height = helper.layoutParams?.height ?: WindowManager.LayoutParams.WRAP_CONTENT
            format = PixelFormat.RGBA_8888
            gravity = Gravity.TOP or Gravity.START
            flags = findFlags(helper.enableHalfHide)
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
        }
    }
}
