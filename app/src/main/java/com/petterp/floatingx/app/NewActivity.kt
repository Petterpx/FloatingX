package com.petterp.floatingx.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.config.FxHelper
import com.petterp.floatingx.impl.FxControlToScopeImpl
import com.petterp.floatingx.impl.createFloatingX

/**
 * @Author petterp
 * @Date 2021/5/21-6:15 下午
 * @Email ShiyihuiCloud@163.com
 * @Function
 */
class NewActivity : AppCompatActivity(R.layout.new_activity), View.OnClickListener {

    private val floatingX by createFloatingX {
        context(this@NewActivity)
        marginEdge(50f)
        layout(R.layout.item_floating)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(floatingX)
        findViewById<View>(R.id.btnShowScopeFx).setOnClickListener(this)
        findViewById<View>(R.id.hideShowScopeFx).setOnClickListener(this)
    }

    private fun showFx() {
        FxControlToScopeImpl.builder {
            context(this@NewActivity)

        }

        val config = FxHelper.builder().build()
        FxControlToScopeImpl.builder(config)
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
