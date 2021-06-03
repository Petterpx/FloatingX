package com.petterp.floatingx.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.FloatingX

class MainActivity : AppCompatActivity(R.layout.activity_main), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.btnInitFloating).setOnClickListener(this)
        findViewById<View>(R.id.btnShowFloating).setOnClickListener(this)
        findViewById<View>(R.id.btnStartNew).setOnClickListener(this)
        findViewById<View>(R.id.btnHideFloating).setOnClickListener(this)
        findViewById<View>(R.id.btnUpdate).setOnClickListener(this)
        findViewById<View>(R.id.btnCancel).setOnClickListener(this)
        findViewById<View>(R.id.btnDismiss).setOnClickListener(this)
        findViewById<View>(R.id.btnInitClick).setOnClickListener(this)
        findViewById<View>(R.id.btnClick).setOnClickListener(this)
        findViewById<View>(R.id.btnStartFull).setOnClickListener(this)
    }

    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.btnStartFull -> {
                startActivity(Intent(this, ImmersedActivity::class.java))
            }
            R.id.btnStartNew -> {
                startActivity(Intent(this, NewActivity::class.java))
            }
            R.id.btnShowFloating -> {
                FloatingX.show()
            }
            R.id.btnHideFloating -> {
                FloatingX.hide()
            }
            R.id.btnCancel -> {
                FloatingX.cancel()
            }
            R.id.btnDismiss -> {
                // 关闭悬浮窗
                FloatingX.dismiss()
            }
            R.id.btnUpdate -> {
                FloatingX.control().updateView {
                    it.backResource(R.id.ivFloatingX, R.mipmap.ic_launcher)
                }
            }
            R.id.btnInitClick -> {
                FloatingX.control().setClickListener {
                    Toast.makeText(this, "123", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
