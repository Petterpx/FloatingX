package com.petterp.floatingx.assist.helper

/**
 * FxSystemConfig 构建器
 * @author petterp
 */
class FxSystemHelper : FxBasisHelper() {

    class Builder : FxBasisHelper.Builder<Builder, FxSystemHelper>() {
        override fun buildHelper(): FxSystemHelper = FxSystemHelper()

        override fun build(): FxSystemHelper {
            return super.build()
        }
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}
