package com.petterp.floatingx.app.test

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.R
import com.petterp.floatingx.app.addItemView
import com.petterp.floatingx.app.addLinearLayout
import com.petterp.floatingx.app.addNestedScrollView
import com.petterp.floatingx.app.addTextView
import com.petterp.floatingx.app.createLinearLayoutToParent
import com.petterp.floatingx.assist.FxDisplayMode
import com.petterp.floatingx.listener.IFxTouchListener
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.view.IFxInternalHelper

/**
 *
 * @author petterp
 */
class SimpleRvActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            addNestedScrollView {
                addLinearLayout {
                    addItemView("显示浮窗") {
                        if (!FloatingX.isInstalled(TAG)) {
                            FloatingX.install {
                                setTag(TAG)
                                setContext(applicationContext)
                                setLayoutView(createRvView(applicationContext))
                                setEnableLog(true)
                                setTouchListener(object : IFxTouchListener {
                                    override fun onInterceptTouchEvent(
                                        event: MotionEvent,
                                        control: IFxInternalHelper?
                                    ): Boolean {
                                        val isHeader =
                                            control?.checkPointerDownTouch(R.id.text, event)
                                        return isHeader ?: true
                                    }
                                })
                                this.setOnClickListener {
                                    Toast.makeText(it.context, "123", Toast.LENGTH_SHORT).show()
                                }
                            }.show()
                        }
                        FloatingX.controlOrNull(TAG)?.show()
                    }
                    addItemView("隐藏浮窗") {
                        FloatingX.controlOrNull(TAG)?.hide()
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

        private val customAdapter by lazy(LazyThreadSafetyMode.NONE) {
            CustomAdapter(3)
        }

        // TODO: 注意全局浮窗使用时的注意事项，需要使用application级别
        fun createRvView(context: Context) =
            LinearLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    500,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                orientation = LinearLayout.VERTICAL
                addTextView {
                    id = R.id.text
                    text = "我是Header"
                    layoutParams = ViewGroup.LayoutParams(
                        500,
                        200,
                    )
                }
                val rv = RecyclerView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        800,
                    )
                    setBackgroundColor(Color.GREEN)
                    adapter = customAdapter
                    layoutManager = LinearLayoutManager(context)
                }
                addView(rv)
            }
    }
}

class CustomAdapter(var sum: Int) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(-2, -2)
        }
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return sum
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.itemView as TextView).text = "123123123123123123"
    }
}
