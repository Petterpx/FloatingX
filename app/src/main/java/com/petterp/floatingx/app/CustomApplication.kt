package com.petterp.floatingx.app

import android.app.Application
import android.util.Log
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.config.Direction

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
            setContext(this@CustomApplication)
            setLayout(R.layout.item_floating)
            setGravity(Direction.RIGHT_OR_BOTTOM)
            setEnableLog(true)
            // 启用辅助方向
            setEnableAssistDirection(true)
            setMoveEdgeMargin(10f)
            setEnableEdgeAdsorption(true)
            setEnableScrollOutsideScreen(true)
            setEnableConfig()
            addBlackClass(
                MainActivity::class.java,
                NewActivity::class.java,
                ImmersedActivity::class.java
            )
            setTagActivityLifecycle {
                onActivityPreResumed = {
                    Log.e("petterp", "onActivityPreResumed")
                }
            }
            // 只有调用了show,默认才会启用fx,否则fx不会自动插入activity
            show()
        }

//        val helper = FxHelper.builder()
//            .setContext(this)
//            .setLayout(R.layout.item_floating)
//            .setGravity(Direction.RIGHT_OR_BOTTOM)
//            .setEnableLog()
//            // 启用辅助方向
//            .setEnableAssistDirection(true)
//            .setEnableConfig()
//            .setEnableAssistDirection(true)
//            .setEnableEdgeRebound(true)
//            .setEnableLog()
//            .setLayoutParams()
//            .setEnableConfig(IFxConfigStorage)
//            .setOnClickListener(800L) {
//            }
//            .setLeftBorder(100f)
//            .setRightBorder(100f)
//            .setBottomBorder(100f)
//            .setTopBorder(100f)
//            .setMoveEdge(10f)
//            .setEnableEdgeAdsorption(true)
//            .addBlackClass(
//                MainActivity::class.java,
//                NewActivity::class.java,
//                ImmersedActivity::class.java
//            )
//            // 只有调用了show,默认才会启用fx,否则fx不会自动插入activity
//            .show()
//            .build()
//        FloatingX.init(helper)
    }
}
