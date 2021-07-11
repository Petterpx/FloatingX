package com.petterp.floatingx.listener

/**
 * @Author petterp
 * @Date 2021/5/25-4:05 下午
 * @Email ShiyihuiCloud@163.com
 * @Function fx-拖动监听事件
 */
interface IFxScrollListener {

    /** 按下 */
    fun down()

    /** 松开 */
    fun up()

    /** 正在拖拽 */
    fun dragIng(x: Float, y: Float)
}
