# FloatingX 3.0 Plan 4：floatingx-compose 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地 `floatingx-compose`：`FxComposeContent`（`ComposeView` 内容 + 归 control 所有的 `FxComposeOwner`：Lifecycle / ViewModelStore / SavedStateRegistry）、`ComposeOwnerFeature`（把 attach/show/hide/detach/cancel 映射到 owner 生命周期，只在 cancel 时 destroy——修 #210/#239）、`LocalFxControl`、`FxConfigScope.compose {}` DSL、`FxControl.stateFlow()/positionFlow()`。

**Architecture:** Compose 的窗口级 Recomposer 由"当前窗口根 view 的 ViewTreeLifecycleOwner"驱动，且 view 从窗口卸下时该 Recomposer 会被取消；所以浮窗每次挂到新窗口都要用当前窗口的 Recomposer 重新组合（保持 `ComposeView` 默认的 `DisposeOnDetachedFromWindowOrReleasedFromPool` 策略），而**状态**由我们自己的 owner 保住：`ViewModel` 在 owner 的 `ViewModelStore` 里、`rememberSaveable` 走 owner 的 `SavedStateRegistry`，二者都只在 `cancel()` 时销毁。owner 设在内容 view 上（Compose 向上查找即命中）并在 attach 时也设到容器根 view 上（系统窗口的根就是容器，窗口级 Recomposer 需要它）。flow 通过 `FxListener` 桥接，无 core 改动。

**Tech Stack:** Kotlin 2.2.21 + compose 编译器插件（`org.jetbrains.kotlin.plugin.compose`，版本随 Kotlin）、compose-ui 1.11.4、lifecycle-runtime/viewmodel 2.11.0、savedstate 1.5.0、kotlinx-coroutines-android 1.11.0、minSdk 23、Robolectric 4.16.1（sdk=35）。

**Spec:** `docs/superpowers/specs/2026-08-29-floatingx-3-modular-architecture-design.md` §6（§1.1 版本矩阵；§6 两处按本计划裁决修订，见 Task 5）。

## Global Constraints

- 模块 `floatingx-compose`，包 `com.petterp.floatingx.compose`，`namespace` 同名；`minSdk = 23`（compose 1.11 / lifecycle 2.11 的 AAR 要求；在模块 build 文件里覆盖 convention plugin 的 21）；`buildFeatures.compose = true` + compose 编译器插件。
- 依赖（spec §1）：`api(project(":floatingx-core"))`、`api(libs.compose.ui.versioned)`（公开 API 含 `@Composable` 与 `CompositionLocal`）、`implementation(libs.lifecycle.runtime)`、`implementation(libs.lifecycle.viewmodel)`、`implementation(libs.savedstate)`、`implementation(libs.coroutines.android)`、`implementation(libs.kotlin.stdlib)`；测试另加 `testImplementation("androidx.compose.foundation:foundation:1.11.4")`。`src/main` 禁止 import `androidx.fragment`、`androidx.appcompat`、`android.view.WindowManager`（Task 1 边界测试）。只有本模块依赖 coroutines；core 无 Flow。
- 只用 core 公开 API（`FxContent`、`FxFeature`/`FxFeatureScope`、`FxListener`、`FxControl`、`FxConfigScope`）。`explicitApi()` 开启。
- owner 归 control：容器 detach 只降到 CREATED（onPause/onStop），**只在** `FxFeature.onCancel()` 时 DESTROYED + `ViewModelStore.clear()`（spec §6）。
- 性能（spec §8）：flow 更新在监听器回调里直接 `value = …`，无额外 post；`positionFlow` 每次 onDrag 赋值一个 `FxPoint`（core 的 `control.position` 本就返回新对象，无额外分配）。
- 注释/KDoc/提交信息中文；Conventional Commits；每个 Task 结束 `./gradlew :floatingx-compose:testDebugUnitTest` 全绿；最后一个 Task 跑 `./gradlew test`。
- Robolectric sdk=35；Compose 在 Robolectric 下组合需要：view 挂在真实 Activity 的窗口里 + `shadowOf(Looper.getMainLooper()).idle()`。

---

## 文件结构

