package com.petterp.floatingx.app

import android.app.Application
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
            setEnableLog()
            // 启用辅助方向
            setEnableAssistDirection(true)
            setEnableConfig()
            addBlackClass(
                MainActivity::class.java,
                NewActivity::class.java,
                ImmersedActivity::class.java
            )
            // 只有调用了show,默认才会启用fx,否则fx不会自动插入activity
            show()
        }
    }
}
