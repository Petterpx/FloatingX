package com.petterp.floatingx.app.kotlin

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.MotionEvent
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.ImmersedActivity
import com.petterp.floatingx.app.MainActivity
import com.petterp.floatingx.app.R
import com.petterp.floatingx.app.ScopeActivity
import com.petterp.floatingx.app.simple.FxAnimationImpl
import com.petterp.floatingx.app.simple.FxConfigStorageToSpImpl
import com.petterp.floatingx.assist.Direction
import com.petterp.floatingx.impl.FxScrollImpl
import com.petterp.floatingx.impl.lifecycle.FxTagActivityLifecycleImpl

/** Kotlin-Application */
class CustomKtApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FloatingX.init {
            // 设置悬浮窗layout
            setLayout(R.layout.item_floating)
            // 设置悬浮窗默认方向
            setGravity(Direction.RIGHT_OR_BOTTOM)
            // 设置是否启用日志
            setEnableLog(true)

            // 启用辅助方向
            setEnableAssistDirection(0f, 0f, 0f, 100f)
            // 设置x轴默认坐标
//            setX()
            // 设置y轴默认坐标
//            setY()
            // 设置启用边缘吸附
            setEnableEdgeAdsorption(true)
            // 设置边缘偏移量
            setEdgeOffset(10f)
            // 设置启用悬浮窗可屏幕外回弹
            setEnableScrollOutsideScreen(true)
            // 设置辅助方向辅助
            // 设置点击事件
//            setOnClickListener { }
            // 设置view-lifecycle监听
//            setViewLifecycle()
            // 设置启用悬浮窗位置修复
            setEnableAbsoluteFix(true)
            // 设置启用动画
            setEnableAnimation(true)
            // 设置启用动画实现
            setAnimationImpl(FxAnimationImpl())
            // 设置方向保存impl
            setSaveDirectionImpl(FxConfigStorageToSpImpl(applicationContext))

            // 设置底部偏移量
            setBottomBorderMargin(100f)
            // 设置顶部偏移量
//            setTopBorderMargin(100f)
            // 设置左侧偏移量
            setLeftBorderMargin(100f)
            // 设置右侧偏移量
            setRightBorderMargin(100f)
            // 设置允许触摸事件
            setEnableTouch(true)

            // 设置悬浮窗LayoutParams
//            setLayoutParams()

            // 指定浮窗可显示的activity方式
            // 1.设置是否允许所有activity都进行显示,默认false
//            setEnableAllBlackClass(false)
            // 2.设置是否只允许显示在特定的页面
            addBlackClass(
                MainActivity::class.java,
                ImmersedActivity::class.java,
                ScopeActivity::class.java,
            )
            // 3. 设置允许所有activity进行显示，同时增加过滤列表
//            setEnableAllBlackClass(true, MainActivity::class.java)

            // 设置tag-Activity生命周期回调时的触发
            setTagActivityLifecycle(object : FxTagActivityLifecycleImpl() {
                override fun onCreated(activity: Activity, bundle: Bundle?) {
                    // 允许插入的浮窗activity执行到onCreated时会回调相应方法
                }
            })
            // 设置滑动监听
            setScrollListener(object : FxScrollImpl() {
                override fun down() {
                    // 按下
                }

                override fun up() {
                    // 释放
                }

                override fun dragIng(event: MotionEvent, x: Float, y: Float) {
                    // 正在拖动
                }

                override fun eventIng(event: MotionEvent) {
                    // 接收所有事件传递
                }
            })
            // 只有调用了enableFx,默认才会启用fx,否则fx不会自动插入activity
            // ps: 这里的只有调用了enableFx仅仅只是配置工具层的标记,后续使用control.show()也会默认启用
            enableFx()
        }
    }
}
