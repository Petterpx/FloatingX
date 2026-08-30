package com.petterp.floatingx.demo

import android.app.Application

/** 3.0 不再需要在 Application 里初始化任何东西：floatingx-app 的 ContentProvider 会自动注册 Activity 跟踪 */
class DemoApp : Application()
