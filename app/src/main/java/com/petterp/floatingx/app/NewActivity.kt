package com.petterp.floatingx.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.impl.simple.FxAnimationImpl

/**
 * @Author petterp
 * @Date 2021/5/21-6:15 下午
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
class NewActivity : AppCompatActivity(R.layout.new_activity), View.OnClickListener {

    private val floatingX by lazy {
        FloatingX.createScopeFx {
            setLayout(R.layout.item_floating)
            setEnableAnimation(true)
            setAnimationListener(FxAnimationImpl())
        }.toControl()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<LinearLayoutCompat>(R.id.ll).apply {
            setOnClickListener {
                floatingX.show(this, true)
            }
        }
        findViewById<View>(R.id.btnShowScopeFx).setOnClickListener(this)
        findViewById<View>(R.id.hideShowScopeFx).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnShowScopeFx -> {
                floatingX.show(this)
            }
            R.id.hideShowScopeFx -> {
                floatingX.hide()
            }
        }
    }
}
