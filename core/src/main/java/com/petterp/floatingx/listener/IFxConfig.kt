package com.petterp.floatingx.listener

/**
 * @Author petterp
 * @Date 2021/6/8-9:35 上午
 * @Email ShiyihuiCloud@163.com
 * @Function fxConfig配置信息，实现此接口，实现自己的保存方式
 * @sample [FxConfigSpImpl]
 */
interface IFxConfig {

    fun getX(): Float
    fun setX(x: Float)

    fun getY(): Float
    fun setY(y: Float)

    fun setVersionCode(version: Int)

    fun getVersionCode(): Int

    fun hasConfig() = getVersionCode() > 0

    fun clear()
}
