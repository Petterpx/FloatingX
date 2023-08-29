package com.petterp.floatingx.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.app.test.BlackActivity
import com.petterp.floatingx.app.test.ImmersedActivity
import com.petterp.floatingx.app.test.MultipleFxActivity
import com.petterp.floatingx.app.test.ScopeActivity
import com.petterp.floatingx.app.test.SimpleRvActivity

/**
 * 测试合集
 * @author petterp
 */
class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            addNestedScrollView {
                addLinearLayout {
                    addItemView("进入多浮窗页面(测试多浮窗功能)") {
                        MultipleFxActivity::class.java.start(context)
                    }
                    addItemView("进入黑名单页面(该页面禁止展示浮窗1)") {
                        BlackActivity::class.java.start(context)
                    }
                    addItemView("进入无状态栏页面-(测试状态栏影响)") {
                        ImmersedActivity::class.java.start(context)
                    }
                    addItemView("进入局部悬浮窗页面-(测试api功能)") {
                        ScopeActivity::class.java.start(context)
                    }
                    addItemView("进入recyclerView测试页面") {
                        SimpleRvActivity::class.java.start(context)
                    }
//                    addItemView("跳转到测试页面-(测试申请权限的浮窗)") {
//                        SingleActivity::class.java.start(context)
//                    }
                }
            }
        }
    }
}