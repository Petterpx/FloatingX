package com.petterp.floatingx.listener

/**
 * fxConfig位置信息接口
 *
 * fx不关心逻辑,只需要实现fx默认的方法即可,具体逻辑自行实现即可。具体可参考 FxConfigStorageToSpImpl
 * */
interface IFxConfigStorage {

    fun getX(): Float

    fun getY(): Float

    fun update(x: Float, y: Float)

    fun hasConfig(): Boolean

    fun clear()
}
