package com.petterp.floatingx.impl.simple

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.petterp.floatingx.listener.IFxConfigStorage

/**
 * @Author petterp
 * @Date 2021/6/8-9:40 上午
 * @Email ShiyihuiCloud@163.com
 * @Function fx的sp存储示例
 */

class FxConfigStorageToSpImpl : IFxConfigStorage {

    private var sp: SharedPreferences? = null
    private var spEdit: SharedPreferences.Editor? = null

    companion object {
        private const val FX_SP_NAME = "floating_x_direction"
        private const val CONFIG_X = "saveX"
        private const val CONFIG_Y = "saveY"
        private const val CONFIG_VERSION_CODE = "fx_save_version_code"

        @SuppressLint("CommitPrefEdits")
        fun init(context: Context): IFxConfigStorage = FxConfigStorageToSpImpl().apply {
            sp = context.getSharedPreferences(FX_SP_NAME, Context.MODE_PRIVATE)
            spEdit = sp?.edit()
        }
    }

    override fun getX(): Float = sp?.getFloat(CONFIG_X, 0f) ?: 0f

    override fun setX(x: Float) {
        spEdit?.putFloat(CONFIG_X, x)?.commit()
    }

    override fun getY(): Float = sp?.getFloat(CONFIG_Y, 0f) ?: 0f

    override fun setY(y: Float) {
        spEdit?.putFloat(CONFIG_Y, y)?.commit()
    }

    override fun setVersionCode(version: Int) {
        spEdit?.putInt(CONFIG_VERSION_CODE, version)?.commit()
    }

    override fun getVersionCode(): Int = sp?.getInt(CONFIG_VERSION_CODE, 0) ?: 0

    override fun clear() {
        spEdit?.clear()?.commit()
    }
}
