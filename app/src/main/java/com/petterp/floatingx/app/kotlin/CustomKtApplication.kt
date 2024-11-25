package com.petterp.floatingx.app.kotlin

import android.app.Application
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.*
import com.petterp.floatingx.app.test.MultipleFxActivity
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.assist.FxScopeType
import com.petterp.floatingx.listener.IFxTouchListener
import com.petterp.floatingx.view.IFxInternalHelper

/** Kotlin-Application */
class CustomKtApplication : Application() {

    override fun onCreate() {
        super.onCreate()

//        FloatingX.install {
//            setLayout(R.layout.item_floating)
//
//            // 如果你全局 [只需要一个浮窗]，这里可以不用传递 tag，默认我们会使用 FX_DEFAULT_TAG 作为未传递TAG时的默认值
//            // 这样的好处是，后续调用控制器(FloatingX.control())时，不用传递 tag。因为相应的方法默认参数里已经携带了该tag
//            // 比如：FloatingX.control()、FloatingX.configControl()
//            // 注意：如果你重复调用install()方法，且未设置tag，那么新的浮窗将会覆盖旧的默认浮窗
//
//            // 注意：这里的tag是用来区分不同的浮窗的，如果你需要多个浮窗，那么请务必设置不同的tag
//            // 注意: 当你调用控制器时，必须传递对应的tag，否则将会抛出异常，除非你使用了 [可null] 的获取方法
//            setTag(MultipleFxActivity.TAG_1)
//        }

        FxSystemSimple.install(this)
        FxAppSimple.install(this)
//        FxComposeSimple.install(this)
    }
}
