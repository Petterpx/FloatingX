package com.petterp.floatingx.core.gesture

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import kotlin.math.abs

/**
 * 点击 / 长按 / 拖动 判定（spec §2.4）。
 * - 全部用 actionMasked；跟踪主指针 id，主指抬起时把控制权转移给剩下的手指
 * - 长按在按下期间由定时器触发，与是否有点击监听无关（#218）
 * - 点击 = 抬起时仍在 slop 内且长按未触发；无额外时间阈值
 * - MOVE 路径零分配
 *
 * 容器的 onInterceptTouchEvent → onIntercept()；onTouchEvent → onTouch()。
 * 收到的坐标均为容器坐标。
 */
internal class FxGestureDetector(
    private val touchSlop: Float,
    private val defaultLongPressTimeout: Long,
    private val callback: Callback,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {

    internal interface Callback {
        fun onClick()
        fun onLongPress()
        fun onDragStart()

        /** 相对上一次事件的增量 */
        fun onDrag(dx: Float, dy: Float)
        fun onDragEnd()

        /** DOWN 落点是否允许起拖（dragRegion） */
        fun canDragFrom(x: Float, y: Float): Boolean

        /** DOWN 落点下是否有可滚动子 view（AUTO 优先级用） */
        fun hasScrollableChildAt(x: Float, y: Float): Boolean
    }

    var config: FxGesture = FxGesture.Normal

    private var pointerId = INVALID
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var moved = false
    private var longPressed = false
    private var dragging = false
    private var canDrag = false
    private var childScrollable = false

    private val longPressRunnable = Runnable {
        if (pointerId == INVALID || moved) return@Runnable
        longPressed = true
        if (config.longPress) callback.onLongPress()
    }

    /** 返回 true 表示从此拦截，子 view 收到 CANCEL */
    fun onIntercept(ev: MotionEvent): Boolean {
        if (!config.touchable) return false
        return when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> { begin(ev); false }
            MotionEvent.ACTION_MOVE -> {
                if (pointerId == INVALID) return false
                if (config.childPriority == FxChildPriority.CHILD) return false
                if (config.childPriority == FxChildPriority.AUTO && childScrollable && config.drag == FxDrag.IMMEDIATE) return false
                val idx = ev.findPointerIndex(pointerId)
                if (idx < 0) return false
                val x = ev.getX(idx)
                val y = ev.getY(idx)
                updateMoved(x, y)
                if (shouldStartDrag()) { startDrag(); true } else false
            }
            // 子 view 消费了整条事件流：不产生点击
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { reset(); false }
            else -> false
        }
    }

    fun onTouch(ev: MotionEvent): Boolean {
        if (!config.touchable) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (pointerId == INVALID) begin(ev) // 已在 onIntercept 里 begin 过则跳过
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = ev.findPointerIndex(pointerId)
                if (idx < 0) return true
                val x = ev.getX(idx)
                val y = ev.getY(idx)
                if (!dragging) {
                    updateMoved(x, y)
                    if (shouldStartDrag()) startDrag()
                }
                if (dragging) {
                    callback.onDrag(x - lastX, y - lastY)
                    lastX = x
                    lastY = y
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val idx = ev.actionIndex
                if (ev.getPointerId(idx) == pointerId) {
                    val newIdx = if (idx == 0) 1 else 0
                    pointerId = ev.getPointerId(newIdx)
                    lastX = ev.getX(newIdx)
                    lastY = ev.getY(newIdx)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    callback.onDragEnd()
                } else if (!moved && !longPressed && config.click) {
                    callback.onClick()
                }
                reset()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (dragging) callback.onDragEnd()
                reset()
                return true
            }
            else -> return true
        }
    }

    /** 容器 detach 或配置切换时调用 */
    fun cancel() {
        if (dragging) callback.onDragEnd()
        reset()
    }

    private fun begin(ev: MotionEvent) {
        reset()
        pointerId = ev.getPointerId(0)
        downX = ev.x
        downY = ev.y
        lastX = downX
        lastY = downY
        canDrag = config.drag != FxDrag.DISABLED && callback.canDragFrom(downX, downY)
        childScrollable = callback.hasScrollableChildAt(downX, downY)
        if (config.longPress || config.drag == FxDrag.AFTER_LONG_PRESS) {
            val timeout = if (config.longPressTimeout > 0) config.longPressTimeout else defaultLongPressTimeout
            handler.postDelayed(longPressRunnable, timeout)
        }
    }

    private fun updateMoved(x: Float, y: Float) {
        if (moved) return
        if (abs(x - downX) > touchSlop || abs(y - downY) > touchSlop) {
            moved = true
            if (!longPressed) handler.removeCallbacks(longPressRunnable)
        }
    }

    private fun shouldStartDrag(): Boolean = canDrag && when (config.drag) {
        FxDrag.IMMEDIATE -> moved
        FxDrag.AFTER_LONG_PRESS -> longPressed
        FxDrag.DISABLED -> false
    }

    /** 第一段增量从 DOWN 点算起，所以 lastX/lastY 回到 down 位置 */
    private fun startDrag() {
        dragging = true
        handler.removeCallbacks(longPressRunnable)
        lastX = downX
        lastY = downY
        callback.onDragStart()
    }

    private fun reset() {
        handler.removeCallbacks(longPressRunnable)
        pointerId = INVALID
        moved = false
        longPressed = false
        dragging = false
        canDrag = false
        childScrollable = false
    }

    private companion object {
        const val INVALID = -1
    }
}
