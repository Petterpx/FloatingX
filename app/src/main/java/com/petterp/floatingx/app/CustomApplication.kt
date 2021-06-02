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
            context(this@CustomApplication)
            layout(R.layout.item_floating)
            defaultDirection(Direction.RIGHT_OR_TOP)
            addBlackClass(
                MainActivity::class.java,
                NewActivity::class.java,
                ImmersedActivity::class.java
            )
            y(70f)
            marginEdge(0f)
        }.isDebug(true)
    }
}
