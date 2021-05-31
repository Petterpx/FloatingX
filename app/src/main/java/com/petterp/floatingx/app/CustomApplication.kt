package com.petterp.floatingx.app

import android.app.Application
import android.util.Log
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.config.FxHelper
import com.petterp.floatingx.config.Direction
import com.petterp.floatingx.listener.IFxScrollListener
import com.petterp.floatingx.listener.IFxViewLifecycle

/**
 * @Author petterp
 * @Date 2021/5/21-5:42 下午
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
class CustomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = FxHelper.builder()
            .layout(R.layout.item_floating)
            .x(100f)
            .y(100f)
            .defaultDirection(Direction.RIGHT_OR_BOTTOM)
            .setViewLifecycle(object : IFxViewLifecycle {
                override fun attach() {
                    Log.e("petterp", "attach")
                }

                override fun detached() {
                    Log.e("petterp", "detached")
                }

                override fun windowsVisibility(visibility: Int) {
                    Log.e("petterp", "visibility")
                }
            })
            .setScrollListener(object : IFxScrollListener {
                override fun down() {
                    Log.e("petterp", "down")
                }

                override fun up() {
                    Log.e("petterp", "up")
                }

                override fun dragIng(x: Float, y: Float) {
                    Log.e("petterp", "dragIng--x-$x---y-$y")
                }
            })
            .context(this)
            .marginEdge(10f)
            .isEdgeEnable(true)
            .addBlackClass(MainActivity::class.java, NewActivity::class.java)
            .build()
        FloatingX.init(config).isDebug(true)
    }
}