```
settings.gradle                                    include ':floatingx-compose'
floatingx-compose/
  build.gradle.kts / .gitignore / consumer-rules.pro
  src/test/resources/robolectric.properties
  src/main/kotlin/com/petterp/floatingx/compose/
    FxComposeOwner.kt                              Lifecycle + ViewModelStore + SavedStateRegistry owner
    FxComposeContent.kt                            FxContent 子类：ComposeView + owner；LocalFxControl
    ComposeOwnerFeature.kt                         生命周期映射 feature
    FxComposeExt.kt                                FxConfigScope.compose {} DSL
    FxComposeFlows.kt                              stateFlow() / positionFlow()
  src/test/kotlin/com/petterp/floatingx/compose/
    DependencyBoundaryTest.kt
    FxComposeOwnerTest.kt
    FxComposeContentTest.kt
    ComposeOwnerFeatureTest.kt
    FxComposeFlowsTest.kt
    TestHost.kt                                    挂在 FrameLayout 上的最小 host（与 core 测试同款）
docs/superpowers/specs/…design.md                  §6 修订
CLAUDE.md                                          模块表
```

---

### Task 1: 模块骨架（compose 插件、minSdk 23、依赖边界）

**Files:**
- Modify: `settings.gradle`（追加 `include ':floatingx-compose'`）
- Create: `floatingx-compose/build.gradle.kts`、`.gitignore`、`consumer-rules.pro`、`src/test/resources/robolectric.properties`
- Create: `floatingx-compose/src/main/kotlin/com/petterp/floatingx/compose/FxComposeOwner.kt`（本 Task 只放最小骨架，Task 2 补全）
- Test: `floatingx-compose/src/test/kotlin/com/petterp/floatingx/compose/DependencyBoundaryTest.kt`

- [ ] **Step 1: settings.gradle**

```groovy
include ':floatingx-compose'
```

- [ ] **Step 2: 构建文件**

`floatingx-compose/build.gradle.kts`：

```kotlin
plugins {
    id("floatingx.library")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.petterp.floatingx.compose"
    // compose 1.11 / lifecycle 2.11 的 AAR 要求 minSdk 23（spec §1.1）
    defaultConfig.minSdk = 23
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":floatingx-core"))
    // 公开 API 带 @Composable 与 CompositionLocal，必须 api 暴露
    api(libs.compose.ui.versioned)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.savedstate)
    implementation(libs.coroutines.android)
    implementation(libs.kotlin.stdlib)

    // 测试里的组合内容只用 foundation 的 Box/size
    testImplementation("androidx.compose.foundation:foundation:1.11.4")
}
```

`.gitignore`（`/build`、`/.kotlin`）、`consumer-rules.pro`（`# FloatingX compose：无需额外混淆规则`）、`robolectric.properties`（`sdk=35`）。

- [ ] **Step 3: 最小真实源文件**

`FxComposeOwner.kt`（Task 2 替换为完整实现）：

```kotlin
package com.petterp.floatingx.compose

/** 占位：Task 2 实现 FxComposeOwner */
internal object ComposeModule
```

- [ ] **Step 4: 边界测试**

```kotlin
package com.petterp.floatingx.compose

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** compose 模块可以依赖 compose / lifecycle / savedstate / coroutines，但不得碰 fragment / appcompat / WindowManager */
class DependencyBoundaryTest {

    private val forbidden = listOf("androidx.fragment", "androidx.appcompat", "android.view.WindowManager")

    @Test
    fun `compose main sources import no forbidden packages`() {
        val root = File("src/main")
        assertTrue("找不到 src/main，当前目录 ${File(".").absolutePath}", root.exists())
        val violations = root.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) return@mapIndexedNotNull null
                    val imported = trimmed.removePrefix("import ").trim()
                    if (forbidden.any { imported.startsWith(it) }) "${file.relativeTo(root)}:${index + 1}: $trimmed" else null
                }
            }
            .toList()
        assertTrue("compose 违反依赖边界：\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
```

- [ ] **Step 5: 验证与提交**

Run: `./gradlew :floatingx-compose:testDebugUnitTest`
Expected: BUILD SUCCESSFUL，1 个用例。若 compose 编译器插件报 Kotlin 版本不匹配，检查 `libs.plugins.compose.compiler` 的 `version.ref = "kotlin"`（必须与 2.2.21 一致），不要改 Kotlin 版本。

Run: `./gradlew :floatingx-compose:publishToMavenLocal -PisPublish=false -PversionName=3.0.0-SNAPSHOT`
Expected: 成功；POM 里 `floatingx-core` 与 `androidx.compose.ui:ui:1.11.4` 为 compile scope，lifecycle/savedstate/coroutines 为 runtime。

