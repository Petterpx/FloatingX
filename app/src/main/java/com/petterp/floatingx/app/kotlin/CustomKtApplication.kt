package com.petterp.floatingx.app.kotlin

import android.app.Application
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.ImmersedActivity
import com.petterp.floatingx.app.MainActivity
import com.petterp.floatingx.app.R
import com.petterp.floatingx.assist.Direction
import com.petterp.floatingx.listener.IFxScrollListener

/** Kotlin-Application */
class CustomKtApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FloatingX.init {
            // 设置context
            setContext(this@CustomKtApplication)
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

            /** 对于浮窗允许显示的位置进行调整 */
            // 1. 设置是否允许所有activity都进行显示,默认false
//            setEnableAllBlackClass(false)
            //  2.设置是否只允许显示在特定的页面
            addBlackClass(
                MainActivity::class.java,
                ImmersedActivity::class.java,
            )
            // 3. 设置允许所有activity进行显示，同时增加过滤列表
//            setEnableAllBlackClass(true, MainActivity::class.java)

            // 设置tag-Activity生命周期回调时的触发
            setTagActivityLifecycle {
                onCreated { activity, bundle ->
                }
                onResumes { }
            }
            setScrollListener(object : IFxScrollListener {
                override fun down() {
                    TODO("Not yet implemented")
                }

                override fun up() {
                    TODO("Not yet implemented")
                }

                override fun dragIng(x: Float, y: Float) {
                    TODO("Not yet implemented")
                }
            })
            // 只有调用了show,默认才会启用fx,否则fx不会自动插入activity
            show()
        }
    }
}
