package com.petterp.floatingx.imp.system

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import com.petterp.floatingx.assist.FxScopeType
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.listener.IFxPermissionAskControl
import com.petterp.floatingx.listener.provider.IFxPlatformProvider
import com.petterp.floatingx.util.FxPermissionResultAction
import com.petterp.floatingx.util.isVisibility
import com.petterp.floatingx.util.permissionControl
import com.petterp.floatingx.util.safeRemovePermissionFragment
import com.petterp.floatingx.util.topActivity
import com.petterp.floatingx.view.FxSystemContainerView

/**
 * 系统浮窗提供平台
 * @author petterp
 */
class FxSystemPlatformProvider(
    override val helper: FxAppHelper,
    override val control: FxSystemControlImp,
) : IFxPlatformProvider<FxAppHelper>, IFxPermissionAskControl {
    private var wm: WindowManager? = null
    private var _lifecycleImp: FxSystemLifecycleImp? = null
    private var _internalView: FxSystemContainerView? = null
    private var requestRunnable: FxPermissionResultAction? = null

    override val context: Context
        get() = helper.context
    override val internalView: FxSystemContainerView?
        get() = _internalView

    init {
        checkRegisterAppLifecycle()
    }

    override fun show() {
        val internalView = _internalView ?: return
        internalView.registerWM(wm ?: return)
        internalView.isVisibility = true
    }

    override fun hide() {
        val internalView = _internalView ?: return
        // 这里本来想直接remove,但是会引发LeakCanary的内存泄漏警告，故才用Gone
        internalView.isVisibility = false
    }

    override fun isShow(): Boolean {
        val internalView = _internalView ?: return false
        return internalView.isAttachToWM && internalView.visibility == View.VISIBLE
    }

    override fun checkOrInit(): Boolean {
        checkRegisterAppLifecycle()
        // topActivity==null,依然返回true,因为在某些情况下，可能会在Activity未创建时，就调用show
        val act = topActivity ?: return true
        // 禁止安装浮窗时，直接返回false
        if (!helper.isCanInstall(act)) {
            helper.fxLog.d("fx not show,This [${act.javaClass.simpleName}] is not in the list of allowed inserts!")
            return false
        }
        if (_internalView == null) {
            if (!checkAgreePermission(act)) {
                internalAskAutoPermission(act)
                return false
            }
            wm = helper.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            _internalView = FxSystemContainerView(helper, wm!!, context)
            _internalView!!.initView()
        }
        return true
    }

    override fun requestPermission(
        activity: Activity,
        isAutoShow: Boolean,
        canUseAppScope: Boolean,
        resultListener: FxPermissionResultAction?
    ) {
        if (isShow()) {
            resultListener?.invoke(true)
            return
        }
        helper.fxLog.d("tag:[${helper.tag}] requestPermission start---->")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkAgreePermission(activity)) {
            if (isAutoShow) control.show()
            resultListener?.invoke(true)
        } else {
            val permissionControl = activity.permissionControl ?: return
            requestRunnable = {
                helper.fxLog.d("tag:[${helper.tag}] requestPermission end,result:$[$it]---->")
                if (it && isAutoShow) {
                    control.show()
                } else if (!it && canUseAppScope) {
                    downgradeToAppScope()
                }
                topActivity?.safeRemovePermissionFragment(helper.fxLog)
                resultListener?.invoke(it)
            }
            permissionControl.requestPermission(helper.tag, requestRunnable)
        }
    }

    override fun releaseConfig(isRelease: Boolean) {
        control.cancel()
    }

    override fun downgradeToAppScope() {
        control.checkReInstallShow()
    }

    override fun reset() {
        hide()
        wm?.removeViewImmediate(internalView)
        topActivity?.safeRemovePermissionFragment(helper.fxLog)
        helper.context.unregisterActivityLifecycleCallbacks(_lifecycleImp)
        requestRunnable = null
        _lifecycleImp = null
    }

    internal fun safeShowOrHide(visible: Boolean) {
        if (visible) {
            if (isShow()) return
            show()
        } else {
            hide()
        }
    }

    private fun checkRegisterAppLifecycle() {
        if (!helper.enableFx || _lifecycleImp != null) return
        _lifecycleImp = FxSystemLifecycleImp(helper, this)
        helper.context.registerActivityLifecycleCallbacks(_lifecycleImp)
    }

    internal fun internalAskAutoPermission(activity: Activity) {
        // 有权限,则跳过
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkAgreePermission(activity)) {
            control.show()
            return
        }
        // 如果用户自己拦截了事件，则将控制权还给用户
        if (helper.fxAskPermissionInterceptor != null) {
            helper.fxAskPermissionInterceptor.invoke(activity, this)
            return
        }
        requestPermission(activity, true, helper.scope == FxScopeType.SYSTEM_AUTO)
    }

    private fun checkAgreePermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(activity)
        }
        return true
    }
}
