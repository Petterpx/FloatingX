package com.petterp.floatingx.app

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
            setEnableScrollOutsideScreen(false)
            setAnimationImpl(FxAnimationImpl())
        }.toControl().init(findViewById<ViewGroup>(R.id.ll))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.btnShowScopeFx).setOnClickListener(this)
        findViewById<View>(R.id.hideShowScopeFx).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnShowScopeFx -> {
                floatingX.show()
            }
            R.id.hideShowScopeFx -> {
                floatingX.hide()
            }
        }
    }
}
