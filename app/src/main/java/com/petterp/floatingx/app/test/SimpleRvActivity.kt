package com.petterp.floatingx.app.test

import android.content.Intent
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

    private val adapter by lazy {
        CustomAdapter(3)
    }

    private val rv by lazy {
        val recyclerView = RecyclerView(this@SimpleRvActivity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        recyclerView.setBackgroundColor(Color.GREEN)
        recyclerView.adapter = adapter
        recyclerView.layoutManager =
            LinearLayoutManager(this@SimpleRvActivity, LinearLayoutManager.VERTICAL, false)
        recyclerView
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
                        adapter.sum += 10
                        adapter.notifyDataSetChanged()
                    }
                    addItemView("减少rv数据") {
                        adapter.sum -= 5
                        if (adapter.sum < 0) adapter.sum = 1
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "SimpleRvActivity"
    }
}