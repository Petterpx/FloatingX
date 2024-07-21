package com.petterp.floatingx.app.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.addItemView
import com.petterp.floatingx.app.addLinearLayout
import com.petterp.floatingx.app.addNestedScrollView
import com.petterp.floatingx.app.createLinearLayoutToParent
import com.petterp.floatingx.app.kotlin.CustomKtApplication
import com.petterp.floatingx.app.kotlin.FxAppSimple
import com.petterp.floatingx.app.kotlin.FxSystemSimple

/**
 * 多浮窗示例
 *
 * @author petterp
 */
class MultipleFxActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLinearLayoutToParent {
            addNestedScrollView {
                addLinearLayout {
                    addItemView("显示全局悬浮窗(tag1)") {
                        if (!FloatingX.isInstalled(TAG_1)) {
                            FxSystemSimple.install(application)
                        }
                        FloatingX.control(TAG_1).show()
                    }
                    addItemView("显示全局悬浮窗(tag2)") {
                        if (!FloatingX.isInstalled(TAG_2)) {
                            FxAppSimple.install(application)
                        }
                        FloatingX.control(TAG_2).show()
                    }
                    addItemView("重复安装全局悬浮窗(tag1)") {
                        FxSystemSimple.install(application)
                        FloatingX.control(TAG_1).show()
                    }
                    addItemView("隐藏全局悬浮窗(tag1)") {
                        FloatingX.controlOrNull(TAG_1)?.hide()
                    }
                    addItemView("隐藏全局悬浮窗(tag2)") {
                        FloatingX.controlOrNull(TAG_2)?.hide()
                    }
                    addItemView("关闭全局悬浮窗(tag1)") {
                        FloatingX.controlOrNull(TAG_1)?.cancel()
                    }
                    addItemView("关闭全局悬浮窗(tag2)") {
                        FloatingX.controlOrNull(TAG_2)?.cancel()
                    }
                    addItemView("卸载所有全局浮窗") {
                        FloatingX.uninstallAll()
                    }
                }
            }
        }
    }

    companion object {
        const val TAG_1 = "tag1"
        const val TAG_2 = "tag2"
    }
}