```bash
git add settings.gradle floatingx-compose
git commit -m "build: 新增 floatingx-compose 模块骨架（compose 插件、minSdk 23）与依赖边界测试"
```

---

### Task 2: `FxComposeOwner`

**Files:**
- Replace: `floatingx-compose/src/main/kotlin/com/petterp/floatingx/compose/FxComposeOwner.kt`
- Test: `floatingx-compose/src/test/kotlin/com/petterp/floatingx/compose/FxComposeOwnerTest.kt`

**Interfaces:**
- Produces: `public class FxComposeOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner`，`public fun moveTo(state: Lifecycle.State)`（DESTROYED 后忽略；不允许传 DESTROYED/INITIALIZED——用 `destroy()`）、`public fun destroy()`（→ DESTROYED + `viewModelStore.clear()`，幂等）、`public fun attachTo(view: View)`（三个 `setViewTree*Owner`）、`public val isDestroyed: Boolean`。构造即 `performAttach()` + `performRestore(null)` + CREATED。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.compose

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxComposeOwnerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    class ProbeViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    @Test
    fun `starts created with a restored saved state registry`() {
        val owner = FxComposeOwner()
        assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState)
        assertTrue(owner.savedStateRegistry.isRestored)
    }

    @Test
    fun `moveTo walks the lifecycle and destroy clears view models`() {
        val owner = FxComposeOwner()
        val vm = ViewModelProvider(owner)[ProbeViewModel::class.java]
        owner.moveTo(Lifecycle.State.RESUMED)
        assertEquals(Lifecycle.State.RESUMED, owner.lifecycle.currentState)
        owner.moveTo(Lifecycle.State.CREATED)
        assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState)
        assertSame(vm, ViewModelProvider(owner)[ProbeViewModel::class.java])   // detach 不清 ViewModel
        owner.destroy()
        assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
        assertTrue(vm.cleared)
        assertTrue(owner.isDestroyed)
        owner.moveTo(Lifecycle.State.RESUMED)                                   // destroy 后忽略
        assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
        owner.destroy()                                                          // 幂等
    }

    @Test
    fun `attachTo installs all three view tree owners`() {
        val owner = FxComposeOwner()
        val view = View(context)
        owner.attachTo(view)
        assertSame(owner, view.findViewTreeLifecycleOwner())
        assertSame(owner, view.findViewTreeViewModelStoreOwner())
        assertSame(owner, view.findViewTreeSavedStateRegistryOwner())
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-compose:testDebugUnitTest --tests '*FxComposeOwnerTest*'`
Expected: 编译失败（`FxComposeOwner` 不存在）。

- [ ] **Step 3: 实现**

```kotlin
package com.petterp.floatingx.compose

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * 浮窗自己的 Lifecycle / ViewModelStore / SavedStateRegistry owner（spec §6）。
 * 归 control 所有：容器 detach 只降到 CREATED，只有 control.cancel() 才 destroy——
 * 所以 ViewModel 与 rememberSaveable 的状态能跨页面、跨 host 存活（修 #210/#239）。
 * 所有方法必须在主线程调用（LifecycleRegistry 的要求）。
 */
public class FxComposeOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    public val isDestroyed: Boolean get() = registry.currentState == Lifecycle.State.DESTROYED

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    /** 在 CREATED / STARTED / RESUMED 之间移动；已 destroy 则忽略。要销毁请用 destroy() */
    public fun moveTo(state: Lifecycle.State) {
        if (isDestroyed) return
        require(state != Lifecycle.State.DESTROYED && state != Lifecycle.State.INITIALIZED) { "请用 destroy()；不支持 $state" }
        registry.currentState = state
    }

    /** 只在 control.cancel() 时调用：DESTROYED + 清空 ViewModel。幂等 */
    public fun destroy() {
        if (isDestroyed) return
        registry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    /** 把三个 owner 装到 view 上，Compose 向上查找 ViewTree owner 时命中 */
    public fun attachTo(view: View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-compose:testDebugUnitTest`
Expected: 全绿。若 `performAttach()` 在 savedstate 1.5 上不存在/已改名，直接删掉那一行（`performRestore` 内部会 attach）；不要改 savedstate 版本。

- [ ] **Step 5: Commit**

```bash
git add floatingx-compose/src
git commit -m "feat(compose): FxComposeOwner——归 control 所有的 Lifecycle/ViewModelStore/SavedStateRegistry owner"
```

---

### Task 3: `FxComposeContent` + `LocalFxControl` + `ComposeOwnerFeature` + `compose {}` DSL

**Files:**
- Create: `floatingx-compose/src/main/kotlin/com/petterp/floatingx/compose/FxComposeContent.kt`
- Create: `floatingx-compose/src/main/kotlin/com/petterp/floatingx/compose/ComposeOwnerFeature.kt`
- Create: `floatingx-compose/src/main/kotlin/com/petterp/floatingx/compose/FxComposeExt.kt`
- Test: `floatingx-compose/src/test/kotlin/com/petterp/floatingx/compose/TestHost.kt`
- Test: `floatingx-compose/src/test/kotlin/com/petterp/floatingx/compose/FxComposeContentTest.kt`
- Test: `floatingx-compose/src/test/kotlin/com/petterp/floatingx/compose/ComposeOwnerFeatureTest.kt`

**Interfaces:**
- Consumes: core `FxContent`（`abstract fun create(context, parent): View`）、`FxFeature`（`onAttach/onDetach/onCancel/onConfigChanged/onShow/onHide`）、`FxFeatureScope`（`control/config/container`）、`FxConfigScope.content()/addFeature()`、`FxConfig.features`、`FxControl.setContent`。
- Produces:
  - `public val LocalFxControl: ProvidableCompositionLocal<FxControl>`（`staticCompositionLocalOf { error("LocalFxControl 只在 FloatingX 的 compose 内容里可用") }`）
  - `public class FxComposeContent(public val content: @Composable (FxControl) -> Unit) : FxContent()`：`public val owner: FxComposeOwner`；`create()` 返回 `ComposeView`（owner 已 attachTo；保持默认组合策略）；`internal fun bind(control: FxControl, view: View)` 调 `setContent { CompositionLocalProvider(LocalFxControl provides control) { content(control) } }`。
  - `public class ComposeOwnerFeature : FxFeature`：`onAttach` → `(config.content as? FxComposeContent)` 的 owner `attachTo(container.view)`（系统窗口根即容器）、`bind(control, contentView)`、`moveTo(STARTED)`（若 `control.isShowing` 则 RESUMED）；`onShow` → RESUMED；`onHide` → STARTED；`onDetach` → CREATED；`onCancel` → `destroy()`；`onConfigChanged(old, new)`：`old.content !== new.content` 时旧 `FxComposeContent.owner.destroy()`，新的若是 `FxComposeContent` 则同 `onAttach` 的绑定。内容不是 `FxComposeContent` 时全部空操作。feature 用字段 `bound: FxComposeContent?` 记住当前绑定的内容（`onDetach` 只清 scope 不清 bound，`onCancel` 用 bound）。
  - `@JvmSynthetic public fun FxConfigScope.compose(content: @Composable (FxControl) -> Unit)`：`content(FxComposeContent(content))` + `addFeature(ComposeOwnerFeature())`（Task 5 加去重）。

**裁决（spec §6 修订）：** spec 写"view tree 已有 owner 则复用 Activity 的"。不复用：Activity 的 owner 会随页面销毁，正是 #210/#239 的根因；浮窗永远用自己的 owner。窗口级 Recomposer 仍由当前窗口驱动（`ComposeView` 默认策略：卸下时 dispose、挂上时用当前窗口重新组合），状态靠 `ViewModel` / `rememberSaveable` 过桥。

- [ ] **Step 1: 写失败测试**

`TestHost.kt`：

```kotlin
package com.petterp.floatingx.compose

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxRect

/** 挂在 FrameLayout 上的最小 host；parent 应已在 Activity 窗口里，Compose 才会真正组合 */
class TestHost(private val parent: FrameLayout) : FxHost {
    override val context: Context get() = parent.context
    var session: FxHostSession? = null
    override fun bind(session: FxHostSession) { this.session = session; session.onHostReady() }
    override fun createContainer(): FxContainer = FxLayerContainer(parent.context)
    override fun attach(container: FxContainer) {
        parent.addView(container.view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
    override fun detach(container: FxContainer) { parent.removeView(container.view) }
    override fun bounds(): FxBounds = FxBounds(FxRect(0f, 0f, parent.width.toFloat(), parent.height.toFloat()))
    override fun release() {}
    fun lose() = session?.onHostLost()
    fun ready() = session?.onHostReady()
}
```

`FxComposeContentTest.kt`：

```kotlin
package com.petterp.floatingx.compose

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxComposeContentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `create returns a ComposeView owned by the content's owner`() {
        val content = FxComposeContent { Box(Modifier.size(10.dp)) }
        val view = content.create(context, FrameLayout(context))
        assertTrue(view is ComposeView)
        assertSame(content.owner, view.findViewTreeLifecycleOwner())
        assertSame(content.owner, view.findViewTreeViewModelStoreOwner())
        assertSame(content.owner, view.findViewTreeSavedStateRegistryOwner())
    }
}
```

`ComposeOwnerFeatureTest.kt`：

```kotlin
package com.petterp.floatingx.compose

import android.app.Activity
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ComposeOwnerFeatureTest {

    class ProbeViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    private var control: FxControl? = null

    private fun parentInWindow(): Pair<FrameLayout, TestHost> {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = FrameLayout(activity)
        activity.findViewById<ViewGroup>(android.R.id.content).addView(parent, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return parent to TestHost(parent)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun owner(c: FxControl): FxComposeOwner = (c.config.content as FxComposeContent).owner

    @After
    fun tearDown() {
        control?.takeIf { it.state != FxState.CANCELLED }?.cancel()
        control = null
    }

    @Test
    fun `dsl installs content and feature and the owner follows the control lifecycle`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") { compose { Box(Modifier.size(10.dp)) }; this.host = host }.also { control = it }
        assertTrue(c.config.content is FxComposeContent)
        assertEquals(1, c.config.features.count { it is ComposeOwnerFeature })
        val o = owner(c)
        assertEquals(Lifecycle.State.STARTED, o.lifecycle.currentState)   // attached，未 show
        c.show()
        assertEquals(Lifecycle.State.RESUMED, o.lifecycle.currentState)
        c.hide()
        assertEquals(Lifecycle.State.STARTED, o.lifecycle.currentState)
        host.lose()
        assertEquals(Lifecycle.State.CREATED, o.lifecycle.currentState)
        host.ready()
        assertSame(o, owner(c))                                              // 同一个 owner
        assertEquals(Lifecycle.State.STARTED, o.lifecycle.currentState)
        c.cancel()
        assertEquals(Lifecycle.State.DESTROYED, o.lifecycle.currentState)
    }

    @Test
    fun `composition runs against the real window and LocalFxControl is provided`() {
        val (_, host) = parentInWindow()
        var seen: FxControl? = null
        var compositions = 0
        val c = FloatingX.create("c") {
            compose { ctrl -> seen = ctrl; SideEffect { compositions++ }; Box(Modifier.size(10.dp)) }
            this.host = host
        }.also { control = it }
        c.show()
        idle()
        assertSame(c, seen)
        assertTrue(compositions >= 1)
    }

    @Test
    fun `view models and rememberSaveable survive host loss, only cancel clears them`() {
        val (_, host) = parentInWindow()
        var restored = -1
        val c = FloatingX.create("c") {
            compose {
                var count by rememberSaveable { mutableIntStateOf(0) }
                SideEffect { if (count == 0) count = 7 else restored = count }
                Box(Modifier.size(10.dp))
            }
            this.host = host
        }.also { control = it }
        val vm = ViewModelProvider(owner(c))[ProbeViewModel::class.java]
        c.show()
        idle()
        host.lose()          // 容器卸下：组合被 dispose，状态进 SavedStateRegistry
        idle()
        host.ready()         // 重新挂上：重新组合，rememberSaveable 恢复为 7
        idle()
        assertEquals(7, restored)
        assertSame(vm, ViewModelProvider(owner(c))[ProbeViewModel::class.java])
        assertFalse(vm.cleared)
        c.cancel()
        assertTrue(vm.cleared)
    }

    @Test
    fun `replacing the content destroys the old owner and binds the new one`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") { compose { Box(Modifier.size(10.dp)) }; this.host = host }.also { control = it }
        val old = owner(c)
        c.show()
        c.setContent(FxComposeContent { Box(Modifier.size(20.dp)) })
        assertTrue(old.isDestroyed)
        val new = owner(c)
        assertEquals(Lifecycle.State.RESUMED, new.lifecycle.currentState)
    }

    @Test
    fun `feature ignores non compose content`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") {
            view { ctx -> android.view.View(ctx).apply { layoutParams = ViewGroup.LayoutParams(10, 10) } }
            addFeature(ComposeOwnerFeature())
            this.host = host
        }.also { control = it }
        c.show()
        c.cancel()   // 不抛异常即可
        assertEquals(FxState.CANCELLED, c.state)
    }
}
```

`rememberSaveable` 的恢复依赖 `ComposeView` 在 dispose 时把状态写进我们 owner 的 `SavedStateRegistry`、重新组合时读回；这是 `AbstractComposeView` 的既有行为（`DisposableSaveableStateRegistry` 用 `findViewTreeSavedStateRegistryOwner()`）。若该用例在 Robolectric 下 `restored` 仍为 -1，先确认 `idle()` 后 `compositions` 确实增加（组合是否发生），再判断是环境问题还是实现问题；实现问题才修。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-compose:testDebugUnitTest --tests '*Compose*'`
Expected: 编译失败。

- [ ] **Step 3: 实现**

`FxComposeContent.kt`：

```kotlin
package com.petterp.floatingx.compose

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
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

    override fun create(context: Context, parent: ViewGroup): View =
        ComposeView(context).also { owner.attachTo(it) }

    /** ComposeOwnerFeature 在 attach 时调用：此时才有 control */
    internal fun bind(control: FxControl, view: View) {
        val composeView = view as? ComposeView ?: return
        composeView.setContent {
            CompositionLocalProvider(LocalFxControl provides control) { content(control) }
        }
    }
}
```

`ComposeOwnerFeature.kt`：

```kotlin
package com.petterp.floatingx.compose

import androidx.lifecycle.Lifecycle
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope

/**
 * 把浮窗生命周期映射到 FxComposeOwner（spec §6）：
 * attach → STARTED，show → RESUMED，hide → STARTED，detach → CREATED，只有 cancel → DESTROYED。
 * 内容不是 FxComposeContent 时什么都不做。
 */
public class ComposeOwnerFeature : FxFeature {

    private var scope: FxFeatureScope? = null
    private var bound: FxComposeContent? = null

    override fun onAttach(scope: FxFeatureScope) {
        this.scope = scope
        (scope.config.content as? FxComposeContent)?.let { bind(it, scope) }
    }

    private fun bind(content: FxComposeContent, scope: FxFeatureScope) {
        bound = content
        // 系统窗口的根 view 就是容器：Compose 的窗口级 Recomposer 从根 view 找 owner
        content.owner.attachTo(scope.container.view)
        scope.container.contentView?.let { content.bind(scope.control, it) }
        content.owner.moveTo(if (scope.control.isShowing) Lifecycle.State.RESUMED else Lifecycle.State.STARTED)
    }

    override fun onShow() { bound?.owner?.moveTo(Lifecycle.State.RESUMED) }

    override fun onHide() { bound?.owner?.moveTo(Lifecycle.State.STARTED) }

    override fun onDetach() {
        bound?.owner?.moveTo(Lifecycle.State.CREATED)
        scope = null
    }

    override fun onCancel() {
        bound?.owner?.destroy()
        bound = null
    }

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        if (old.content === new.content) return
        (old.content as? FxComposeContent)?.owner?.destroy()
        bound = null
        val s = scope ?: return
        (new.content as? FxComposeContent)?.let { bind(it, s) }
    }
}
```

`FxComposeExt.kt`：

```kotlin
package com.petterp.floatingx.compose

import androidx.compose.runtime.Composable
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.config.FxConfigScope

/** DSL：`FloatingX.install("tag") { compose { ctrl -> … }; appHost(app) }`。同一配置里只调用一次（Task 5 加去重） */
@JvmSynthetic
public fun FxConfigScope.compose(content: @Composable (FxControl) -> Unit) {
    content(FxComposeContent(content))
    addFeature(ComposeOwnerFeature())
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-compose:testDebugUnitTest`
Expected: 全绿。Compose 在 Robolectric 下若报 `Recomposer` / `Choreographer` 相关错误，确认 `parentInWindow()` 的 Activity 走了 `setup()`（decor 已 attach），并在每个断言前 `idle()`。

- [ ] **Step 5: Commit**

```bash
git add floatingx-compose/src
git commit -m "feat(compose): FxComposeContent / LocalFxControl / ComposeOwnerFeature 与 compose {} DSL（#210 #239）"
```

---

### Task 4: `stateFlow()` / `positionFlow()`

**Files:**
- Create: `floatingx-compose/src/main/kotlin/com/petterp/floatingx/compose/FxComposeFlows.kt`
- Test: `floatingx-compose/src/test/kotlin/com/petterp/floatingx/compose/FxComposeFlowsTest.kt`

**Interfaces:**
- Produces: `public fun FxControl.stateFlow(): StateFlow<FxState>`、`public fun FxControl.positionFlow(): StateFlow<FxPoint>`。同一 control 多次调用返回同一 flow（`WeakHashMap<FxControl, Flows>`）；通过一个 `FxListener` 更新：`onAttach/onDetach/onShow/onHide/onCancel` → `state = control.state`；`onDrag` / `onDragEnd` / `onPositionChanged` → `position = control.position`。cancel 后 core 清空监听器，map 里的弱引用随 control 回收。

**裁决（spec §6 修订）：** `positionFlow` 用 core 的 `FxPoint` 而不是 `android.graphics.PointF`（core 几何类型统一）。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.compose

import android.content.Context
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxComposeFlowsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun parent() = FrameLayout(context).also {
        it.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY))
        it.layout(0, 0, 1080, 1920)
    }

    @Test
    fun `stateFlow tracks show hide cancel and is cached per control`() {
        val p = parent()
        val c = FloatingX.create("f") { view { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }; host = TestHost(p) }
        val flow = c.stateFlow()
        assertSame(flow, c.stateFlow())
        assertEquals(FxState.ATTACHED, flow.value)
        c.show()
        assertEquals(FxState.SHOWN, flow.value)
        c.hide()
        assertEquals(FxState.ATTACHED, flow.value)
        c.cancel()
        assertEquals(FxState.CANCELLED, flow.value)
    }

    @Test
    fun `positionFlow follows moveTo`() {
        val p = parent()
        val c = FloatingX.create("f") { view { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }; host = TestHost(p) }
        c.show()
        p.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY))
        p.layout(0, 0, 1080, 1920)
        val flow = c.positionFlow()
        c.moveTo(300f, 400f, animate = false)
        assertEquals(300f, flow.value.x, 0f)
        assertEquals(400f, flow.value.y, 0f)
        c.cancel()
    }
}
```

- [ ] **Step 2: 运行确认失败** — 编译失败（`stateFlow` 不存在）。

- [ ] **Step 3: 实现**

```kotlin
package com.petterp.floatingx.compose

