package com.petterp.floatingx.compose

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.config.FxContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** 在 compose 内容里拿当前浮窗的 control */
public val LocalFxControl: ProvidableCompositionLocal<FxControl> =
    staticCompositionLocalOf { error("LocalFxControl 只在 FloatingX 的 compose 内容里可用") }

/**
 * Compose 内容（spec §6）：一个 ComposeView + 归 control 所有的 FxComposeOwner。
 * owner 在 create() 时装到 view 上，Compose 向上查找 ViewTree owner 即命中。
 *
 * **每个实例持有自己的 [Recomposer]**，并把它设成 ComposeView 的 parentCompositionContext：
 * `ComposeView.resolveParentCompositionContext()` 优先返回 parentContext，所以组合完全不依赖
 * 宿主窗口的 Recomposer——浮窗被静默挪到另一个窗口（AppHost 换页）时不会去新根 view 上找
 * ViewTreeLifecycleOwner，裸 `android.app.Activity` 宿主也能用。
 * 组合策略保持默认（卸下 dispose、挂上重新组合），状态靠 ViewModel / rememberSaveable 过桥。
 *
 * 状态存活范围：ViewModel 与 rememberSaveable 能跨容器 detach/re-attach、跨页面、跨 host 存活，
 * 但**只在进程内**——进程被杀后不恢复（浮窗没有宿主 Activity 的 onSaveInstanceState 时机，
 * 见下面 savedState 的说明）。需要跨进程存活的数据请自己落盘。
 *
 * 一个实例只服务一个 control：control.cancel() 会把 owner destroy 掉，之后不能再复用。
 * 构造与 [create] / [bind] / [release] 都必须在主线程（Recomposer 绑的是主线程的 Choreographer 帧时钟）。
 */
public class FxComposeContent(public val content: @Composable (FxControl) -> Unit) : FxContent() {

    public val owner: FxComposeOwner = FxComposeOwner()

    /**
     * rememberSaveable 的过桥仓库：组合随容器 detach 被 dispose 时把状态存在这里，
     * 重新组合时读回。不能靠 owner 的 SavedStateRegistry——那要有人调
     * performSave/performRestore（宿主 Activity 的活），浮窗没有这个时机。
     *
     * 不做 `canBeSaved` 过滤（恒 true）：这里只是内存里的 map，不走 Bundle 也不跨进程，
     * 任何值都收；生命周期跟内容一致——换内容 / cancel 时由 ComposeOwnerFeature 调 [release] 清掉。
     */
    private var savedState: Map<String, List<Any?>> = emptyMap()

    /** 自己的组合驱动，见类注释；[release] 时连同 [recomposeScope] 一起取消 */
    private var recomposer: Recomposer? = null
    private var recomposeScope: CoroutineScope? = null

    /** 已经 setContent 过的 control + view，重复 bind（每次 re-attach 都会来）不再白跑一次组合 */
    private var boundControl: FxControl? = null
    private var boundView: View? = null

    override fun create(context: Context, parent: ViewGroup): View {
        checkAlive()
        // 换了 view，旧的绑定作废
        boundControl = null
        boundView = null
        return ComposeView(context).also {
            owner.attachTo(it)
            it.setParentCompositionContext(startRecomposer())
        }
    }

    /** ComposeOwnerFeature 在 attach / 换内容时调用：此时才有 control */
    internal fun bind(control: FxControl, view: View) {
        checkAlive()
        val composeView = view as? ComposeView ?: return
        if (boundControl === control && boundView === composeView) return
        boundControl = control
        boundView = composeView
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

    /** 内容被替换或 control.cancel() 时调用：丢掉暂存的组合状态，停掉自己的 Recomposer */
    internal fun release() {
        savedState = emptyMap()
        boundControl = null
        boundView = null
        recomposer?.cancel()
        recomposer = null
        recomposeScope?.cancel()
        recomposeScope = null
    }

    /** 幂等：内容 view 只会建一次，但 create() 被重复调用也不能起第二个 Recomposer */
    private fun startRecomposer(): Recomposer {
        recomposer?.let { return it }
        // CurrentThread 必须在主线程取：它带的是这条线程的 Choreographer 帧时钟
        val dispatcher = AndroidUiDispatcher.CurrentThread
        val r = Recomposer(dispatcher)
        val scope = CoroutineScope(dispatcher)
        recomposer = r
        recomposeScope = scope
        scope.launch { r.runRecomposeAndApplyChanges() }
        return r
    }

    private fun checkAlive() {
        check(!owner.isDestroyed) { "FxComposeContent 已随 control.cancel() 销毁，不能复用；请新建一个" }
    }
}
