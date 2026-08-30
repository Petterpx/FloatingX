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
        demoPage(R.string.page_issue244_title) {
            note(R.string.note_issue244_steps)
            note(R.string.note_issue244_replace)
            button(R.string.btn_fragment_add_oncreate) {
                supportFragmentManager.beginTransaction().replace(R.id.fragmentSlot, ScopeFragment()).commit()
            }
            custom(slot)
        }
    }
}