import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.WeakHashMap

/** 每个 control 一份 flow；用 FxListener 桥接，无 core 改动。control cancel 后监听器被 core 清掉，条目随 control 回收 */
private class FxControlFlows(control: FxControl) : FxListener {
    val state = MutableStateFlow(control.state)
    val position = MutableStateFlow(control.position)

    override fun onAttach(control: FxControl) { state.value = control.state }
    override fun onDetach(control: FxControl) { state.value = control.state }
    override fun onShow(control: FxControl) { state.value = control.state }
    override fun onHide(control: FxControl) { state.value = control.state }
    override fun onCancel(control: FxControl) { state.value = FxState.CANCELLED }
    override fun onDrag(control: FxControl, x: Float, y: Float) { position.value = control.position }
    override fun onDragEnd(control: FxControl, x: Float, y: Float) { position.value = control.position }
    override fun onPositionChanged(control: FxControl, anchor: FxAnchor) { position.value = control.position }
}

private val flows = WeakHashMap<FxControl, FxControlFlows>()

private fun FxControl.flows(): FxControlFlows = flows.getOrPut(this) { FxControlFlows(this).also(::addListener) }

/** 浮窗状态；主线程更新 */
public fun FxControl.stateFlow(): StateFlow<FxState> = flows().state

/** 内容左上角屏幕坐标（拖动中每帧更新、moveTo/吸附结束更新） */
public fun FxControl.positionFlow(): StateFlow<FxPoint> = flows().position
```

`moveTo(animate=false)` 走 `commitAndApply` → `onPositionChanged`；`position` 读容器坐标，此时已 apply。

- [ ] **Step 4: 运行测试** — 全绿。

- [ ] **Step 5: Commit**

```bash
git add floatingx-compose/src
git commit -m "feat(compose): FxControl.stateFlow / positionFlow"
```

---

### Task 5: core `FxConfigScope.removeFeatures` + DSL 去重 + 文档

**Files:**
- Modify: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/config/FxConfigScope.kt`（`addFeature` 之后加 `public fun removeFeatures(predicate: (FxFeature) -> Boolean) { features.removeAll(predicate) }`）与 `FxConfig.kt`（`Builder.removeFeatures(predicate: (FxFeature) -> Boolean): Builder = apply { features.removeAll(predicate) }`）
- Modify: `floatingx-compose/src/main/kotlin/com/petterp/floatingx/compose/FxComposeExt.kt`（先 `removeFeatures { it is ComposeOwnerFeature }` 再 add；KDoc 去掉"只调用一次"）
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/config/FxConfigTest.kt` + 1 用例；`ComposeOwnerFeatureTest` + 1 用例
- Modify: spec §6、`CLAUDE.md`

- [ ] **Step 1: 测试**

`FxConfigTest`：

```kotlin
    @Test
    fun `removeFeatures drops matching features`() {
        val config = FxConfigScope(null).apply { content(content); addFeature(feature); removeFeatures { it === feature } }.build()
        assertTrue(config.features.isEmpty())
    }
