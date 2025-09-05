package com.petterp.floatingx.imp.app

import android.app.Activity
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.listener.provider.IFxPlatformProvider
import com.petterp.floatingx.util.decorView
import com.petterp.floatingx.util.safeAddView
import com.petterp.floatingx.util.safeRemoveView
import com.petterp.floatingx.util.topActivity
import com.petterp.floatingx.view.FxDefaultContainerView
import java.lang.ref.WeakReference

/**
 * 免权限的浮窗提供者
 * @author petterp
 */
class FxAppPlatformProvider(
    override val helper: FxAppHelper,
    override val control: FxAppControlImp,
) : IFxPlatformProvider<FxAppHelper> {

    private var _lifecycleImp: FxAppLifecycleImp? = null
    private var _internalView: FxDefaultContainerView? = null
    private var _containerGroup: WeakReference<ViewGroup>? = null

    private val windowsInsetsListener = OnApplyWindowInsetsListener { _, insets ->
        val statusBar = insets.stableInsetTop
        if (helper.statsBarHeight != statusBar) {
            helper.fxLog.v("System--StatusBar---old-(${helper.statsBarHeight}),new-($statusBar))")
            helper.statsBarHeight = statusBar
        }
        insets
    }

    private val containerGroupView: ViewGroup?
        get() = _containerGroup?.get()

    override val context: Context
        get() = helper.context
    override val internalView: FxDefaultContainerView?
        get() = _internalView

    init {
        // 这里仅仅是为了兼容旧版逻辑
        checkRegisterAppLifecycle()
    }

    override fun checkOrInit(): Boolean {
        checkRegisterAppLifecycle()
        // topActivity==null,依然返回true,因为在某些情况下，可能会在Activity未创建时，就调用show
        val act = topActivity ?: return true
        if (!helper.isCanInstall(act)) {
            helper.fxLog.d("fx not show,This ${act.javaClass.simpleName} is not in the list of allowed inserts!")
            return false
        }
        if (_internalView == null) {
            _internalView = FxDefaultContainerView(helper, helper.context)
            _internalView?.initView()
            checkOrInitSafeArea(act)
            attach(act)
        }
        return true
    }

    override fun show() {
        val fxView = _internalView ?: return
        if (!ViewCompat.isAttachedToWindow(fxView)) {
            fxView.visibility = View.VISIBLE
            checkOrReInitGroupView()?.safeAddView(fxView)
        } else if (fxView.visibility != View.VISIBLE) {
            fxView.visibility = View.VISIBLE
        }
        // Update overlay when showing
        updateBlockOutsideClicks()
    }

    override fun hide() {
        // Remove touch interception when hiding
        removeTouchInterception()
        detach()
    }

    /**
     * Update the outside click blocking state
     */
    fun updateBlockOutsideClicks() {
        if (helper.enableBlockOutsideClicks) {
            setupTouchInterception()
        } else {
            removeTouchInterception()
        }
    }

    private var originalTouchInterceptor: View.OnTouchListener? = null

    private fun setupTouchInterception() {
        val containerView = containerGroupView ?: return
        
        // Save original touch listener if any
        if (originalTouchInterceptor == null) {
            // Set our touch interceptor
            val touchInterceptorTag = "fx_touch_interceptor".hashCode()
            originalTouchInterceptor = containerView.getTag(touchInterceptorTag) as? View.OnTouchListener
            
            val touchInterceptor = View.OnTouchListener { _, event ->
                // Check if touch is inside floating window
                val fxView = _internalView
                if (fxView != null && ViewCompat.isAttachedToWindow(fxView) && fxView.visibility == View.VISIBLE) {
                    val location = IntArray(2)
                    fxView.getLocationOnScreen(location)
                    val fxLeft = location[0]
                    val fxTop = location[1]
                    val fxRight = fxLeft + fxView.width
                    val fxBottom = fxTop + fxView.height
                    
                    val x = event.rawX
                    val y = event.rawY
                    
                    // If touch is inside floating window, let it pass through
                    if (x >= fxLeft && x <= fxRight && y >= fxTop && y <= fxBottom) {
                        return@OnTouchListener false  // Don't consume, let floating window handle
                    }
                    
                    // Touch is outside floating window, consume the event
                    return@OnTouchListener true
                }
                
                // No floating window visible, don't consume
                false
            }
            
            containerView.setTag(touchInterceptorTag, touchInterceptor)
            containerView.setOnTouchListener(touchInterceptor)
        }
        
        helper.fxLog.v("Touch interception enabled")
    }
    
    private fun removeTouchInterception() {
        val containerView = containerGroupView ?: return
        
        // Restore original touch listener
        val touchInterceptorTag = "fx_touch_interceptor".hashCode()
        containerView.setOnTouchListener(originalTouchInterceptor)
        containerView.setTag(touchInterceptorTag, null)
        originalTouchInterceptor = null
        
        helper.fxLog.v("Touch interception disabled")
    }

    private fun checkOrReInitGroupView(): ViewGroup? {
        val curGroup = containerGroupView
        if (curGroup == null || curGroup !== topActivity?.decorView) {
            _containerGroup = WeakReference(topActivity?.decorView)
            helper.fxLog.v("view-----> reinitialize the fx container")
        }
        return containerGroupView
    }

    private fun attach(activity: Activity): Boolean {
        val fxView = _internalView ?: return false
        val decorView = activity.decorView ?: return false
        if (containerGroupView === decorView) return false
        if (ViewCompat.isAttachedToWindow(fxView)) containerGroupView?.safeRemoveView(fxView)
        _containerGroup = WeakReference(decorView)
        decorView.safeAddView(fxView)
        return true
    }

    fun reAttach(activity: Activity): Boolean {
        val nContainer = activity.decorView ?: return false
        if (_internalView == null) {
            _containerGroup = WeakReference(nContainer)
            return true
        } else {
            if (nContainer === containerGroupView) return false
            containerGroupView?.safeRemoveView(_internalView)
            nContainer.safeAddView(_internalView)
            _containerGroup = WeakReference(nContainer)
        }
        return false
    }

    fun destroyToDetach(activity: Activity): Boolean {
        val fxView = _internalView ?: return false
        val oldContainer = containerGroupView ?: return false
        if (!ViewCompat.isAttachedToWindow(fxView)) return false
        val nContainer = activity.decorView ?: return false
        if (nContainer !== oldContainer) return false
        oldContainer.safeRemoveView(_internalView)
        return true
    }

    override fun reset() {
        hide()
        clearWindowsInsetsListener()
        removeTouchInterception()
        _internalView = null
        _containerGroup?.clear()
        _containerGroup = null
        helper.context.unregisterActivityLifecycleCallbacks(_lifecycleImp)
        _lifecycleImp = null
    }

    private fun detach() {
        _internalView?.visibility = View.GONE
        containerGroupView?.safeRemoveView(_internalView)
        removeTouchInterception()
        _containerGroup?.clear()
        _containerGroup = null
    }

    private fun checkRegisterAppLifecycle() {
        if (!helper.enableFx || _lifecycleImp != null) return
        _lifecycleImp = FxAppLifecycleImp(helper, control)
        helper.context.registerActivityLifecycleCallbacks(_lifecycleImp)
    }

    private fun checkOrInitSafeArea(act: Activity) {
        if (!helper.enableSafeArea) return
        helper.updateStatsBar(act)
        helper.updateNavigationBar(act)
        val fxView = _internalView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(fxView, windowsInsetsListener)
        fxView.requestApplyInsets()
    }

    private fun clearWindowsInsetsListener() {
        val managerView = _internalView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(managerView, null)
    }
}
