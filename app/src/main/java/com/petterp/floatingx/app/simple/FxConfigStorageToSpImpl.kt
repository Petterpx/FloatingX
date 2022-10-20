package com.petterp.floatingx.app.simple

import android.content.Context
import android.content.SharedPreferences
import com.petterp.floatingx.listener.IFxConfigStorage

/** fx的sp存储示例*/
class FxConfigStorageToSpImpl(context: Context) : IFxConfigStorage {

    private var sp: SharedPreferences? = null
    private var spEdit: SharedPreferences.Editor? = null

    init {
        sp = context.getSharedPreferences(FX_SP_NAME, Context.MODE_PRIVATE)
        spEdit = sp?.edit()
    }

    companion object {
        private const val FX_SP_NAME = "floating_x_direction"
        private const val CONFIG_X = "saveX"
        private const val CONFIG_Y = "saveY"
        private const val CONFIG_VERSION_CODE = "fx_save_version_code"
    }

    override fun getX(): Float = sp?.getFloat(CONFIG_X, 0f) ?: 0f

    override fun getY(): Float = sp?.getFloat(CONFIG_Y, 0f) ?: 0f
    override fun update(x: Float, y: Float) {
        val version = sp?.getInt(CONFIG_VERSION_CODE, 0) ?: 0
        spEdit?.putFloat(CONFIG_X, x)?.putFloat(CONFIG_Y, y)
            ?.putInt(CONFIG_VERSION_CODE, version + 1)?.commit()
    }

    override fun hasConfig(): Boolean = (sp?.getInt(CONFIG_VERSION_CODE, 0) ?: 0) > 0

    override fun clear() {
        spEdit?.clear()?.commit()
    }
}
