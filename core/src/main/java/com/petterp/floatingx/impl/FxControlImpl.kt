package com.petterp.floatingx.impl

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.config.FxHelper
import com.petterp.floatingx.ext.BarExt
import com.petterp.floatingx.ext.FxDebug
import com.petterp.floatingx.ext.hide
import com.petterp.floatingx.ext.rootView
import com.petterp.floatingx.ext.show
import com.petterp.floatingx.listener.FxLifecycleCallback
import com.petterp.floatingx.listener.IFxControl
import com.petterp.floatingx.view.FxMagnetView
import com.petterp.floatingx.view.FxViewHolder
import java.lang.ref.WeakReference

/**
 * @Author petterp
 * @Date 2021/5/21-2:24 下午
 * @Email ShiyihuiCloud@163.com
 * @Function 控制器实现类
 */
open class FxControlImpl(private val helper: FxHelper) : IFxControl {

    private var managerView: FxMagnetView? = null
    private var viewHolder: FxViewHolder? = null
    private var mContainer: WeakReference<FrameLayout>? = null
    internal var fxLifecycleCallback: FxLifecycleCallback? = null
    private val managerViewOrContainerIsNull: Boolean
        get() = mContainer == null && managerView == null

    override fun show() {
        helper.isEnable = true
        managerView ?: showInit()
        managerView?.show()
    }

    override fun show(activity: Activity) {
        helper.isEnable = true
        attach(activity)
        managerView?.show()
    }

    override fun hide() {
        managerView ?: return
        managerView?.hide()
    }

    override fun getView(): View? = managerView

    override fun isShowRunning(): Boolean =
        managerView != null && ViewCompat.isAttachedToWindow(managerView!!) && managerView?.isVisible == true

    override

    fun updateView(obj: (FxViewHolder) -> Unit) {
        viewHolder?.let(obj)
    }

    override fun updateView(@DrawableRes resource: Int) {
        initManagerView(resource)
    }

    override fun updateParams(params: ViewGroup.LayoutParams) {
        managerView?.layoutParams = params
    }

    override fun attach(activity: Activity) {
        activity.rootView?.let {
            attach(it)
        }
    }

    // 安装到fragmentLayout上
    override fun attach(container: FrameLayout) {
        if (managerView?.parent === container) {
            return
        }
        if (managerView == null) {
            initManagerView()
        } else {
            (managerView?.parent as? ViewGroup)?.removeView(managerView)
        }
        FxDebug.d("view-lifecycle-> addView")
        mContainer = WeakReference(container)
        container.addView(managerView)
    }

    /** 删除view */
    override fun detach(activity: Activity) {
        if (managerViewOrContainerIsNull) return
        activity.rootView?.let {
            detach(it)
        }
    }

    override fun detach(container: FrameLayout) {
        if (managerView != null && ViewCompat.isAttachedToWindow(managerView!!)) {
            FxDebug.d("view-lifecycle-> removeView")
            container.removeView(managerView)
        }
        mContainer = null
    }

    override fun setClickListener(obj: (View) -> Unit) {
        helper.clickListener = obj
    }

    override fun dismiss() {
        mContainer?.get()?.let {
            detach(it)
        }
        helper.isEnable = false
        managerView = null
        viewHolder = null
    }

    private fun showInit() {
        if (getContainer() == null) {
            // 这里的异常还是要抛出去
            attach(FloatingX.topActivity)
            return
        }
        if (managerView == null) initManagerView()
        // 判断悬浮窗有没有已添加，如果未添加,而且不在当前decorView中
        if (ViewCompat.isAttachedToWindow(managerView!!) && getContainer() != managerView?.parent) {
            getContainer()?.addView(managerView)
        }
    }

    open fun initManagerView(@DrawableRes layout: Int = 0) {
        if (managerView != null) return
        if (layout != 0) helper.layoutId = layout
        if (helper.layoutId == 0) throw RuntimeException("The layout id cannot be 0")
        managerView = FxMagnetView(helper)
        ViewCompat.setOnApplyWindowInsetsListener(managerView!!, windowsInsetsListener)
        managerView?.requestApplyInsets()
        viewHolder = FxViewHolder(managerView!!)
    }

    @SuppressLint("WrongConstant")
    val windowsInsetsListener: OnApplyWindowInsetsListener =
        OnApplyWindowInsetsListener { _, insets ->
            FxDebug.v("System--StatusBar---old-(${BarExt.realStatusBarHeight}),new-(${insets.systemWindowInsetTop})")
            BarExt.realStatusBarHeight = insets.systemWindowInsetTop
            insets
        }

    private fun getContainer(): FrameLayout? {
        return mContainer?.get()
    }
}
