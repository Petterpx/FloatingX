package com.petterp.floatingx.listener

import com.petterp.floatingx.assist.helper.FxBuilderDsl

/**
 * fxConfig位置信息接口
 *
 * fx不关心逻辑,只需要实现fx默认的方法即可,具体逻辑自行实现即可。具体可参考 FxConfigStorageToSpImpl
 * */
@FxBuilderDsl
interface IFxConfigStorage {

    /** x坐标 */
    fun getX(): Float

    /** y坐标 */
    fun getY(): Float

    /** 悬浮窗位置更新时调用 */
    fun update(x: Float, y: Float)

    /** 是否存在历史位置,返回true悬浮窗才会使用 */
    fun hasConfig(): Boolean

    /** 清除存储信息 */
    fun clear()
}
