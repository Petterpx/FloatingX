package com.petterp.floatingx.listener

/**
 * fx-拖动监听事件
 * */
interface IFxScrollListener {

    /** 按下 */
    fun down()

    /** 松开 */
    fun up()

    /** 正在拖拽 */
    fun dragIng(x: Float, y: Float)
}
