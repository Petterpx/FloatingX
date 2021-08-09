package com.petterp.floatingx.app

import android.app.Application
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.Direction
import com.petterp.floatingx.impl.simple.FxAnimationImpl
import com.petterp.floatingx.impl.simple.FxConfigStorageToSpImpl

/**
 * @Author petterp
 * @Date 2021/5/21-5:42 下午
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
class CustomApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FloatingX.init {
            // 设置context
            setContext(this@CustomApplication)
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
            setOnClickListener { }
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
//            defaultSaveDirection(applicationContext)

            // 设置底部偏移量
            setBorderBorderMargin(100f)
            // 设置顶部偏移量
            setTopBorderMargin(100f)
            // 设置左侧偏移量
            setLeftBorderMargin(100f)
            // 设置右侧偏移量
            setRightBorderMargin(100f)

            // 设置悬浮窗LayoutParams
//            setLayoutParams()
            // 设置要显示的activity
            addBlackClass(
                MainActivity::class.java,
                ImmersedActivity::class.java
            )
            // 设置tag-Activity
            setTagActivityLifecycle {
                onCreated { activity, bundle ->
                }
                onResumes { }
            }
            setEnableLog(true)
            // 只有调用了show,默认才会启用fx,否则fx不会自动插入activity
//            show()
        }

        FloatingX.init {
            setContext(this@CustomApplication)
            setLayout(R.layout.item_floating_new)
            show()
        }
    }
}
