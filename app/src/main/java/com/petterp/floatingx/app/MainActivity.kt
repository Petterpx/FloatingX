package com.petterp.floatingx.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.simple.FxAnimationImpl
import com.petterp.floatingx.app.test.FloatingCompose
import com.petterp.floatingx.app.test.MultipleFxActivity
import com.petterp.floatingx.app.test.SystemActivity
import com.petterp.floatingx.util.createFx

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            addNestedScrollView {
                addLinearLayout {
                    addItemView("展示浮窗") {
                        FloatingCompose.install(this@MainActivity.application)
                    }
                }
            }
        }
    }

}