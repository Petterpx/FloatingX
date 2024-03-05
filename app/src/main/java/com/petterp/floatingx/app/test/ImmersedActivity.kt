package com.petterp.floatingx.app.test

import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity

/** 全屏Activity */
class ImmersedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE) // 这行代码一定要在setContentView之前，不然会闪退
//        createLinearLayoutToParent {
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//            window.decorView.systemUiVisibility = (
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE // 防止状态栏、底部导航栏隐藏时，内容区域大小发生变化
//                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                ) // Activity会全屏显示，但状态栏不会被隐藏，状态栏依然可见，Activity 顶端布局部分会被状态栏盖住
//            window.statusBarColor = Color.TRANSPARENT
//        }
    }
}
