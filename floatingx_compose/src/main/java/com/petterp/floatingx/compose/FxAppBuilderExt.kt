package com.petterp.floatingx.compose

import android.view.View
import android.widget.FrameLayout.VISIBLE
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.listener.IFxViewLifecycle

/**
 * 为system浮窗启用compose支持
 * @author petterp
 */
fun FxAppHelper.Builder.enableComposeSupport() = apply {
    addViewLifecycle(FxComposeViewLifecycleImp())
}

internal class FxComposeViewLifecycleImp : IFxViewLifecycle {
    private var lifecycleOwner: FxComposeLifecycleOwner? = null

    override fun attach(view: View) {
        // 如果存在viewLifecycle,没有必要开启这些
        if (lifecycleOwner != null || view.findViewTreeLifecycleOwner() != null) return
        lifecycleOwner = FxComposeLifecycleOwner()
        lifecycleOwner?.attachToDecorView(view)
        lifecycleOwner?.onCreate()
        lifecycleOwner?.onStart()
    }

    override fun detached(view: View) {
        lifecycleOwner?.onStop()
        lifecycleOwner?.onDestroy()
        lifecycleOwner?.detachFromDecorView(view)
        lifecycleOwner = null
    }

    override fun windowsVisibility(visibility: Int) {
        if (visibility == VISIBLE) {
            lifecycleOwner?.onResume()
        } else {
            lifecycleOwner?.onPause()
        }
    }
}