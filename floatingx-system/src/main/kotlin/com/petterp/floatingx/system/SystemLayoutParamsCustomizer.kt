package com.petterp.floatingx.system

import android.view.WindowManager

/** 在默认 LayoutParams 之后执行，可覆盖任何字段（type / flags / softInputMode…，#194/#220/#241/#235/#155/#211） */
public fun interface SystemLayoutParamsCustomizer {
    public fun customize(lp: WindowManager.LayoutParams)
}
