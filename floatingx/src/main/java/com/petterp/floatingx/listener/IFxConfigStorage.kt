package com.petterp.floatingx.listener

/**
 * @Author petterp
 * @Date 2021/6/8-9:35 上午
 * @Email ShiyihuiCloud@163.com
 * @Function fxConfig配置信息，实现此接口，实现自己的保存方式
 * 示例 FxConfigSpImpl
 */
interface IFxConfigStorage {

    fun getX(): Float
    fun setX(x: Float)

    fun getY(): Float
    fun setY(y: Float)

    /** 设置当前存储版本号 */
    fun setVersionCode(version: Int)

    /** 获取当前版本号 */
    fun getVersionCode(): Int = 0

    /** 默认只有版本号大于0,才断定存在历史位置 */
    fun hasConfig() = getVersionCode() > 0

    fun clear()
}
