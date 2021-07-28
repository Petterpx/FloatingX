package com.petterp.floatingx.assist.helper

import com.petterp.floatingx.impl.control.FxBasisControlImpl
import com.petterp.floatingx.listener.IFxControl

/**
 * @Author petterp
 * @Date 2021/7/28-10:04 PM
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
class BasisHelper : BaseHelper() {

    fun toControl(): IFxControl = FxBasisControlImpl(this)

    class Builder : BaseHelper.Builder<Builder, BasisHelper>() {
        override fun buildHelper(): BasisHelper = BasisHelper()
    }
}
