package com.petterp.floatingx.imp.system

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.listener.provider.IFxPlatformProvider
import com.petterp.floatingx.util.isVisibility
import com.petterp.floatingx.util.permissionControl
import com.petterp.floatingx.util.safeRemovePermissionFragment
import com.petterp.floatingx.util.topActivity
import com.petterp.floatingx.view.FxSystemContainerView

/**
 *
 * @author petterp
 */
class FxSystemPlatformProvider(
    override val helper: FxAppHelper,
    private val lifecycleImp: FxSystemLifecycleImp,
) : IFxPlatformProvider<FxAppHelper>, Application.ActivityLifecycleCallbacks by lifecycleImp {
    private var wm: WindowManager? = null
    private var isRegisterAppLifecycle = false
    private var _internalView: FxSystemContainerView? = null

    init {
        lifecycleImp.provider = this
        checkRegisterAppLifecycle()
    }

    override val context: Context
        get() = helper.context
    override val internalView: FxSystemContainerView?
        get() = _internalView

    override fun show() {
        val internalView = _internalView ?: return
        internalView.registerWM(wm ?: return)
        internalView.isVisibility = true
    }

    override fun hide() {
        val internalView = _internalView ?: return
        // FIXME: 这里本来想直接remove,但是会引发LeakCanary的内存泄漏警告，故才用Gone
        internalView.isVisibility = false
    }

    override fun isShow(): Boolean {
        val internalView = _internalView ?: return false
        return internalView.isAttachToWM && internalView.visibility == View.VISIBLE
    }

    override fun reset() {
        hide()
        wm?.removeViewImmediate(internalView)
        helper.context.unregisterActivityLifecycleCallbacks(this)
        topActivity?.safeRemovePermissionFragment(helper.fxLog)
    }

    override fun checkOrInit(): Boolean {
        // 说明此时其实需要延迟初始化
        val activity = topActivity ?: return false
        if (_internalView == null) {
            if (!checkAgreePermission(activity)) {
                askPermissionAndShow(activity)
                return false
            }
            wm = helper.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            _internalView = FxSystemContainerView(helper, wm!!, context)
            _internalView!!.initView()
            checkRegisterAppLifecycle()
        }
        return true
    }

    private fun checkRegisterAppLifecycle() {
        if (!isRegisterAppLifecycle && helper.enableFx) {
            isRegisterAppLifecycle = true
            helper.context.unregisterActivityLifecycleCallbacks(this)
            helper.context.registerActivityLifecycleCallbacks(this)
        }
    }

    internal fun askPermissionAndShow(activity: Activity) {
        if (isShow()) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkAgreePermission(activity)) {
            FloatingX.control(helper.tag).show()
        } else {
            val permissionControl = activity.permissionControl ?: return
            permissionControl.requestPermission(helper.tag) {
                if (it) {
                    FloatingX.control(helper.tag).show()
                } else {
                    FloatingX.checkReInstall(helper)?.show()
                }
                activity.safeRemovePermissionFragment(helper.fxLog)
            }
        }
    }

    private fun checkAgreePermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(activity)
        }
        return false
    }
}