```

`ComposeOwnerFeatureTest`：

```kotlin
    @Test
    fun `calling compose twice keeps a single feature and the last content`() {
        val (_, host) = parentInWindow()
        val c = FloatingX.create("c") {
            compose { Box(Modifier.size(10.dp)) }
            compose { Box(Modifier.size(20.dp)) }
            this.host = host
        }.also { control = it }
        assertEquals(1, c.config.features.count { it is ComposeOwnerFeature })
    }
```

- [ ] **Step 2–4:** 先跑确认失败，实现，再跑 `./gradlew :floatingx-core:testDebugUnitTest :floatingx-compose:testDebugUnitTest` 全绿。

- [ ] **Step 5: 文档**

spec §6（用 Edit 精确替换）：
- 代码块 `public fun FxControl.positionFlow(): StateFlow<PointF>` → `StateFlow<FxPoint>`；追加两行 `public class FxComposeContent(content: @Composable (FxControl) -> Unit) : FxContent()` 与 `public class ComposeOwnerFeature : FxFeature   // compose {} 自动注册`。
- "若容器所在 view tree 已有 owner（AppHost 挂在 Activity 下），则不覆盖，直接复用 Activity 的。" 一条改为：`浮窗永远用自己的 FxComposeOwner（Activity 的 owner 随页面销毁正是 #210/#239 的根因）；owner 装在内容 view 上，attach 时也装到容器根 view（系统窗口的窗口级 Recomposer 从根 view 找 owner）。ComposeView 保持默认组合策略：卸下时 dispose、挂上时用当前窗口的 Recomposer 重新组合；ViewModel 与 rememberSaveable 通过 owner 跨页面/跨 host 存活。`
- 生命周期一条改为：`attach → STARTED，show → RESUMED，hide → STARTED，detach → CREATED，只有 cancel → DESTROYED + ViewModelStore.clear()`。

