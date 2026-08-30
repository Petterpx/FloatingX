package com.petterp.floatingx.demo.regression

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.demo.R
import com.petterp.floatingx.demo.pages.ScopeFragment
import com.petterp.floatingx.demo.pages.fragmentSlot
import com.petterp.floatingx.demo.ui.demoPage

/**
 * #244 回归：在 Fragment.onCreate（view 还没创建）里创建局部浮窗，浮窗不显示。
 * 3.0 的 FragmentHost 观察 viewLifecycleOwnerLiveData，view 就绪后自动挂载。
 */
class Issue244Activity : AppCompatActivity() {

    private val slot by lazy { fragmentSlot(this, 240) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("#244 Fragment 内浮窗") {
            note(
                "复现步骤：\n" +
                    "1. 点下面的按钮添加 Fragment；\n" +
                    "2. ScopeFragment 在 onCreate 里调用 fxScope(\"scope-frag\").show()，此时它还没有根 view；\n" +
                    "3. 期望：Fragment 的 view 一创建，「Frag」浮窗就出现在下面的灰色区域里，并且只能在区域内拖动。",
            )
            note("再点一次按钮会 replace 掉旧 Fragment：旧 fragment destroy → 浮窗自动 cancel，新 fragment 重新建一个。")
            button("添加 Fragment（onCreate 里建浮窗）") {
                supportFragmentManager.beginTransaction().replace(R.id.fragmentSlot, ScopeFragment()).commit()
            }
            custom(slot)
        }
    }
}
