package com.petterp.floatingx.imp

import android.widget.FrameLayout
import com.petterp.floatingx.assist.helper.FxBasisHelper
import com.petterp.floatingx.listener.provider.IFxAnimationProvider

/**
 *
 * @author petterp
 */
class FxBasicAnimationProvider<F : FxBasisHelper>(override val helper: F) : IFxAnimationProvider {

    override fun start(view: FrameLayout, obj: (() -> Unit)?) {
        val fxAnimation = helper.fxAnimation ?: return
        if (fxAnimation.fromJobIsRunning()) {
            helper.fxLog.d("fxView -> Animation,startAnimation Executing, cancel this operation!")
            return
        }
        helper.fxLog.d("fxView -> Animation,startAnimation Running.")
        fxAnimation.setFromAnimatorListener(obj)
        fxAnimation.fromStartAnimator(view)
    }

    override fun hide(view: FrameLayout, obj: (() -> Unit)?) {
        val fxAnimation = helper.fxAnimation ?: return
        if (helper.fxAnimation!!.endJobIsRunning()) {
            helper.fxLog.d("fxView -> Animation,endAnimation Executing, cancel this operation!")
            return
        }
        helper.fxLog.d("fxView -> Animation,endAnimation Running.")
        fxAnimation.setEndAnimatorListener(obj)
        fxAnimation.toEndAnimator(view)
    }

    override fun canRunAnimation(): Boolean {
        return helper.enableAnimation && helper.fxAnimation != null
    }

    override fun canCancelAnimation(): Boolean {
        return helper.enableAnimation && helper.fxAnimation != null
    }

    override fun reset() {
        helper.fxAnimation?.cancelAnimation()
    }
}
