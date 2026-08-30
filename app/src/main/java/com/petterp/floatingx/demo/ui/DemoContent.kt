package com.petterp.floatingx.demo.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.petterp.floatingx.app.R

/** 各页共用的浮窗内容 */
object DemoContent {

    /** 圆形卡片；用 theme 化的 context 才能解析 Material 属性 */
    fun card(context: Context, text: String): View =
        LayoutInflater.from(context).inflate(R.layout.fx_card, null, false).also {
            it.findViewById<TextView>(R.id.tvTitle).text = text
        }

    fun toast(context: Context, message: String) {
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
