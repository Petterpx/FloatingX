package com.petterp.floatingx.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.simple.FxAnimationImpl
import com.petterp.floatingx.app.test.MultipleFxActivity
import com.petterp.floatingx.util.createFx

class MainActivity : AppCompatActivity() {

    private lateinit var viewGroup: FrameLayout

    private val activityFx by createFx {
        setLayout(R.layout.item_floating)
        setEnableLog(true, "activityFx")
        build().toControl(this@MainActivity)
    }

    private val viewFx by createFx {
        setLayout(R.layout.item_floating)
        setEnableScrollOutsideScreen(false)
        setAnimationImpl(FxAnimationImpl())
        setEnableLog(true, "main_fx")
        build().toControl(viewGroup)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            viewGroup = addScopeFrameViewGroup()
            addNestedScrollView {
                addLinearLayout {
                    addItemView("显示全局悬浮窗") {
                        FloatingX.control(MultipleFxActivity.TAG_1).apply {
                            updateViewContent { holder ->
                                val tv = holder.getViewOrNull<TextView>(R.id.tvItemFx)
                                tv?.setOnClickListener {
                                    Toast.makeText(this@MainActivity, "文字被点击", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.show(this@MainActivity)
                    }
                    addItemView("隐藏全局悬浮窗") {
                        FloatingX.control(MultipleFxActivity.TAG_1).hide()
                    }
                    addItemView("更新当前[全局浮窗]内容-(layoutId方式)") {
                        FloatingX.control(MultipleFxActivity.TAG_1).apply {
                            updateView(R.layout.item_floating)
                            this.updateViewContent {
                                it.setText(R.id.tvItemFx, "App")
                            }
                        }.show(this@MainActivity)
                    }
                    addItemView("更新当前[全局浮窗]内容-(传递view方式)") {
                        FloatingX.control(MultipleFxActivity.TAG_1).apply {
                            updateView {
                                TextView(it).apply {
                                    layoutParams = ViewGroup.LayoutParams(50.dp, 50.dp)
                                    text = "App"
                                    gravity = Gravity.CENTER
                                    textSize = 15f
                                    setBackgroundColor(Color.GRAY)
                                }
                            }
                            show(this@MainActivity)
                        }
                    }
                    addItemView("显示一个Activity悬浮窗-(展示与多指触摸)") {
                        activityFx.show()
                        activityFx.updateViewContent {
                            it.setText(R.id.tvItemFx, "Act")
                            it.getView<CardView>(R.id.cardItemFx).setCardBackgroundColor(Color.BLUE)
                        }
                    }
                    addItemView("显示一个View级别悬浮窗-(展示与多指触摸)") {
                        viewFx.show()
                        viewFx.updateViewContent {
                            it.setText(R.id.tvItemFx, "view")
                            it.getView<CardView>(R.id.cardItemFx)
                                .setCardBackgroundColor(Color.GREEN)
                        }
                    }
                    addItemView("显示windows级别悬浮窗") {
//                        val config =
//                            FxAppHelper.builder().setLayout(R.layout.item_floating)
//                                .setEnableLog(true, "windows")
//                                .setContext(applicationContext).build()
//                        val layoutParam = WindowManager.LayoutParams().apply {
//                            // 设置大小 自适应
//                            width = WindowManager.LayoutParams.WRAP_CONTENT
//                            height = WindowManager.LayoutParams.WRAP_CONTENT
//                            format = PixelFormat.TRANSPARENT
//                            /**
//                             * 注意，flag的值可以为：
//                             * 下面的flags属性的效果形同“锁定”。
//                             * 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
//                             * LayoutParams.FLAG_NOT_TOUCH_MODAL 不影响后面的事件
//                             * LayoutParams.FLAG_NOT_FOCUSABLE 不可聚焦
//                             * LayoutParams.FLAG_NOT_TOUCHABLE 不可触摸
//                             */
//                            flags =
//                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//                            } else {
//                                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
//                            }
//                        }
//                        val fx = FxManagerView(applicationContext).init(config)
//                        fx.windowManager = windowManager
//                        windowManager.addView(fx, layoutParam)
                        windowManager
                    }
                    addItemView("进入测试页面") {
                        TestActivity::class.java.start(this@MainActivity)
                    }
                }
            }
        }
    }

    private fun ViewGroup.addScopeFrameViewGroup(): FrameLayout {
        val viewGroup = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                300,
            ).apply {
                leftMargin = 50
                topMargin = 50
                rightMargin = 50
                bottomMargin = 50
            }
            setBackgroundColor(Color.YELLOW)
        }
        addView(viewGroup)
        return viewGroup
    }
}

class ItemViewTouchListener(val wl: WindowManager.LayoutParams, val windowManager: WindowManager) :
    View.OnTouchListener {
    private var x = 0
    private var y = 0
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                x = motionEvent.rawX.toInt()
                y = motionEvent.rawY.toInt()
            }

            MotionEvent.ACTION_MOVE -> {
                val nowX = motionEvent.rawX.toInt()
                val nowY = motionEvent.rawY.toInt()
                val movedX = nowX - x
                val movedY = nowY - y
                x = nowX
                y = nowY
                wl.apply {
                    x += movedX
                    y += movedY
                }
                // 更新悬浮球控件位置
                windowManager?.updateViewLayout(view, wl)
            }

            else -> {
            }
        }
        return false
    }
}
