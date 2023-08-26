package com.petterp.floatingx.app.test

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.petterp.floatingx.app.addItemView
import com.petterp.floatingx.app.addLinearLayout
import com.petterp.floatingx.app.addNestedScrollView
import com.petterp.floatingx.app.createLinearLayoutToParent
import com.petterp.floatingx.util.createFx

/**
 *
 * @author petterp
 */
class SimpleRvActivity : AppCompatActivity() {

    private val adapter by lazy {
        CustomAdapter(3)
    }

    private val activityFx by createFx {
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
        setLayoutView(recyclerView)
        build().toControl(this@SimpleRvActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            addNestedScrollView {
                addLinearLayout {
                    addItemView("显示浮窗") {
                        activityFx.show()
                    }
                    addItemView("隐藏浮窗") {
                        activityFx.hide()
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
}