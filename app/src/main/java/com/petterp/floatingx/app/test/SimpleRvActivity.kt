package com.petterp.floatingx.app.test

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.addItemView
import com.petterp.floatingx.app.addLinearLayout
import com.petterp.floatingx.app.addNestedScrollView
import com.petterp.floatingx.app.createLinearLayoutToParent

/**
 *
 * @author petterp
 */
class SimpleRvActivity : AppCompatActivity() {

    private val customAdapter by lazy(LazyThreadSafetyMode.NONE) {
        CustomAdapter(3)
    }

    private val rv by lazy(LazyThreadSafetyMode.NONE) {
        RecyclerView(this@SimpleRvActivity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.GREEN)
            adapter = customAdapter
            layoutManager = LinearLayoutManager(this@SimpleRvActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            addNestedScrollView {
                addLinearLayout {
                    addItemView("显示浮窗") {
                        if (FloatingX.isInstalled(TAG)) return@addItemView
                        FloatingX.install {
                            setContext(this@SimpleRvActivity)
                            setLayoutView(rv)
                            setTag(TAG)
                            enableFx()
                        }.show(this@SimpleRvActivity)
                    }
                    addItemView("隐藏浮窗") {
                        FloatingX.control(TAG).detach(this@SimpleRvActivity)
                    }
                    addItemView("增加rv数据") {
                        customAdapter.sum += 10
                        customAdapter.notifyDataSetChanged()
                    }
                    addItemView("减少rv数据") {
                        customAdapter.sum -= 5
                        if (customAdapter.sum < 0) customAdapter.sum = 1
                        customAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "SimpleRvActivity"
    }
}