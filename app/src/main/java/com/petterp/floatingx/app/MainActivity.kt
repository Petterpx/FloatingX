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
                FloatingX.control().show(this)
            }
            R.id.btnHideFloating -> {
                FloatingX.control().hide()
            }
            R.id.btnCancel -> {
                FloatingX.control().cancel()
            }
            R.id.btnUpdate -> {
                FloatingX.control().updateView {
                    it.backResource(R.id.ivFloatingX, R.mipmap.ic_launcher)
                }
            }
            R.id.btnInitClick -> {
                FloatingX.control().setClickListener(2000L) {
                    Toast.makeText(this, "123", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
