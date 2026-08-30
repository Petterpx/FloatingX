package com.petterp.floatingx.demo.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.demo.ui.demoPage

/** Compose 浮窗的换页观察页：换页只是把容器挪到本页，组合状态不重来 */
class ComposeSecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("Compose 第二页") {
            note(
                "浮窗跟到本页，count 与 vm 都应保持原值：\n" +
                    "- 容器只是换了父 view，FxComposeOwner 归 control 所有，不随页面销毁（#210）；\n" +
                    "- 组合若被 dispose 过，rememberSaveable 的值由 FxComposeContent 的过桥仓库带回来。",
            )
            note("在本页点浮窗继续计数，返回上一页数字应接着走。")
            button("返回") { finish() }
        }
    }
}
