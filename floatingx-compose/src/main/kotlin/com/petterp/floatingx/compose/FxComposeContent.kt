package com.petterp.floatingx.compose

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.ComposeView
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.config.FxContent

/** 在 compose 内容里拿当前浮窗的 control */
public val LocalFxControl: ProvidableCompositionLocal<FxControl> =
    staticCompositionLocalOf { error("LocalFxControl 只在 FloatingX 的 compose 内容里可用") }

/**
 * Compose 内容（spec §6）：一个 ComposeView + 归 control 所有的 FxComposeOwner。
 * owner 在 create() 时装到 view 上，Compose 向上查找 ViewTree owner 即命中；
 * 组合策略保持默认（卸下 dispose、挂上用当前窗口重新组合），状态靠 ViewModel / rememberSaveable 过桥。
 */
public class FxComposeContent(public val content: @Composable (FxControl) -> Unit) : FxContent() {

    public val owner: FxComposeOwner = FxComposeOwner()

    /**
     * rememberSaveable 的过桥仓库：组合随容器 detach 被 dispose 时把状态存在这里，
     * 重新组合时读回。不能靠 owner 的 SavedStateRegistry——那要有人调
     * performSave/performRestore（宿主 Activity 的活），浮窗没有这个时机。
     * 生命周期跟内容一致：换内容 / cancel 时由 ComposeOwnerFeature 调 release() 清掉。
     */
    private var savedState: Map<String, List<Any?>> = emptyMap()

    override fun create(context: Context, parent: ViewGroup): View =
        ComposeView(context).also { owner.attachTo(it) }

    /** ComposeOwnerFeature 在 attach 时调用：此时才有 control */
    internal fun bind(control: FxControl, view: View) {
        val composeView = view as? ComposeView ?: return
        composeView.setContent {
            val registry = remember { SaveableStateRegistry(savedState) { true } }
            CompositionLocalProvider(
                LocalSaveableStateRegistry provides registry,
                LocalFxControl provides control,
            ) { content(control) }
            // 放在内容之后：dispose 按 remember 的逆序回调，此时内容里的 rememberSaveable 还没注销
            DisposableEffect(registry) {
                onDispose { savedState = registry.performSave() }
            }
        }
    }

    /** 内容被替换或 control.cancel() 时调用，丢掉暂存的组合状态 */
    internal fun release() {
        savedState = emptyMap()
    }
}
