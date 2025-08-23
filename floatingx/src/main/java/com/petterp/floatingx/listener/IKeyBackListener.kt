package com.petterp.floatingx.listener

/**
 * 系统拦截返回监听
 */
interface IKeyBackListener {
    fun onBackPressed(): Boolean
}