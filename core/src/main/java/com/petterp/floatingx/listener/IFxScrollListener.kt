package com.petterp.floatingx.listener

/**
 * @Author petterp
 * @Date 2021/5/25-4:05 下午
 * @Email ShiyihuiCloud@163.com
 * @Function 拖动监听事件
 */
interface IFxScrollListener {

    fun down()

    fun up()

    /** 正在拖动 */
    fun dragIng(x: Float, y: Float)
}
