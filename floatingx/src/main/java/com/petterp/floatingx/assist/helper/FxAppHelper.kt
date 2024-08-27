package com.petterp.floatingx.assist.helper

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.IdRes
import com.petterp.floatingx.assist.FxScopeType
import com.petterp.floatingx.listener.IFxPermissionInterceptor
import com.petterp.floatingx.listener.IFxProxyTagActivityLifecycle
import com.petterp.floatingx.util.FX_DEFAULT_TAG
import com.petterp.floatingx.util.FX_INSTALL_SCOPE_APP_TAG
import com.petterp.floatingx.util.FX_INSTALL_SCOPE_SYSTEM_TAG
import com.petterp.floatingx.util.navigationBarHeight
import com.petterp.floatingx.util.statusBarHeight

/** FxAppConfig 构建器 */
class FxAppHelper(
    /** 浮窗tag,默认为[FX_DEFAULT_TAG] */
    @JvmSynthetic
    internal val tag: String,
    @JvmSynthetic
    internal val context: Application,
    /** 黑名单list */
    @JvmSynthetic
    internal val blackFilterList: MutableList<String>,
    /** 白名单list */
    @JvmSynthetic
    internal val whiteInsertList: MutableList<String>,
    /** 是否允许插入全部Activity */
    @JvmSynthetic
    internal val isAllInstall: Boolean,
    /** 显示的scope */
    @JvmSynthetic
    internal var scope: FxScopeType,
    @JvmSynthetic
    internal var editTextIds: List<Int>?,
    @JvmSynthetic
    internal var isEnableKeyBoardAdapt: Boolean = false,
    /** 显示悬浮窗的Activity生命周期回调 */
    @JvmSynthetic
    internal val fxLifecycleExpand: IFxProxyTagActivityLifecycle?,

    @JvmSynthetic
    internal val fxAskPermissionInterceptor: IFxPermissionInterceptor?,
) : FxBasisHelper() {
    private val insertCls = mutableMapOf<Class<*>, Boolean>()

    @JvmSynthetic
    internal fun updateNavigationBar(activity: Activity?) {
        navigationBarHeight = activity?.navigationBarHeight ?: navigationBarHeight
        fxLog.v("system-> navigationBar-$navigationBarHeight")
    }

    @JvmSynthetic
    internal fun updateStatsBar(activity: Activity?) {
        statsBarHeight = activity?.statusBarHeight ?: statsBarHeight
        fxLog.v("system-> statusBarHeight-$statsBarHeight")
    }

    @JvmSynthetic
    internal fun isCanInstall(act: Activity): Boolean {
        val cls = act.javaClass
        return insertCls[cls] ?: let {
            val name = cls.name
            val canInstall = (isAllInstall && !blackFilterList.contains(name)) ||
                    (!isAllInstall && whiteInsertList.contains(name))
            insertCls[cls] = canInstall
            canInstall
        }
    }

    class Builder : FxBasisHelper.Builder<Builder, FxAppHelper>() {
        private var enableFx = false
        private var tag = FX_DEFAULT_TAG
        private var context: Application? = null
        private var isEnableAllInstall: Boolean = true
        private var editTextIds: List<Int>? = null
        private var isEnableKeyBoardAdapt: Boolean = false
        private var scopeEnum: FxScopeType = FxScopeType.APP
        private var fxLifecycleExpand: IFxProxyTagActivityLifecycle? = null
        private var askPermissionInterceptor: IFxPermissionInterceptor? = null
        private var whiteInsertList: MutableList<String> = mutableListOf()
        private var blackFilterList: MutableList<String> = mutableListOf()

        /**
         * 设置context
         * @param context context
         * */
        fun setContext(context: Context): Builder {
            if (context is Application) {
                this.context = context
            } else {
                this.context = context.applicationContext as Application
            }
            return this
        }

        /**
         * 设置显示悬浮窗的Activity生命周期回调
         *
         * @param tagActivityLifecycle 生命周期实现类回调
         * @sample
         *     [com.petterp.floatingx.impl.lifecycle.FxTagActivityLifecycleImpl]
         *     空实现,便于直接object:XXX
         */
        fun setTagActivityLifecycle(tagActivityLifecycle: IFxProxyTagActivityLifecycle): Builder {
            this.fxLifecycleExpand = tagActivityLifecycle
            return this
        }

        /**
         * 添加禁止显示悬浮窗的activity
         *
         * @param actNames 禁止显示的activity
         * @sample [xxxActivity::class.java.name]
         *
         * [setEnableAllBlackClass(true)] 时,此方法生效
         */
        fun addInstallBlackClass(vararg actNames: String): Builder {
            blackFilterList.addAll(actNames)
            return this
        }

        fun addInstallBlackClass(vararg cls: Class<out Activity>): Builder {
            val names = cls.map { it.name }
            blackFilterList.addAll(names)
            return this
        }

        fun addInstallBlackClass(cls: List<Class<out Activity>>): Builder {
            val names = cls.map { it.name }
            blackFilterList.addAll(names)
            return this
        }

        /**
         * 启用键盘适配，启用System浮窗将支持键盘弹出与关闭
         * @param isEnable 是否启用
         * @param editTextViewIds 要兼容的输入框ids
         * */
        fun setEnableKeyBoardAdapt(
            isEnable: Boolean,
            @IdRes editTextViewIds: List<Int> = emptyList()
        ): Builder {
            isEnableKeyBoardAdapt = isEnable
            editTextIds = editTextViewIds
            return this
        }

        /**
         * 设置悬浮窗的tag，用于区分不同的悬浮窗
         *
         * 注意：tag 不能为 [""]
         */
        @Throws(IllegalArgumentException::class)
        fun setTag(tag: String): Builder {
            check(tag.isNotEmpty()) {
                "浮窗 tag 不能为 [\"\"],请设置一个合法的tag"
            }
            this.tag = tag
            return this
        }

        /**
         * 设置浮窗安装范围
         * */
        fun setScopeType(scope: FxScopeType): Builder {
            this.scopeEnum = scope
            return this
        }

        /**
         * 允许显示浮窗的activity
         *
         * @param actNames 允许显示的activity路径
         * @sample [xxxActivity::class.java.name]
         *
         * [setEnableAllBlackClass(false)] 时,此方法生效
         */
        fun addInstallWhiteClass(vararg actNames: String): Builder {
            whiteInsertList.addAll(actNames)
            return this
        }

        fun addInstallWhiteClass(vararg cls: Class<out Activity>): Builder {
            val names = cls.map { it.name }
            whiteInsertList.addAll(names)
            return this
        }

        fun addInstallWhiteClass(cls: List<Class<out Activity>>): Builder {
            val names = cls.map { it.name }
            whiteInsertList.addAll(names)
            return this
        }

        /**
         * 是否允许给所有浮窗安装悬浮窗
         *
         * @param isEnable 是否允许,默认true
         */
        fun setEnableAllInstall(isEnable: Boolean): Builder {
            isEnableAllInstall = isEnable
            return this
        }

        fun setPermissionInterceptor(listener: IFxPermissionInterceptor): Builder {
            askPermissionInterceptor = listener
            return this
        }

        override fun buildHelper(): FxAppHelper {
            checkNotNull(context) { "context == null, please call AppHelper.setContext(context) to set context" }
            return FxAppHelper(
                tag,
                context!!,
                blackFilterList,
                whiteInsertList,
                isEnableAllInstall,
                scopeEnum,
                editTextIds,
                isEnableKeyBoardAdapt,
                fxLifecycleExpand,
                askPermissionInterceptor,
            )
        }

        override fun build(): FxAppHelper {
            return super.build().apply {
                enableFx = this@Builder.enableFx
                // 有可能用户会使用多个浮窗，这里为了防止日志混乱，将浮窗tag赋值给日志tag
                if (enableDebugLog && fxLogTag.isEmpty()) {
                    fxLogTag = tag
                }
                if (scopeEnum == FxScopeType.SYSTEM) {
                    initLog(FX_INSTALL_SCOPE_SYSTEM_TAG)
                } else {
                    initLog(FX_INSTALL_SCOPE_APP_TAG)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}