CLAUDE.md 模块表加 `floatingx-compose`（minSdk 23），测试命令加 `:floatingx-compose:test`，Plan 4 计划路径。

- [ ] **Step 6: 全量测试与提交**

Run: `./gradlew test`；commit `feat(compose): compose {} 去重（core FxConfigScope.removeFeatures）；docs: spec §6 同步 Plan 4 裁决`。

---

## 自查记录

- **Spec 覆盖：** §6 四个 API（`compose`、`LocalFxControl`、`stateFlow`、`positionFlow`）→ Task 3/4；owner 归 control、detach 只 pause/stop、cancel 才 destroy → Task 2/3；0 尺寸不定位由核心锚点模型已覆盖（无需本模块动作）；owner 在 `FxContent.create()` 内设置、destroy 挂 `FxFeature.onCancel` → Task 3；仅本模块依赖 coroutines → Task 1。
- **占位扫描：** 无。
- **类型一致性：** `FxComposeContent.owner`（Task 3 定义，Task 3/5 测试使用）、`FxComposeOwner.moveTo/destroy/attachTo/isDestroyed`（Task 2 定义，Task 3 使用）、`TestHost.lose()/ready()`（Task 3 定义，Task 3/4 使用）、`removeFeatures`（Task 5 定义，Task 5 使用）。
