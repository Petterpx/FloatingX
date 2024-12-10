package com.petterp.floatingx.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.EditText
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.util.FxInputHelper
import com.petterp.floatingx.util.realScreenHeight
import com.petterp.floatingx.util.screenWidth

/** 系统悬浮窗View */
@SuppressLint("ViewConstructor")
class FxSystemContainerView @JvmOverloads constructor(
    override val helper: FxAppHelper,
    private val wm: WindowManager,
    context: Context,
    attrs: AttributeSet? = null,
) : FxBasicContainerView(helper, context, attrs) {

    private var downTouchX = 0f
    private var downTouchY = 0f
    private var isShowKeyBoard = false
    private lateinit var wl: WindowManager.LayoutParams

    val isAttachToWM: Boolean
        get() = windowToken != null

    override fun initView() {
        super.initView()
        installChildView() ?: return
        initWLParams()
    }

    override fun onInitChildViewEnd(vh: FxViewHolder) {
        if (!helper.isEnableKeyBoardAdapt) return
        helper.editTextIds?.forEach {
            val editView = vh.getViewOrNull<EditText>(it)
            FxInputHelper.setEditTextAdapt(editView, helper.tag)
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
        return helper.context.screenWidth to helper.context.realScreenHeight
    }

    override fun dispatchKeyEventPreIme(event: KeyEvent?): Boolean {
        if (isShowKeyBoard && event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_BACK) {
            isShowKeyBoard = false
            updateKeyBoardStatus(false)
        }
        return super.dispatchKeyEventPreIme(event)
    }

    internal fun registerWM(wm: WindowManager) {
        try {
            if (isAttachToWM) return
            wm.addView(this, wl)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    internal fun updateEnableHalfStatus(enableHalfHide: Boolean) {
        wl.flags = defaultFlags.checkFullFlags(enableHalfHide)
        safeUpdateViewLayout(wl)
    }

    internal fun updateKeyBoardStatus(showKeyBoard: Boolean) {
        wl.flags = if (showKeyBoard) {
            isShowKeyBoard = true
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.checkFullFlags()
        } else {
            defaultFlags.checkFullFlags()
        }
        safeUpdateViewLayout(wl)
    }

    private fun initWLParams() {
        wl = WindowManager.LayoutParams().apply {
            width = helper.layoutParams?.width ?: WindowManager.LayoutParams.WRAP_CONTENT
            height = helper.layoutParams?.height ?: WindowManager.LayoutParams.WRAP_CONTENT
            format = PixelFormat.RGBA_8888
            gravity = Gravity.TOP or Gravity.START
            flags = defaultFlags.checkFullFlags()
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
        }
    }

    private fun safeUpdateViewLayout(lp: WindowManager.LayoutParams) {
        if (!isAttachToWM) return
        wm.updateViewLayout(this, lp)
    }

    private val defaultFlags: Int
        get() = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

    private fun Int.checkFullFlags(
        enableHalfHide: Boolean = helper.enableHalfHide,
        enableSafeArea: Boolean = helper.enableSafeArea
    ): Int {
        // 半悬浮(暂时似乎有点问题)
//        if (enableHalfHide) {
//            return this or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//        }
        // 不适配安全区域(导航栏状态栏可插入浮窗)
        if (enableHalfHide || !enableSafeArea) {
            return this or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
        return this
    }
}
