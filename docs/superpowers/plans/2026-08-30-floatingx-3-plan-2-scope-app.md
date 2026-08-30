# FloatingX 3.0 Plan 2：floatingx-scope + floatingx-app 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在已完成的 `floatingx-core` 之上落地两个 host 模块：`floatingx-scope`（`ViewGroupHost` / `FragmentHost` + `Activity/ViewGroup/Fragment.fxScope` 扩展）与 `floatingx-app`（`AppHost`：前台 Activity 跟踪、静默换父、黑白名单/父类过滤、DecorView/Content 挂载、safe-area insets），全部带 Robolectric 测试，并给 core 补上 `modal()` 配置入口。

**Architecture:** host 只做三件事——决定父容器、在合适的时机回调 `FxHostSession.onHostReady/onHostLost/onBoundsChanged`、提供 `bounds()`。scope 的两个 host 直接把 `FxLayerContainer` 加到给定 ViewGroup；`AppHost` 通过 `FxActivityTracker` 观察前台 Activity，在 `onActivityPostResumed`（API 29+）或 `onActivityResumed` 后 `decorView.post`（API < 29）把**同一个容器**从旧 DecorView 静默挪到新 DecorView（engine 状态、feature、动画均不重来，位置由 translation 保留、父容器布局后按锚点校正）；只有当前挂载的 Activity 被销毁或被过滤规则拒绝时才走 `onHostLost`。`modal` 是 Layer 容器的通用能力（`ModalScrimFeature`），因此从 spec §3 的 `AppHost.Builder` 挪到 `FxConfigScope.modal()` / `FxConfig.Builder.modal()`，scope 与 app 共用。

**Tech Stack:** Kotlin 2.2.21、AGP 8.13.2、JDK 17、compileSdk 36、minSdk 21、androidx.core 1.13.1（`ViewCompat` / `WindowInsetsCompat`）、androidx.fragment 1.8.9（scope 模块 `compileOnly`，测试 `testImplementation`）、JUnit 4.13.2、Robolectric 4.16.1（sdk=35）。

**Spec:** `docs/superpowers/specs/2026-08-29-floatingx-3-modular-architecture-design.md`（§3、§5、§7、§8、§10 由本计划实现；§2 已由 Plan 1 落地）。Plan 1 的遗留项与裁决见 `docs/superpowers/plans/2026-08-29-floatingx-3-plan-1-core.md` 末尾。

## Global Constraints

- 仓库工具链：AGP `8.13.2`、Gradle `8.14.3`、Kotlin `2.2.21`、JDK 17；库模块用 `build-logic` 的 `floatingx.library` convention plugin（已存在，不改）：`minSdk 21`、`compileSdk 36`、`explicitApi()`、`jvmTarget 17`、`jvmDefault ENABLE`、Robolectric 配置、maven 坐标 `io.github.petterpx:<module>`。
- 依赖边界（spec §1）：`floatingx-scope` 只允许 `api(project(":floatingx-core"))` + `kotlin-stdlib` + `compileOnly(androidx.fragment:fragment:1.8.9)`；`floatingx-app` 只允许 `api(project(":floatingx-core"))` + `implementation(androidx.core:core:1.13.1)` + `kotlin-stdlib`。两个模块的 `src/main` 都禁止 import `android.view.WindowManager`、`androidx.compose`、`androidx.appcompat`、`androidx.savedstate`、`kotlinx.coroutines`；`floatingx-app` 另禁止 `androidx.fragment`、`androidx.lifecycle`；`floatingx-scope` 的 `androidx.fragment` / `androidx.lifecycle` import **只允许出现在** `FragmentHost.kt` 与 `FxFragmentScope.kt` 两个文件（Task 1 的边界测试守住）。
- 包名：scope 在 `com.petterp.floatingx.scope`，app 在 `com.petterp.floatingx.app`；`namespace` 同名。不得使用 core 的 `internal` API（`FxControlImpl` / `FxEngine` 等）——host 只能通过 `FxHost` / `FxHostSession` / `FxContainer` 公开接口与 core 交互。
- Java 互操作（spec §7）：`ViewGroupHost.of(viewGroup)`、`AppHost.builder(app)` / `AppHost.Builder` 是 Java 入口；接受 Kotlin lambda 的 DSL 扩展（`fxScope`、`viewGroupHost`、`fragmentHost`、`appHost`）标 `@JvmSynthetic`；Java 用的回调接口用 `fun interface`（`AppActivityFilter`）。
- 性能（spec §8）：换父路径除 `ViewGroup.LayoutParams` 外无分配；host 不得用 `post/postDelayed` 兜底定位——唯一允许的 `post` 是 API < 29 的 `onActivityResumed` 后的主线程 `Handler.post`（spec §3 的 `decorView.post` 按 Task 7 裁决改为 Handler）；`bounds()` 不在触摸 MOVE 路径上（core 的 `LocationFeature` 已在拖动开始时缓存 `layoutInput`），可按需计算。
- 注释、日志、KDoc 用中文；提交信息用中文、Conventional Commits 前缀（`feat:` / `build:` / `test:` / `docs:`）。
- 每个 Task 结束时 `./gradlew :floatingx-scope:testDebugUnitTest :floatingx-app:testDebugUnitTest :floatingx-core:testDebugUnitTest` 必须全绿；最后一个 Task 跑完整 `./gradlew test`。所有 `./gradlew` 在仓库根目录执行。
- Robolectric 用例 sdk=35（`src/test/resources/robolectric.properties`），需要低版本分支的用例用 `@Config(sdk = [28])`。

---

## 文件结构（本计划新增 / 修改）

```
settings.gradle                                    include ':floatingx-scope' / ':floatingx-app'
floatingx-core/src/main/kotlin/com/petterp/floatingx/core/config/
  FxConfigScope.kt                                 + modal(enabled, dismissOnOutsideTouch)
  FxConfig.kt                                      + Builder.modal(enabled, dismissOnOutsideTouch)
floatingx-core/src/test/kotlin/com/petterp/floatingx/core/config/FxConfigTest.kt   + 3 个 modal 用例

floatingx-scope/
  build.gradle.kts / .gitignore / consumer-rules.pro
  src/test/resources/robolectric.properties
  src/main/kotlin/com/petterp/floatingx/scope/
    ViewGroupHost.kt                               任意 ViewGroup 承载（Layer 容器）
    FxScope.kt                                     Activity.fxScope / ViewGroup.fxScope / FxInstallScope.viewGroupHost
    FragmentHost.kt                                Fragment 承载：等 view 创建、view 销毁即 lost（仅此文件与下一文件可 import fragment/lifecycle）
    FxFragmentScope.kt                             Fragment.fxScope / FxInstallScope.fragmentHost（fragment destroy 自动 cancel）
  src/test/kotlin/com/petterp/floatingx/scope/
    DependencyBoundaryTest.kt
    ViewGroupHostTest.kt
    FxScopeTest.kt
    FragmentHostTest.kt
    JavaScopeApiTest.java

floatingx-app/
  build.gradle.kts / .gitignore / consumer-rules.pro
  src/test/resources/robolectric.properties
  src/main/kotlin/com/petterp/floatingx/app/
    AppAttachTarget.kt                             DECOR / CONTENT
    AppActivityFilter.kt                           fun interface
    internal/ActivityRules.kt                      黑白名单 + 父类匹配 + 自定义过滤
    AppHost.kt                                     host 实现 + Builder
    FxAppExt.kt                                    FxInstallScope.appHost DSL + FxControl.attachedActivity
  src/test/kotlin/com/petterp/floatingx/app/
    DependencyBoundaryTest.kt
    ActivityRulesTest.kt
    AppHostTest.kt
    AppHostInsetsTest.kt
    JavaAppApiTest.java

docs/superpowers/specs/2026-08-29-floatingx-3-modular-architecture-design.md   §3 / §5 同步本计划裁决
CLAUDE.md                                          模块表 + 测试命令
```

---

### Task 1: 两个模块的骨架与依赖边界测试

**Files:**
- Modify: `settings.gradle`（`include ':floatingx-core'` 之后追加两行）
- Create: `floatingx-scope/build.gradle.kts`、`floatingx-scope/.gitignore`、`floatingx-scope/consumer-rules.pro`、`floatingx-scope/src/test/resources/robolectric.properties`
- Create: `floatingx-app/build.gradle.kts`、`floatingx-app/.gitignore`、`floatingx-app/consumer-rules.pro`、`floatingx-app/src/test/resources/robolectric.properties`
- Test: `floatingx-scope/src/test/kotlin/com/petterp/floatingx/scope/DependencyBoundaryTest.kt`
- Test: `floatingx-app/src/test/kotlin/com/petterp/floatingx/app/DependencyBoundaryTest.kt`

**Interfaces:**
- Consumes: `build-logic` 的 `floatingx.library` 插件；版本目录别名 `libs.androidx.core`、`libs.kotlin.stdlib`、`libs.fragment`。
- Produces: 两个可编译、可 `publishToMavenLocal` 的空模块；后续 Task 只往 `src/main/kotlin` 加文件。

- [ ] **Step 1: settings.gradle 加入两个模块**

在 `settings.gradle` 的 `include ':floatingx-core'` 之后追加：

```groovy
include ':floatingx-scope'
include ':floatingx-app'
```

- [ ] **Step 2: 创建 floatingx-scope 的构建文件**

`floatingx-scope/build.gradle.kts`：

```kotlin
plugins {
    id("floatingx.library")
}

android {
    namespace = "com.petterp.floatingx.scope"
}

dependencies {
    api(project(":floatingx-core"))
    implementation(libs.kotlin.stdlib)
    // Fragment 支持是可选的：只有调用 Fragment.fxScope / FragmentHost 的使用方才需要自己依赖 androidx.fragment
    compileOnly(libs.fragment)

    testImplementation(libs.fragment)
}
```

`floatingx-scope/.gitignore`：

```
/build
/.kotlin
```

`floatingx-scope/consumer-rules.pro`：

```
# FloatingX scope：无需额外混淆规则
```

`floatingx-scope/src/test/resources/robolectric.properties`：

```
sdk=35
```

- [ ] **Step 3: 创建 floatingx-app 的构建文件**

`floatingx-app/build.gradle.kts`：

```kotlin
plugins {
    id("floatingx.library")
}

android {
    namespace = "com.petterp.floatingx.app"
}

dependencies {
    api(project(":floatingx-core"))
    // ViewCompat.getRootWindowInsets / WindowInsetsCompat：safe area insets
    implementation(libs.androidx.core)
    implementation(libs.kotlin.stdlib)
}
```

`floatingx-app/.gitignore`、`consumer-rules.pro`、`robolectric.properties` 内容与 scope 相同（consumer-rules 注释改为 `# FloatingX app：无需额外混淆规则`）。

- [ ] **Step 4: 写两个依赖边界测试**

`floatingx-scope/src/test/kotlin/com/petterp/floatingx/scope/DependencyBoundaryTest.kt`：

```kotlin
package com.petterp.floatingx.scope

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * scope 模块的依赖边界（spec §1 + Plan 2 Global Constraints）：
 * fragment / lifecycle 只允许出现在 FragmentHost.kt 与 FxFragmentScope.kt。
 */
class DependencyBoundaryTest {

    private val forbiddenEverywhere = listOf(
        "android.view.WindowManager",
        "androidx.compose",
        "androidx.appcompat",
        "androidx.savedstate",
        "kotlinx.coroutines",
    )
    private val fragmentOnly = listOf("androidx.fragment", "androidx.lifecycle")
    private val fragmentFiles = setOf("FragmentHost.kt", "FxFragmentScope.kt")

    @Test
    fun `scope main sources respect dependency boundary`() {
        val root = File("src/main")
        assertTrue("找不到 src/main，当前目录 ${File(".").absolutePath}", root.exists())
        val violations = root.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) return@mapIndexedNotNull null
                    val imported = trimmed.removePrefix("import ").trim()
                    val bad = forbiddenEverywhere.any { imported.startsWith(it) } ||
                        (file.name !in fragmentFiles && fragmentOnly.any { imported.startsWith(it) })
                    if (bad) "${file.relativeTo(root)}:${index + 1}: $trimmed" else null
                }
            }
            .toList()
        assertTrue("scope 违反依赖边界：\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
```

`floatingx-app/src/test/kotlin/com/petterp/floatingx/app/DependencyBoundaryTest.kt`：

```kotlin
package com.petterp.floatingx.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** app 模块只依赖 core + androidx.core（spec §1） */
class DependencyBoundaryTest {

    private val forbidden = listOf(
        "android.view.WindowManager",
        "androidx.fragment",
        "androidx.compose",
        "androidx.lifecycle",
        "androidx.appcompat",
        "androidx.savedstate",
        "kotlinx.coroutines",
    )

    @Test
    fun `app main sources import no forbidden packages`() {
        val root = File("src/main")
        assertTrue("找不到 src/main，当前目录 ${File(".").absolutePath}", root.exists())
        val violations = root.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    val imported = trimmed.removePrefix("import ").trim()
                    if (trimmed.startsWith("import ") && forbidden.any { imported.startsWith(it) }) {
                        "${file.relativeTo(root)}:${index + 1}: $trimmed"
                    } else null
                }
            }
            .toList()
        assertTrue("app 违反依赖边界：\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
```

两个测试要求 `src/main` 存在，所以本 Task 直接创建后续 Task 会用到的最小真实源文件（不要用 .gitkeep 之类占位）：

`floatingx-scope/src/main/kotlin/com/petterp/floatingx/scope/ViewGroupHost.kt`（Task 3 会替换成完整实现）：

```kotlin
package com.petterp.floatingx.scope

/** 占位：Task 3 实现 ViewGroupHost */
internal object ScopeModule
```

`floatingx-app/src/main/kotlin/com/petterp/floatingx/app/AppAttachTarget.kt`（Task 6 会补 KDoc，内容已是最终版）：

```kotlin
package com.petterp.floatingx.app

/** AppHost 把容器挂到 Activity 的哪一层 */
public enum class AppAttachTarget {
    /** Window 的 DecorView（默认）：拖动范围是整个窗口，不受状态栏/导航栏裁剪（spec §3） */
    DECOR,

    /** android.R.id.content：只覆盖内容区 */
    CONTENT,
}
```

- [ ] **Step 5: 运行测试与发布验证**

Run: `./gradlew :floatingx-scope:testDebugUnitTest :floatingx-app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL，两个 `DependencyBoundaryTest` 通过。

Run: `./gradlew :floatingx-scope:publishToMavenLocal :floatingx-app:publishToMavenLocal -PisPublish=false -PversionName=3.0.0-SNAPSHOT`
Expected: BUILD SUCCESSFUL；`~/.m2/repository/io/github/petterpx/floatingx-scope/3.0.0-SNAPSHOT/floatingx-scope-3.0.0-SNAPSHOT.pom` 中 `floatingx-core` 依赖 scope 为 `compile`，`fragment` 依赖**不出现**（compileOnly 不进 POM）。用 `grep -A3 'floatingx-core\|fragment' ~/.m2/repository/io/github/petterpx/floatingx-scope/3.0.0-SNAPSHOT/*.pom` 确认。

如果 `compileOnly(libs.fragment)` 触发 manifest merger 的 minSdk 校验失败（fragment 1.8.9 声明的 minSdk 高于 21），在 `floatingx-scope/src/main/AndroidManifest.xml` 加 `<uses-sdk tools:overrideLibrary="androidx.fragment" />`——但先确认错误信息里确实是 fragment；不要顺手提高模块 minSdk。

- [ ] **Step 6: Commit**

```bash
git add settings.gradle floatingx-scope floatingx-app
git commit -m "build: 新增 floatingx-scope / floatingx-app 模块骨架与依赖边界测试"
```

---

### Task 2: core 增加 `modal()` 配置入口

**Files:**
- Modify: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/config/FxConfigScope.kt`
- Modify: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/config/FxConfig.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/config/FxConfigTest.kt`

**Interfaces:**
- Consumes: `com.petterp.floatingx.core.feature.ModalScrimFeature(dismissOnOutsideTouch: Boolean = false)`（已存在）。
- Produces: `FxConfigScope.modal(enabled: Boolean = true, dismissOnOutsideTouch: Boolean = false)`；`FxConfig.Builder.modal(enabled: Boolean = true, dismissOnOutsideTouch: Boolean = false): Builder`（`@JvmOverloads`）。

**裁决（spec §3 修订）：** spec 把 `modal(enabled, dismissOnOutsideTouch)` 放在 `AppHost.Builder`，但 host 拿不到 control/config，无法注册 feature；而 `ModalScrimFeature` 对所有 Layer 容器（app、scope）都有效。因此 `modal` 落在配置层，Task 8 同步 spec。

- [ ] **Step 1: 写失败测试**

在 `FxConfigTest.kt` 里追加（保留文件已有 import 与用例；补 `import com.petterp.floatingx.core.feature.ModalScrimFeature`）：

```kotlin
    @Test
    fun `dsl modal adds a single ModalScrimFeature`() {
        val config = FxConfigScope(null).apply {
            content(content)
            modal()
            modal(dismissOnOutsideTouch = true)
        }.build()
        assertEquals(1, config.features.count { it is ModalScrimFeature })
    }

    @Test
    fun `dsl modal false removes ModalScrimFeature`() {
        val base = FxConfigScope(null).apply { content(content); modal() }.build()
        val updated = FxConfigScope(base).apply { modal(enabled = false) }.build()
        assertTrue(updated.features.none { it is ModalScrimFeature })
    }

    @Test
    fun `builder modal mirrors dsl`() {
        val config = FxConfig.builder(content).modal().modal(false).modal(true, true).build()
        assertEquals(1, config.features.count { it is ModalScrimFeature })
    }
```

`content` 是该测试类已有的 `FxContent.layout(1)` 字段；`FxConfigTest` 是纯 JVM 测试（无 Robolectric），新用例不得引入 View/Context。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests '*FxConfigTest*'`
Expected: 编译失败，`Unresolved reference: modal`。

- [ ] **Step 3: 实现**

`FxConfigScope.kt` 的 `addFeature` 之后加（补 `import com.petterp.floatingx.core.feature.ModalScrimFeature`）：

```kotlin
    /**
     * 拦截内容之外的触摸（#212），dismissOnOutsideTouch=true 时点击外部自动 hide（#151）。
     * 只对 Layer 容器（app / scope）生效；重复调用只保留最后一次。
     */
    public fun modal(enabled: Boolean = true, dismissOnOutsideTouch: Boolean = false) {
        features.removeAll { it is ModalScrimFeature }
        if (enabled) features += ModalScrimFeature(dismissOnOutsideTouch)
    }
```

`FxConfig.kt` 的 `Builder.addFeature` 之后加（补同一 import）：

```kotlin
        /** 见 FxConfigScope.modal */
        @JvmOverloads
        public fun modal(enabled: Boolean = true, dismissOnOutsideTouch: Boolean = false): Builder = apply {
            features.removeAll { it is ModalScrimFeature }
            if (enabled) features += ModalScrimFeature(dismissOnOutsideTouch)
        }
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest`
Expected: 全绿（原 127 + 3）。

- [ ] **Step 5: Commit**

```bash
git add floatingx-core/src
git commit -m "feat(core): 配置层增加 modal() 入口，ModalScrimFeature 对 app/scope 通用"
```

---

### Task 3: `ViewGroupHost`

**Files:**
- Replace: `floatingx-scope/src/main/kotlin/com/petterp/floatingx/scope/ViewGroupHost.kt`
- Test: `floatingx-scope/src/test/kotlin/com/petterp/floatingx/scope/ViewGroupHostTest.kt`

**Interfaces:**
- Consumes: `FxHost`、`FxHostSession`、`FxContainer`、`FxLayerContainer(context)`、`FxBounds(rect, insets)`、`FxRect`。
- Produces: `public class ViewGroupHost(public val viewGroup: ViewGroup) : FxHost`，`companion object { @JvmStatic fun of(viewGroup: ViewGroup): ViewGroupHost }`。

**行为约定：**
- `bind()`：记录 session、给 `viewGroup` 加 `OnAttachStateChangeListener`，然后**立即** `onHostReady()`（父容器已存在；尺寸为 0 的情况由 core 的 `layoutInput()` 守卫 + 容器 `onSizeChanged` 触发的 `onBoundsChanged` 处理，spec §2.2）。
- `viewGroup` 从 window 卸下 → `onHostLost()`；重新挂上 → `onHostReady()`（engine 对非 INSTALLED 态的 ready 幂等）。
- `attach()`：`viewGroup.addView(container.view, MATCH_PARENT × MATCH_PARENT)`；`detach()`：`viewGroup.removeView(container.view)`。
- `bounds()`：`viewGroup` 的 width/height，insets 为 `NONE`（局部浮窗不关心系统栏）。
- `release()`：移除监听、清 session。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.scope

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.layout.FxGravity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViewGroupHostTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var control: FxControl? = null

    /** 记录 host 回调顺序的最小 session */
    private class RecordingSession : FxHostSession {
        val events = mutableListOf<String>()
        override fun onHostReady() { events += "ready" }
        override fun onHostLost() { events += "lost" }
        override fun onBoundsChanged() { events += "bounds" }
        override fun requestSwap(fallback: FxHost) { events += "swap" }
    }

    private fun parent(w: Int = 1080, h: Int = 1920): FrameLayout = FrameLayout(context).also {
        it.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
        it.layout(0, 0, w, h)
    }

    private fun content(): FxContent = FxContent.provider { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }

    private fun install(host: ViewGroupHost): FxControl =
        FloatingX.create(FxConfig.builder(content()).anchor(FxGravity.TOP_START).build(), host, "vg").also { control = it }

    @After
    fun tearDown() {
        control?.takeIf { it.state != FxState.CANCELLED }?.cancel()
        control = null
    }

    @Test
    fun `bind reports ready immediately`() {
        val host = ViewGroupHost(parent())
        val session = RecordingSession()
        host.bind(session)
        assertEquals(listOf("ready"), session.events)
    }

    @Test
    fun `attach adds match_parent layer to the view group and detach removes it`() {
        val p = parent()
        val host = ViewGroupHost(p)
        val c = install(host)
        c.show()
        assertEquals(1, p.childCount)
        val layer = p.getChildAt(0)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, layer.layoutParams.width)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, layer.layoutParams.height)
        assertEquals(FxState.SHOWN, c.state)
        c.cancel()
        assertEquals(0, p.childCount)
    }

    @Test
    fun `content is positioned by anchor after parent layout`() {
        val p = parent()
        val c = install(ViewGroupHost(p))
        c.show()
        // 容器与内容尚未布局：跑一次父容器布局，容器 onSizeChanged → onBoundsChanged → 按锚点定位
        p.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY))
        p.layout(0, 0, 1080, 1920)
        assertEquals(0f, c.position.x, 0f)
        assertEquals(0f, c.position.y, 0f)
        c.moveTo(300f, 400f, animate = false)
        assertEquals(300f, c.position.x, 0f)
        assertEquals(400f, c.position.y, 0f)
    }

    @Test
    fun `window detach reports lost and re-attach reports ready`() {
        // 用真实 Activity 拿到会经历 window attach/detach 的 ViewGroup
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val contentView = controller.get().findViewById<ViewGroup>(android.R.id.content)
        val host = ViewGroupHost(contentView)
        val c = install(host)
        c.show()
        assertEquals(FxState.SHOWN, c.state)
        assertEquals(1, contentView.childCount)

        controller.pause().stop().destroy()   // DecorView 从 window 卸下 → onViewDetachedFromWindow
        assertEquals(FxState.INSTALLED, c.state)
        assertEquals(0, contentView.childCount)
    }

    @Test
    fun `bounds follow view group size with no insets`() {
        val host = ViewGroupHost(parent(720, 1280))
        val b = host.bounds()
        assertEquals(720f, b.rect.width, 0f)
        assertEquals(1280f, b.rect.height, 0f)
        assertEquals(0f, b.insets.top, 0f)
    }

    @Test
    fun `release stops forwarding window events`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val contentView = controller.get().findViewById<ViewGroup>(android.R.id.content)
        val host = ViewGroupHost(contentView)
        val session = RecordingSession()
        host.bind(session)
        host.release()
        controller.pause().stop().destroy()
        assertEquals(listOf("ready"), session.events)
    }

    @Test
    fun `of is a java friendly factory`() {
        val p = parent()
        assertSame(p, ViewGroupHost.of(p).viewGroup)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-scope:testDebugUnitTest --tests '*ViewGroupHostTest*'`
Expected: 编译失败，`Unresolved reference: ViewGroupHost`（占位文件里只有 `ScopeModule`）。

- [ ] **Step 3: 实现**

用下面内容**整体替换** `ViewGroupHost.kt`：

```kotlin
package com.petterp.floatingx.scope

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxRect

/**
 * 把浮窗挂到任意 ViewGroup 上的 host（spec §5）。
 * - bind 后立即 ready：父容器已存在，尺寸为 0 时 core 不会定位，等容器 onSizeChanged 再算。
 * - viewGroup 从 window 卸下即 lost（Activity 销毁、RecyclerView 回收…），重新挂上再 ready。
 * - 不进 FloatingX 注册表，生命周期归调用方：不再需要时调用 control.cancel()。
 * - viewGroup 应是 FrameLayout 一类可叠放子 view 的容器（FrameLayout / ConstraintLayout / CoordinatorLayout / android.R.id.content）。
 */
public class ViewGroupHost(public val viewGroup: ViewGroup) : FxHost {

    override val context: Context get() = viewGroup.context

    private var session: FxHostSession? = null

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) { session?.onHostReady() }
        override fun onViewDetachedFromWindow(v: View) { session?.onHostLost() }
    }

    override fun bind(session: FxHostSession) {
        this.session = session
        viewGroup.addOnAttachStateChangeListener(attachListener)
        session.onHostReady()
    }

    override fun createContainer(): FxContainer = FxLayerContainer(context)

    override fun attach(container: FxContainer) {
        viewGroup.addView(container.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    override fun detach(container: FxContainer) {
        viewGroup.removeView(container.view)
    }

    override fun bounds(): FxBounds = FxBounds(FxRect(0f, 0f, viewGroup.width.toFloat(), viewGroup.height.toFloat()))

    override fun release() {
        viewGroup.removeOnAttachStateChangeListener(attachListener)
        session = null
    }

    public companion object {
        /** Java 入口：FloatingX.create(config, ViewGroupHost.of(viewGroup)) */
        @JvmStatic
        public fun of(viewGroup: ViewGroup): ViewGroupHost = ViewGroupHost(viewGroup)
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-scope:testDebugUnitTest`
Expected: 全绿（边界测试 + 7 个 host 用例）。若 `window detach reports lost` 用例里 `destroy()` 后 state 仍为 SHOWN，说明 Robolectric 的 `destroy()` 没有把 DecorView 从 WindowManager 移除——改为在 `destroy()` 前显式 `controller.get().windowManager.removeViewImmediate(controller.get().window.decorView)`，并在测试里注明原因；不要改实现。

- [ ] **Step 5: Commit**

```bash
git add floatingx-scope/src
git commit -m "feat(scope): ViewGroupHost——任意 ViewGroup 承载的 Layer host"
```

---

### Task 4: `Activity.fxScope` / `ViewGroup.fxScope` / `viewGroupHost` DSL

**Files:**
- Create: `floatingx-scope/src/main/kotlin/com/petterp/floatingx/scope/FxScope.kt`
- Test: `floatingx-scope/src/test/kotlin/com/petterp/floatingx/scope/FxScopeTest.kt`
- Test: `floatingx-scope/src/test/kotlin/com/petterp/floatingx/scope/JavaScopeApiTest.java`

**Interfaces:**
- Consumes: `FloatingX.create(tag: String = "", block: FxInstallScope.() -> Unit): FxControl`（`FxInstallScope : FxConfigScope`，有 `var host: FxHost?`）；`ViewGroupHost`（Task 3）。
- Produces:
  - `@JvmSynthetic public fun FxInstallScope.viewGroupHost(viewGroup: ViewGroup): ViewGroupHost`（创建并**同时设置** `host`，返回以便链式）
  - `@JvmSynthetic public fun ViewGroup.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl`
  - `@JvmSynthetic public fun Activity.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl`——挂在 `android.R.id.content`；API 29+ 通过 `Activity.registerActivityLifecycleCallbacks` 在该 Activity destroy 时自动 `cancel()`，API < 29 无此回调（文档说明：调用方在 `onDestroy` 里 cancel；不 cancel 也不泄漏——host 只被 Activity 的 view 树引用）。

**裁决：** spec §5 的签名没有 `tag`；core 的 `create()` 已规定"空 tag 不做位置持久化"，所以两个扩展都加 `tag: String = ""` 首参数，让局部浮窗也能持久化位置。

- [ ] **Step 1: 写失败测试**

`FxScopeTest.kt`：

```kotlin
package com.petterp.floatingx.scope

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.layout.FxGravity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class FxScopeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val created = mutableListOf<FxControl>()

    private fun box(ctx: Context) = View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) }

    @After
    fun tearDown() {
        created.filter { it.state != FxState.CANCELLED }.forEach { it.cancel() }
        created.clear()
    }

    @Test
    fun `view group fxScope mounts inside that view group`() {
        val parent = FrameLayout(context)
        val control = parent.fxScope { view(::box); anchor(FxGravity.BOTTOM_END) }.also(created::add)
        control.show()
        assertTrue(control.host is ViewGroupHost)
        assertSame(parent, (control.host as ViewGroupHost).viewGroup)
        assertEquals(1, parent.childCount)
        assertEquals(FxState.SHOWN, control.state)
    }

    @Test
    fun `activity fxScope mounts on android R id content`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val control = activity.fxScope { view(::box) }.also(created::add)
        control.show()
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        assertSame(content, (control.host as ViewGroupHost).viewGroup)
        assertEquals(1, content.childCount)
    }

    @Test
    fun `activity fxScope cancels automatically when the activity is destroyed`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val control = controller.get().fxScope { view(::box) }.also(created::add)
        control.show()
        controller.pause().stop().destroy()
        assertEquals(FxState.CANCELLED, control.state)
    }

    @Test
    @Config(sdk = [28])
    fun `activity fxScope below api 29 only loses host on destroy`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val control = controller.get().fxScope { view(::box) }.also(created::add)
        control.show()
        controller.pause().stop().destroy()
        assertEquals(FxState.INSTALLED, control.state)   // 没有 per-Activity 回调，靠 window detach → lost
    }

    @Test
    fun `viewGroupHost dsl sets host on install scope`() {
        val parent = FrameLayout(context)
        val control = FloatingX.create("dsl") { view(::box); viewGroupHost(parent) }.also(created::add)
        assertSame(parent, (control.host as ViewGroupHost).viewGroup)
    }

    @Test
    fun `tag is forwarded for persistence`() {
        val parent = FrameLayout(context)
        val control = parent.fxScope(tag = "local-a") { view(::box) }.also(created::add)
        assertEquals("local-a", control.tag)
    }
}
```

`JavaScopeApiTest.java`（放在同一测试源集，Kotlin 测试源集编译 Java 文件没问题——Plan 1 的 `JavaApiTest.java` 就是这样）：

```java
package com.petterp.floatingx.scope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import com.petterp.floatingx.core.FloatingX;
import com.petterp.floatingx.core.FxControl;
import com.petterp.floatingx.core.FxState;
import com.petterp.floatingx.core.config.FxConfig;
import com.petterp.floatingx.core.config.FxContent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Java 侧只用 ViewGroupHost.of + FloatingX.create（spec §5） */
@RunWith(RobolectricTestRunner.class)
public class JavaScopeApiTest {

    @Test
    public void createWithViewGroupHost() {
        Context context = ApplicationProvider.getApplicationContext();
        FrameLayout parent = new FrameLayout(context);
        FxConfig config = FxConfig.builder(FxContent.view(new View(context))).modal(true, true).build();
        FxControl control = FloatingX.create(config, ViewGroupHost.of(parent), "java");
        control.show();
        assertSame(parent, ((ViewGroupHost) control.getHost()).getViewGroup());
        assertEquals(FxState.SHOWN, control.getState());
        control.cancel();
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-scope:testDebugUnitTest --tests '*FxScopeTest*'`
Expected: 编译失败，`Unresolved reference: fxScope`。

- [ ] **Step 3: 实现 FxScope.kt**

```kotlin
package com.petterp.floatingx.scope

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfigScope
import com.petterp.floatingx.core.config.FxInstallScope

/** DSL：`FloatingX.create { viewGroupHost(parent); layout(...) }`，创建 host 并设置到 scope 上 */
@JvmSynthetic
public fun FxInstallScope.viewGroupHost(viewGroup: ViewGroup): ViewGroupHost =
    ViewGroupHost(viewGroup).also { host = it }

/**
 * 局部浮窗：挂在本 ViewGroup 内，不进注册表。
 * tag 只用于日志与位置持久化的存储键（空则不持久化，见 FloatingX.create）。
 * 不再需要时调用 control.cancel()。
 */
@JvmSynthetic
public fun ViewGroup.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl =
    FloatingX.create(tag) {
        host = ViewGroupHost(this@fxScope)
        block()
    }

/**
 * 局部浮窗：挂在 Activity 的 android.R.id.content 上。
 * API 29+ 在该 Activity destroy 时自动 cancel；API < 29 只能靠 window 卸下时 lost，
 * 建议调用方在 onDestroy 里 cancel（不 cancel 也不会泄漏：host 只被 Activity 自己的 view 树引用）。
 */
@JvmSynthetic
public fun Activity.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl {
    val control = findViewById<ViewGroup>(android.R.id.content).fxScope(tag, block)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        registerActivityLifecycleCallbacks(CancelOnDestroy(control))
    }
    return control
}

/** Activity 级生命周期回调（API 29+），destroy 时 cancel 并自我注销 */
private class CancelOnDestroy(private val control: FxControl) : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) activity.unregisterActivityLifecycleCallbacks(this)
        if (control.state != FxState.CANCELLED) control.cancel()
    }
}
```

注意 `Activity.registerActivityLifecycleCallbacks` 是 API 29 的方法，调用点已有 `SDK_INT >= Q` 守卫；Lint 若仍报 `NewApi`，给 `CancelOnDestroy.onActivityDestroyed` 里的 unregister 加 `@RequiresApi(Build.VERSION_CODES.Q)` 到整个类而不是压制警告。

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-scope:testDebugUnitTest`
Expected: 全绿。`below api 29` 用例在 Robolectric sdk 28 下若 `destroy()` 没有触发 window detach 而 state 停在 SHOWN，按 Task 3 Step 4 的同一处理（测试里显式 `removeViewImmediate`）。

- [ ] **Step 5: Commit**

```bash
git add floatingx-scope/src
git commit -m "feat(scope): Activity/ViewGroup.fxScope 与 viewGroupHost DSL"
```

---

### Task 5: `FragmentHost` + `Fragment.fxScope`

**Files:**
- Create: `floatingx-scope/src/main/kotlin/com/petterp/floatingx/scope/FragmentHost.kt`
- Create: `floatingx-scope/src/main/kotlin/com/petterp/floatingx/scope/FxFragmentScope.kt`
- Test: `floatingx-scope/src/test/kotlin/com/petterp/floatingx/scope/FragmentHostTest.kt`

**Interfaces:**
- Consumes: `androidx.fragment.app.Fragment`（`viewLifecycleOwnerLiveData`、`view`、`context`、`lifecycle`）、`androidx.lifecycle.DefaultLifecycleObserver` / `LifecycleOwner` / `Observer`（fragment 的传递依赖，compileOnly 可见）。
- Produces: `public class FragmentHost(fragment: Fragment) : FxHost`；`@JvmSynthetic fun FxInstallScope.fragmentHost(fragment): FragmentHost`；`@JvmSynthetic fun Fragment.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl`。

**行为约定（修 #244）：**
- `bind()`：`fragment.viewLifecycleOwnerLiveData.observe(fragment, observer)`。LiveData 在 fragment ≥ STARTED 时投递：view 已创建则在 `onStart` 前后拿到非空 owner → `root = fragment.view as ViewGroup` → 给 view lifecycle 加 observer（`onDestroy` → lost）→ `onHostReady()`；view 尚未创建时等待创建。fragment 自身 DESTROYED 时 LiveData 自动移除 observer。
- root 不是 `ViewGroup` → `IllegalStateException("Fragment 根 view 必须是 ViewGroup 才能承载浮窗")`（开发期错误，直接抛）。
- `context`：`fragment.context`，为 null 时抛 `IllegalStateException("Fragment 尚未 attach，请在 onCreate/onViewCreated 之后调用 fxScope")`。
- `Fragment.fxScope`：创建 control 后给 `fragment.lifecycle` 加 `DefaultLifecycleObserver`，`onDestroy` 时 cancel（并移除自己）。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.scope

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FragmentHostTest {

    /** 在 onCreate（view 尚不存在）就调用 fxScope，复现 #244 */
    class EarlyFragment : Fragment() {
        lateinit var control: FxControl
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            control = fxScope(tag = "frag") { view { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } } }
            control.show()
        }
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FrameLayout(requireContext())
    }

    /** 根 view 不是 ViewGroup */
    class PlainViewFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = View(requireContext())
    }

    private fun activity(): FragmentActivity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()

    @Test
    fun `fxScope called before the view exists attaches once the view is created`() {
        val activity = activity()
        val fragment = EarlyFragment()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commitNow()
        assertEquals(FxState.SHOWN, fragment.control.state)
        val root = fragment.requireView() as ViewGroup
        assertEquals(1, root.childCount)
    }

    @Test
    fun `view destroy loses host and view recreate readies again on the new root`() {
        val activity = activity()
        val fragment = EarlyFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(android.R.id.content, fragment).commitNow()
        val firstRoot = fragment.requireView()

        fm.beginTransaction().detach(fragment).commitNow()   // 只销毁 view，fragment 存活
        assertEquals(FxState.INSTALLED, fragment.control.state)

        fm.beginTransaction().attach(fragment).commitNow()   // 重新创建 view
        assertEquals(FxState.SHOWN, fragment.control.state)
        val secondRoot = fragment.requireView() as ViewGroup
        assertNotSame(firstRoot, secondRoot)
        assertEquals(1, secondRoot.childCount)
        assertSame(secondRoot, (fragment.control.contentView!!.parent as View).parent)
    }

    @Test
    fun `fragment destroy cancels the control`() {
        val activity = activity()
        val fragment = EarlyFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(android.R.id.content, fragment).commitNow()
        fm.beginTransaction().remove(fragment).commitNow()
        assertEquals(FxState.CANCELLED, fragment.control.state)
    }

    @Test
    fun `non view group root is rejected`() {
        val activity = activity()
        val fragment = PlainViewFragment()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commitNow()
        assertThrows(IllegalStateException::class.java) {
            fragment.fxScope { view { ctx -> View(ctx) } }
        }
    }

    @Test
    fun `fxScope before attach is rejected with a clear message`() {
        val ex = assertThrows(IllegalStateException::class.java) {
            EarlyFragment().fxScope { view { ctx -> View(ctx) } }
        }
        assertEquals(true, ex.message!!.contains("尚未 attach"))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-scope:testDebugUnitTest --tests '*FragmentHostTest*'`
Expected: 编译失败，`Unresolved reference: fxScope`（Fragment 接收者的重载不存在）。

- [ ] **Step 3: 实现 FragmentHost.kt**

```kotlin
package com.petterp.floatingx.scope

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxRect

/**
 * Fragment 承载（spec §5，修 #244）：跟随 viewLifecycleOwner——
 * view 创建后 ready、view 销毁即 lost；fragment 自己销毁时由 Fragment.fxScope 负责 cancel。
 * 使用方需自行依赖 androidx.fragment（本模块只 compileOnly）。
 */
public class FragmentHost(private val fragment: Fragment) : FxHost {

    override val context: Context
        get() = checkNotNull(fragment.context) { "Fragment 尚未 attach，请在 onCreate/onViewCreated 之后调用 fxScope" }

    private var session: FxHostSession? = null
    private var root: ViewGroup? = null

    private val viewOwnerObserver = Observer<LifecycleOwner?> { owner ->
        if (owner == null) onViewLost() else onViewCreated(owner)
    }

    private val viewLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            owner.lifecycle.removeObserver(this)
            onViewLost()
        }
    }

    override fun bind(session: FxHostSession) {
        this.session = session
        // 以 fragment 为 owner：fragment DESTROYED 时自动移除，不会泄漏 host
        fragment.viewLifecycleOwnerLiveData.observe(fragment, viewOwnerObserver)
    }

    private fun onViewCreated(owner: LifecycleOwner) {
        if (root != null) return
        val view = fragment.view
        root = checkNotNull(view as? ViewGroup) { "Fragment 根 view 必须是 ViewGroup 才能承载浮窗，当前为 ${view?.javaClass?.name}" }
        owner.lifecycle.addObserver(viewLifecycleObserver)
        session?.onHostReady()
    }

    private fun onViewLost() {
        if (root == null) return
        root = null
        session?.onHostLost()
    }

    override fun createContainer(): FxContainer = FxLayerContainer(context)

    override fun attach(container: FxContainer) {
        val r = checkNotNull(root) { "FragmentHost 尚未 ready 就被 attach" }
        r.addView(container.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    override fun detach(container: FxContainer) {
        (container.view.parent as? ViewGroup)?.removeView(container.view)
    }

    override fun bounds(): FxBounds {
        val r = root ?: return FxBounds(FxRect(0f, 0f, 0f, 0f))
        return FxBounds(FxRect(0f, 0f, r.width.toFloat(), r.height.toFloat()))
    }

    override fun release() {
        fragment.viewLifecycleOwnerLiveData.removeObserver(viewOwnerObserver)
        root = null
        session = null
    }
}
```

`FxFragmentScope.kt`：

```kotlin
package com.petterp.floatingx.scope

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfigScope
import com.petterp.floatingx.core.config.FxInstallScope

/** DSL：`FloatingX.create { fragmentHost(fragment); layout(...) }` */
@JvmSynthetic
public fun FxInstallScope.fragmentHost(fragment: Fragment): FragmentHost =
    FragmentHost(fragment).also { host = it }

/**
 * 局部浮窗：挂在 Fragment 的根 view 上，view 创建后才显示（#244），fragment destroy 时自动 cancel。
 * 须在 onCreate 或之后调用（需要 fragment.context）。
 */
@JvmSynthetic
public fun Fragment.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl {
    val control = FloatingX.create(tag) {
        host = FragmentHost(this@fxScope)
        block()
    }
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            owner.lifecycle.removeObserver(this)
            if (control.state != FxState.CANCELLED) control.cancel()
        }
    })
    return control
}
```

`non view group root is rejected` 用例的路径：`fxScope` → `FloatingX.create` → `FxControlImpl.init` → `host.bind` → LiveData 立即投递（fragment 已 RESUMED）→ `onViewCreated` → `checkNotNull` 抛出。异常从 `create` 冒出，符合测试预期。

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-scope:testDebugUnitTest`
Expected: 全绿。如果 Robolectric 抱怨 `FragmentActivity` 需要 AppCompat 主题——它不需要（只有 `AppCompatActivity` 才需要）；若报 `androidx.activity` 类缺失，说明 `testImplementation(libs.fragment)` 的传递依赖没进测试 classpath，检查 Task 1 的 build 文件而不是加新依赖。

- [ ] **Step 5: Commit**

```bash
git add floatingx-scope/src
git commit -m "feat(scope): FragmentHost 与 Fragment.fxScope——view 创建后挂载、destroy 自动 cancel（#244）"
```

---

### Task 6: `AppActivityFilter` + `ActivityRules`（黑白名单、父类匹配、自定义过滤）

**Files:**
- Create: `floatingx-app/src/main/kotlin/com/petterp/floatingx/app/AppActivityFilter.kt`
- Create: `floatingx-app/src/main/kotlin/com/petterp/floatingx/app/internal/ActivityRules.kt`
- Test: `floatingx-app/src/test/kotlin/com/petterp/floatingx/app/ActivityRulesTest.kt`

**Interfaces:**
- Produces:
  - `public fun interface AppActivityFilter { public fun accept(activity: Activity): Boolean }`
  - `internal class ActivityRules(blackClasses: List<Class<out Activity>>, blackNames: Set<String>, whiteClasses: List<Class<out Activity>>, whiteNames: Set<String>, filters: List<AppActivityFilter>)` + `fun accept(activity: Activity): Boolean` + `companion object { val ACCEPT_ALL: ActivityRules }`。

**判定顺序：** 白名单非空且不匹配 → 拒；黑名单匹配 → 拒；任一自定义过滤返回 false → 拒；否则接受。`Class` 条目用 `isInstance`（覆盖子类，#221），`String` 条目与 `activity.javaClass.name` 精确相等。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.app

import android.app.Activity
import com.petterp.floatingx.app.internal.ActivityRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActivityRulesTest {

    open class BaseActivity : Activity()
    class SubActivity : BaseActivity()
    class OtherActivity : Activity()

    private fun <T : Activity> make(cls: Class<T>): T = Robolectric.buildActivity(cls).get()

    private fun rules(
        blackClasses: List<Class<out Activity>> = emptyList(),
        blackNames: Set<String> = emptySet(),
        whiteClasses: List<Class<out Activity>> = emptyList(),
        whiteNames: Set<String> = emptySet(),
        filters: List<AppActivityFilter> = emptyList(),
    ) = ActivityRules(blackClasses, blackNames, whiteClasses, whiteNames, filters)

    @Test
    fun `accept all by default`() {
        assertTrue(ActivityRules.ACCEPT_ALL.accept(make(OtherActivity::class.java)))
    }

    @Test
    fun `blacklist by class also matches subclasses`() {
        val r = rules(blackClasses = listOf(BaseActivity::class.java))
        assertFalse(r.accept(make(BaseActivity::class.java)))
        assertFalse(r.accept(make(SubActivity::class.java)))
        assertTrue(r.accept(make(OtherActivity::class.java)))
    }

    @Test
    fun `blacklist by name is exact`() {
        val r = rules(blackNames = setOf(BaseActivity::class.java.name))
        assertFalse(r.accept(make(BaseActivity::class.java)))
        assertTrue(r.accept(make(SubActivity::class.java)))
    }

    @Test
    fun `whitelist rejects everything not listed`() {
        val r = rules(whiteClasses = listOf(BaseActivity::class.java))
        assertTrue(r.accept(make(SubActivity::class.java)))
        assertFalse(r.accept(make(OtherActivity::class.java)))
    }

    @Test
    fun `blacklist wins over whitelist`() {
        val r = rules(whiteClasses = listOf(BaseActivity::class.java), blackClasses = listOf(SubActivity::class.java))
        assertTrue(r.accept(make(BaseActivity::class.java)))
        assertFalse(r.accept(make(SubActivity::class.java)))
    }

    @Test
    fun `custom filters must all accept`() {
        val r = rules(filters = listOf(AppActivityFilter { true }, AppActivityFilter { it !is OtherActivity }))
        assertTrue(r.accept(make(BaseActivity::class.java)))
        assertFalse(r.accept(make(OtherActivity::class.java)))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-app:testDebugUnitTest --tests '*ActivityRulesTest*'`
Expected: 编译失败，`Unresolved reference: ActivityRules`。

- [ ] **Step 3: 实现**

`AppActivityFilter.kt`：

```kotlin
package com.petterp.floatingx.app

import android.app.Activity

/** 自定义 Activity 过滤（#221：父类、包名、任意规则）。返回 true 表示允许浮窗出现在该 Activity 上 */
public fun interface AppActivityFilter {
    public fun accept(activity: Activity): Boolean
}
```

`internal/ActivityRules.kt`：

```kotlin
package com.petterp.floatingx.app.internal

import android.app.Activity
import com.petterp.floatingx.app.AppActivityFilter

/**
 * AppHost 的 Activity 过滤规则。判定顺序：白名单（非空时必须命中）→ 黑名单 → 自定义过滤。
 * Class 条目用 isInstance 匹配（含子类，#221），String 条目与类全名精确相等。
 */
internal class ActivityRules(
    private val blackClasses: List<Class<out Activity>>,
    private val blackNames: Set<String>,
    private val whiteClasses: List<Class<out Activity>>,
    private val whiteNames: Set<String>,
    private val filters: List<AppActivityFilter>,
) {

    fun accept(activity: Activity): Boolean {
        val name = activity.javaClass.name
        val hasWhitelist = whiteClasses.isNotEmpty() || whiteNames.isNotEmpty()
        if (hasWhitelist && name !in whiteNames && whiteClasses.none { it.isInstance(activity) }) return false
        if (name in blackNames || blackClasses.any { it.isInstance(activity) }) return false
        for (f in filters) if (!f.accept(activity)) return false
        return true
    }

    companion object {
        val ACCEPT_ALL = ActivityRules(emptyList(), emptySet(), emptyList(), emptySet(), emptyList())
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-app:testDebugUnitTest`
Expected: 全绿。若 Robolectric 因测试内定义的 Activity 子类未在 manifest 声明而抛异常（信息含 `not found in AndroidManifest`），改用 `Robolectric.setupActivity` 之外的方式无济于事——此时在 `floatingx-app/src/test/AndroidManifest.xml` **不会**被合并；正确的兜底是把这几个测试 Activity 声明到 `floatingx-app/src/debug/AndroidManifest.xml`（只进 debug 变体，release AAR 不含），并在 release 单测上用 `@Config(manifest = ...)` 忽略——先确认 Robolectric 4.16 是否真的拒绝（它对未声明 Activity 一般只打 warning）。

- [ ] **Step 5: Commit**

```bash
git add floatingx-app/src
git commit -m "feat(app): Activity 过滤规则——黑白名单、父类匹配、自定义过滤（#221）"
```

---

### Task 7: `AppHost` + `Builder`

**Files:**
- Create: `floatingx-app/src/main/kotlin/com/petterp/floatingx/app/AppHost.kt`
- Test: `floatingx-app/src/test/kotlin/com/petterp/floatingx/app/AppHostTest.kt`
- Test: `floatingx-app/src/test/kotlin/com/petterp/floatingx/app/AppHostInsetsTest.kt`

**Interfaces:**
- Consumes: `FxActivityTracker.init/addObserver/removeObserver/topActivity` + `FxActivityTracker.Observer`；`ActivityRules`（Task 6）；`AppAttachTarget`（Task 1）；`ViewCompat.getRootWindowInsets`、`WindowInsetsCompat.Type`、`androidx.core.graphics.Insets`。
- Produces:
  - `public class AppHost private constructor(...) : FxHost, FxActivityTracker.Observer` 带 `public val application: Application`、`public val attachedActivity: Activity?`、`public val target: AppAttachTarget`、`public fun accepts(activity: Activity): Boolean`。
  - `public class Builder(application: Application)`：`blacklist(vararg classes: Class<out Activity>)`、`blacklist(vararg classNames: String)`、`whitelist(vararg classes: Class<out Activity>)`、`whitelist(vararg classNames: String)`、`filter(filter: AppActivityFilter)`、`attachTo(target: AppAttachTarget)`、`theme(@StyleRes themeRes: Int)`、`build(): AppHost`；`companion object { @JvmStatic fun builder(application: Application): Builder }`。
  - `internal companion fun contentInsets(bars: Insets, target: AppAttachTarget, offsetX: Int, offsetY: Int, parentWidth: Int, parentHeight: Int, rootWidth: Int, rootHeight: Int): FxInsets`（纯函数，供 insets 测试）。

**行为约定（spec §3 + 裁决）：**
1. `bind(session)`：`FxActivityTracker.init(application)`，`addObserver(this)`；若 `topActivity` 非空则 `moveTo(it)`。
2. `moveTo(activity)`：
   - 已 released 或 `activity === attachedActivity` → 返回。
   - `!accepts(activity)` → 若当前有挂载 → `lose()`；返回（spec：被过滤的页面浮窗不显示，而不是留在旧页）。
   - 否则 `newParent = parentOf(activity)`；若容器已在旧父上（`container != null && oldParent != null`）→ **静默换父**：旧父移除布局监听与容器，新父 `addView(MATCH_PARENT)` + 加布局监听，`session.onBoundsChanged()`；否则（首次 / lost 之后）→ `session.onHostReady()`。
   - 静默换父不经过 engine：状态、feature、动画、监听器都不重来；位置由 translation 保留，新父第一次布局时 `parentLayoutListener` → `onBoundsChanged` → core 按锚点校正（insets 不同的页面也能对齐）。
3. `onActivityPostResumed`（API 29+）→ `moveTo`；`onActivityResumed`（API < 29）→ 主线程 `Handler.post { if (!released && !activity.isDestroyed) moveTo(activity) }`（spec 原文写的 `decorView.post`：DecorView 在 onResume 时通常尚未 attach 到 window，`View.post` 会推迟到首次 traversal，时机不可控，改用 Handler；Task 8 同步 spec）；API 29+ 的 `onActivityResumed` 不处理。
4. `onActivityDestroyed(activity)`：`activity === attachedActivity` → `lose()`：清 `attachedActivity/parent`、移除布局监听、`session.onHostLost()`（engine 随后调 `detach`）。其它 Activity 的 destroy 忽略。
5. `attach(container)`：加到 `parent`，记录 `container`，给 parent 加 `OnLayoutChangeListener`（每次父布局都 `session.onBoundsChanged()`——父布局很少发生，core 的 relayout 幂等且无 requestLayout，不会成环）。`detach(container)`：从 `container.view.parent` 移除、移除监听、清 `container`。
6. `bounds()`：`parent` 为 null → `FxBounds(FxRect(0,0,0,0))`；否则 rect = parent 尺寸，insets = `contentInsets(systemBars ∪ displayCutout, …)`：DECOR 直接用；CONTENT 扣掉 parent 相对窗口的偏移（非 edge-to-edge 时 content 已被系统栏挤开）。
7. `release()`：`released = true`，`removeObserver`，移除监听，清引用。
8. `context`：默认 `application`；`Builder.theme(res)` 时为 `ContextThemeWrapper(application, res)`（内容 view 在 install 时用 host.context 创建，Material 组件需要主题）。

- [ ] **Step 1: 写失败测试**

`AppHostInsetsTest.kt`（纯函数）：

```kotlin
package com.petterp.floatingx.app

import androidx.core.graphics.Insets
import com.petterp.floatingx.core.layout.FxInsets
import org.junit.Assert.assertEquals
import org.junit.Test

class AppHostInsetsTest {

    private val bars = Insets.of(0, 63, 0, 126)

    @Test
    fun `decor target uses system bar insets as is`() {
        val r = AppHost.contentInsets(bars, AppAttachTarget.DECOR, offsetX = 0, offsetY = 0, parentWidth = 1080, parentHeight = 1920, rootWidth = 1080, rootHeight = 1920)
        assertEquals(FxInsets(0f, 63f, 0f, 126f), r)
    }

    @Test
    fun `content target subtracts the offset the system bars already pushed it by`() {
        // content 已被状态栏挤下 63、被导航栏挤上 126：剩余 insets 应为 0
        val r = AppHost.contentInsets(bars, AppAttachTarget.CONTENT, offsetX = 0, offsetY = 63, parentWidth = 1080, parentHeight = 1731, rootWidth = 1080, rootHeight = 1920)
        assertEquals(FxInsets.NONE, r)
    }

    @Test
    fun `content target keeps insets when edge to edge`() {
        val r = AppHost.contentInsets(bars, AppAttachTarget.CONTENT, offsetX = 0, offsetY = 0, parentWidth = 1080, parentHeight = 1920, rootWidth = 1080, rootHeight = 1920)
        assertEquals(FxInsets(0f, 63f, 0f, 126f), r)
    }

    @Test
    fun `content target never goes negative`() {
        val r = AppHost.contentInsets(bars, AppAttachTarget.CONTENT, offsetX = 0, offsetY = 100, parentWidth = 1080, parentHeight = 1600, rootWidth = 1080, rootHeight = 1920)
        assertEquals(FxInsets.NONE, r)
    }
}
```

`AppHostTest.kt`：

```kotlin
package com.petterp.floatingx.app

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.layout.FxGravity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AppHostTest {

    class BlackActivity : Activity()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val controllers = mutableListOf<ActivityController<out Activity>>()

    private class Counting : FxListener {
        var attach = 0
        var detach = 0
        override fun onAttach(control: FxControl) { attach++ }
        override fun onDetach(control: FxControl) { detach++ }
    }

    private fun content(): FxContent = FxContent.provider { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }

    private fun config(): FxConfig = FxConfig.builder(content()).anchor(FxGravity.TOP_START).build()

    /** 走完 resume + postResume：API 29+ 的挂载时机 */
    private fun <T : Activity> launch(cls: Class<T>): ActivityController<T> =
        Robolectric.buildActivity(cls).create().start().resume().postResume().visible().also { controllers += it }

    private fun decor(a: Activity): ViewGroup = a.window.decorView as ViewGroup

    private fun layoutDecor(a: Activity, w: Int = 1080, h: Int = 1920) {
        decor(a).measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
        decor(a).layout(0, 0, w, h)
    }

    private fun layerParent(control: FxControl): ViewGroup? = control.contentView?.parent?.parent as? ViewGroup

    private fun install(host: AppHost = AppHost.builder(app).build(), config: FxConfig = config()): FxControl =
        FloatingX.install("app-test", config, host)

    @After
    fun tearDown() {
        FloatingX.uninstallAll()
        controllers.forEach { runCatching { it.pause().stop().destroy() } }
        controllers.clear()
    }

    @Test
    fun `binds to the current top activity`() {
        val a = launch(Activity::class.java).get()
        val control = install()
        control.show()
        assertEquals(FxState.SHOWN, control.state)
        assertSame(decor(a), layerParent(control))
        assertSame(a, (control.host as AppHost).attachedActivity)
    }

    @Test
    fun `installed before any activity attaches on first post resume`() {
        val control = install()
        control.show()
        assertEquals(FxState.INSTALLED, control.state)
        val a = launch(Activity::class.java).get()
        assertEquals(FxState.SHOWN, control.state)
        assertSame(decor(a), layerParent(control))
    }

    @Test
    fun `re-parents silently when another activity resumes`() {
        launch(Activity::class.java)
        val control = install()
        val counting = Counting().also(control::addListener)
        control.show()
        val b = launch(Activity::class.java).get()
        assertSame(decor(b), layerParent(control))
        assertEquals(FxState.SHOWN, control.state)
        assertEquals(1, counting.attach)
        assertEquals(0, counting.detach)
        assertSame(b, (control.host as AppHost).attachedActivity)
    }

    @Test
    fun `follows back navigation and ignores destroy of a non attached activity`() {
        val ctrlA = launch(Activity::class.java)
        val control = install()
        val counting = Counting().also(control::addListener)
        control.show()
        val ctrlB = launch(Activity::class.java)
        ctrlA.resume().postResume()          // 返回 A
        ctrlB.pause().stop().destroy()       // B 之后才销毁
        controllers.remove(ctrlB)
        assertSame(decor(ctrlA.get()), layerParent(control))
        assertEquals(1, counting.attach)
        assertEquals(0, counting.detach)
    }

    @Test
    fun `destroying the attached activity detaches until the next resume`() {
        val ctrlA = launch(Activity::class.java)
        val control = install()
        val counting = Counting().also(control::addListener)
        control.show()
        val ctrlB = launch(Activity::class.java)
        ctrlB.pause().stop().destroy()       // 当前挂载的 B 先销毁
        controllers.remove(ctrlB)
        assertEquals(FxState.INSTALLED, control.state)
        assertNull(layerParent(control))
        assertNull((control.host as AppHost).attachedActivity)
        ctrlA.resume().postResume()
        assertEquals(FxState.SHOWN, control.state)
        assertSame(decor(ctrlA.get()), layerParent(control))
        assertEquals(2, counting.attach)
        assertEquals(1, counting.detach)
    }

    @Test
    fun `blacklisted activity detaches and coming back restores`() {
        val ctrlA = launch(Activity::class.java)
        val control = install(AppHost.builder(app).blacklist(BlackActivity::class.java).build())
        control.show()
        launch(BlackActivity::class.java)
        assertEquals(FxState.INSTALLED, control.state)
        assertNull(layerParent(control))
        ctrlA.resume().postResume()
        assertEquals(FxState.SHOWN, control.state)
        assertSame(decor(ctrlA.get()), layerParent(control))
    }

    @Test
    fun `installing on a blacklisted top activity stays installed`() {
        launch(BlackActivity::class.java)
        val control = install(AppHost.builder(app).blacklist(BlackActivity::class.java.name).build())
        control.show()
        assertEquals(FxState.INSTALLED, control.state)
    }

    @Test
    fun `position survives re-parenting`() {
        val a = launch(Activity::class.java).get()
        val control = install()
        control.show()
        layoutDecor(a)
        control.moveTo(300f, 400f, animate = false)
        assertEquals(300f, control.position.x, 0f)
        assertEquals(400f, control.position.y, 0f)
        val b = launch(Activity::class.java).get()
        layoutDecor(b)
        assertSame(decor(b), layerParent(control))
        assertEquals(300f, control.position.x, 0f)
        assertEquals(400f, control.position.y, 0f)
    }

    @Test
    fun `bounds follow the decor size`() {
        val a = launch(Activity::class.java).get()
        val control = install()
        layoutDecor(a, 720, 1280)
        val b = control.host.bounds()
        assertEquals(720f, b.rect.width, 0f)
        assertEquals(1280f, b.rect.height, 0f)
    }

    @Test
    fun `content target mounts on android R id content`() {
        val a = launch(Activity::class.java).get()
        val control = install(AppHost.builder(app).attachTo(AppAttachTarget.CONTENT).build())
        control.show()
        assertSame(a.findViewById<ViewGroup>(android.R.id.content), layerParent(control))
    }

    @Test
    @Config(sdk = [28])
    fun `below api 29 mounts after the resume post runs`() {
        val control = install()
        control.show()
        val ctrl = Robolectric.buildActivity(Activity::class.java).create().start().resume().visible().also { controllers += it }
        shadowOf(Looper.getMainLooper()).idle()                 // 执行 onActivityResumed 排的 post
        assertEquals(FxState.SHOWN, control.state)
        assertSame(decor(ctrl.get()), layerParent(control))
    }

    @Test
    fun `cancel releases the tracker observer`() {
        launch(Activity::class.java)
        val control = install()
        val host = control.host as AppHost
        control.cancel()
        launch(Activity::class.java)
        assertNull(host.attachedActivity)
    }

    @Test
    fun `theme wraps the application context`() {
        val host = AppHost.builder(app).theme(android.R.style.Theme_Material_Light).build()
        assertEquals(true, host.context is android.view.ContextThemeWrapper)
        assertSame(app, AppHost.builder(app).build().context)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-app:testDebugUnitTest --tests '*AppHost*'`
Expected: 编译失败，`Unresolved reference: AppHost`。

- [ ] **Step 3: 实现 AppHost.kt**

```kotlin
package com.petterp.floatingx.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.petterp.floatingx.app.internal.ActivityRules
import com.petterp.floatingx.core.FxActivityTracker
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxInsets
import com.petterp.floatingx.core.layout.FxRect

/**
 * App 级 host（spec §3）：跟随前台 Activity，把同一个 Layer 容器挂到当前 Activity 的 DecorView（或 content）上。
 *
 * - 换页：API 29+ 在 onActivityPostResumed、API < 29 在 onActivityResumed 后主线程 post 里
 *   把容器从旧父**静默**挪到新父——engine 状态、feature、动画不重来，位置由 translation 保留，
 *   新父首次布局时按锚点校正（insets 不同的页面也对齐）。
 * - 被过滤（黑白名单 / 自定义规则）的 Activity：卸下容器（浮窗不显示），回到允许的页面再挂上。
 * - 当前挂载的 Activity 销毁 → onHostLost；其它 Activity 销毁忽略。
 * - bounds()：父容器尺寸 + 系统栏 / 刘海 insets。
 */
public class AppHost private constructor(
    public val application: Application,
    override val context: Context,
    private val rules: ActivityRules,
    public val target: AppAttachTarget,
) : FxHost, FxActivityTracker.Observer {

    /** 容器当前挂在哪个 Activity 上；未挂载为 null */
    public var attachedActivity: Activity? = null
        private set

    private var session: FxHostSession? = null
    private var parent: ViewGroup? = null
    private var container: FxContainer? = null
    private var released = false
    private val tmpLocation = IntArray(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 父容器每次布局（首帧、旋转、insets 变化）都让 core 按锚点重算；relayout 只写 translation，不会成环 */
    private val parentLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> session?.onBoundsChanged() }

    public fun accepts(activity: Activity): Boolean = rules.accept(activity)

    // ---------- FxHost ----------

    override fun bind(session: FxHostSession) {
        this.session = session
        FxActivityTracker.init(application)
        FxActivityTracker.addObserver(this)
        FxActivityTracker.topActivity?.let { moveTo(it) }
    }

    override fun createContainer(): FxContainer = FxLayerContainer(context)

    override fun attach(container: FxContainer) {
        val p = checkNotNull(parent) { "AppHost 尚未 ready 就被 attach" }
        this.container = container
        p.addView(container.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        p.addOnLayoutChangeListener(parentLayoutListener)
    }

    override fun detach(container: FxContainer) {
        (container.view.parent as? ViewGroup)?.let {
            it.removeOnLayoutChangeListener(parentLayoutListener)
            it.removeView(container.view)
        }
        this.container = null
    }

    override fun bounds(): FxBounds {
        val p = parent ?: return FxBounds(FxRect(0f, 0f, 0f, 0f))
        val rect = FxRect(0f, 0f, p.width.toFloat(), p.height.toFloat())
        val windowInsets = ViewCompat.getRootWindowInsets(p) ?: return FxBounds(rect)
        val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        p.getLocationInWindow(tmpLocation)
        val root = p.rootView
        return FxBounds(rect, contentInsets(bars, target, tmpLocation[0], tmpLocation[1], p.width, p.height, root.width, root.height))
    }

    override fun release() {
        released = true
        FxActivityTracker.removeObserver(this)
        parent?.removeOnLayoutChangeListener(parentLayoutListener)
        parent = null
        attachedActivity = null
        container = null
        session = null
    }

    // ---------- FxActivityTracker.Observer ----------

    override fun onActivityResumed(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return   // 29+ 走 onActivityPostResumed
        // 低版本没有 postResumed：排到当前 onResume 派发之后再挂载（不用 decorView.post：此时 decor 未必已 attach）
        mainHandler.post { if (!released && !activity.isDestroyed) moveTo(activity) }
    }

    override fun onActivityPostResumed(activity: Activity) {
        moveTo(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity === attachedActivity) lose()
    }

    // ---------- internal ----------

    private fun moveTo(activity: Activity) {
        if (released || activity === attachedActivity) return
        if (!rules.accept(activity)) {
            if (attachedActivity != null) lose()
            return
        }
        val newParent = parentOf(activity)
        val oldParent = parent
        val c = container
        attachedActivity = activity
        parent = newParent
        if (c != null && oldParent != null) {
            oldParent.removeOnLayoutChangeListener(parentLayoutListener)
            oldParent.removeView(c.view)
            newParent.addView(c.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            newParent.addOnLayoutChangeListener(parentLayoutListener)
            session?.onBoundsChanged()
        } else {
            session?.onHostReady()
        }
    }

    private fun lose() {
        parent?.removeOnLayoutChangeListener(parentLayoutListener)
        attachedActivity = null
        parent = null
        session?.onHostLost()
    }

    private fun parentOf(activity: Activity): ViewGroup = when (target) {
        AppAttachTarget.DECOR -> activity.window.decorView as ViewGroup
        AppAttachTarget.CONTENT -> activity.findViewById(android.R.id.content)
    }

    // ---------- Builder ----------

    public class Builder(private val application: Application) {
        private val blackClasses = mutableListOf<Class<out Activity>>()
        private val blackNames = mutableSetOf<String>()
        private val whiteClasses = mutableListOf<Class<out Activity>>()
        private val whiteNames = mutableSetOf<String>()
        private val filters = mutableListOf<AppActivityFilter>()
        private var target = AppAttachTarget.DECOR
        private var context: Context = application

        /** 这些 Activity（含子类）上不显示浮窗 */
        public fun blacklist(vararg classes: Class<out Activity>): Builder = apply { blackClasses += classes }

        /** 按类全名精确匹配的黑名单 */
        public fun blacklist(vararg classNames: String): Builder = apply { blackNames += classNames }

        /** 只在这些 Activity（含子类）上显示浮窗 */
        public fun whitelist(vararg classes: Class<out Activity>): Builder = apply { whiteClasses += classes }

        public fun whitelist(vararg classNames: String): Builder = apply { whiteNames += classNames }

        /** 自定义规则（#221），可多次调用，全部通过才显示 */
        public fun filter(filter: AppActivityFilter): Builder = apply { filters += filter }

        public fun attachTo(target: AppAttachTarget): Builder = apply { this.target = target }

        /** 内容 view 用 Application context 创建；需要主题属性（Material 组件等）时在这里指定 */
        public fun theme(@StyleRes themeRes: Int): Builder = apply { context = ContextThemeWrapper(application, themeRes) }

        public fun build(): AppHost = AppHost(
            application,
            context,
            ActivityRules(blackClasses.toList(), blackNames.toSet(), whiteClasses.toList(), whiteNames.toSet(), filters.toList()),
            target,
        )
    }

    public companion object {
        @JvmStatic
        public fun builder(application: Application): Builder = Builder(application)

        /**
         * 把窗口的系统栏 insets 换算到父容器坐标系：
         * DECOR 直接用；CONTENT 扣掉父容器已经被系统栏挤开的部分（非 edge-to-edge 时结果为 0），不为负。
         */
        internal fun contentInsets(
            bars: Insets,
            target: AppAttachTarget,
            offsetX: Int,
            offsetY: Int,
            parentWidth: Int,
            parentHeight: Int,
            rootWidth: Int,
            rootHeight: Int,
        ): FxInsets {
            if (target == AppAttachTarget.DECOR) {
                return FxInsets(bars.left.toFloat(), bars.top.toFloat(), bars.right.toFloat(), bars.bottom.toFloat())
            }
            val left = (bars.left - offsetX).coerceAtLeast(0)
            val top = (bars.top - offsetY).coerceAtLeast(0)
            val right = (bars.right - (rootWidth - offsetX - parentWidth)).coerceAtLeast(0)
            val bottom = (bars.bottom - (rootHeight - offsetY - parentHeight)).coerceAtLeast(0)
            return FxInsets(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        }
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-app:testDebugUnitTest`
Expected: 全绿。常见失败与处理：
- `re-parents silently` 里 `layerParent` 为 null：`FxLayerContainer.contentView.parent` 是容器，`.parent` 才是 decor，检查测试 helper 而不是实现。
- API 28 用例里 `post` 在 `resume()` 内就执行了：Robolectric PAUSED looper 不会，如果发生说明测试用了 `LEGACY` 模式——不要改模式，改断言为 idle 后的状态。
- `position survives re-parenting` 位置变了：说明换父后 `onBoundsChanged` 触发的 relayout 用了旧锚点以外的东西；`moveTo(300,400,false)` 会 `commitAnchor`，relayout 应回到同一坐标——若不等，先打印 `control.anchor` 判断是 core 还是 host 的问题，再决定修哪边（core 问题记 ledger，不在本 Task 修）。

- [ ] **Step 5: Commit**

```bash
git add floatingx-app/src
git commit -m "feat(app): AppHost——前台 Activity 跟踪、静默换父、过滤规则、DecorView/Content 挂载与 insets"
```

---

### Task 8: `appHost` DSL、`attachedActivity` 扩展、Java API 测试、文档同步

**Files:**
- Create: `floatingx-app/src/main/kotlin/com/petterp/floatingx/app/FxAppExt.kt`
- Test: `floatingx-app/src/test/kotlin/com/petterp/floatingx/app/JavaAppApiTest.java`
- Test: 在 `AppHostTest.kt` 追加 2 个用例
- Modify: `docs/superpowers/specs/2026-08-29-floatingx-3-modular-architecture-design.md` §3、§5
- Modify: `CLAUDE.md`（仓库根）

**Interfaces:**
- Produces: `@JvmSynthetic public fun FxInstallScope.appHost(application: Application, block: AppHost.Builder.() -> Unit = {}): AppHost`（创建并设置 `host`）；`public val FxControl.attachedActivity: Activity?`（非 AppHost 时为 null）。

- [ ] **Step 1: 写失败测试**

`AppHostTest.kt` 追加：

```kotlin
    @Test
    fun `appHost dsl installs with blacklist`() {
        val ctrlA = launch(Activity::class.java)
        val control = FloatingX.install("dsl") {
            view { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }
            appHost(app) { blacklist(BlackActivity::class.java) }
        }
        control.show()
        assertSame(ctrlA.get(), control.attachedActivity)
        launch(BlackActivity::class.java)
        assertNull(control.attachedActivity)
    }

    @Test
    fun `attachedActivity is null for non app hosts`() {
        val parent = android.widget.FrameLayout(app)
        val host = object : com.petterp.floatingx.core.host.FxHost {
            override val context get() = app
            override fun bind(session: com.petterp.floatingx.core.host.FxHostSession) = session.onHostReady()
            override fun createContainer() = com.petterp.floatingx.core.container.FxLayerContainer(app)
            override fun attach(container: com.petterp.floatingx.core.container.FxContainer) { parent.addView(container.view) }
            override fun detach(container: com.petterp.floatingx.core.container.FxContainer) { parent.removeView(container.view) }
            override fun bounds() = com.petterp.floatingx.core.layout.FxBounds(com.petterp.floatingx.core.layout.FxRect(0f, 0f, 100f, 100f))
            override fun release() {}
        }
        val control = FloatingX.create(config(), host)
        assertNull(control.attachedActivity)
        control.cancel()
    }
```

`JavaAppApiTest.java`：

```java
package com.petterp.floatingx.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.petterp.floatingx.core.FloatingX;
import com.petterp.floatingx.core.FxControl;
import com.petterp.floatingx.core.FxState;
import com.petterp.floatingx.core.config.FxConfig;
import com.petterp.floatingx.core.config.FxContent;
import com.petterp.floatingx.core.layout.FxGravity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** spec §7 的 Java 样例必须能编译并工作 */
@RunWith(RobolectricTestRunner.class)
public class JavaAppApiTest {

    @After
    public void tearDown() {
        FloatingX.uninstallAll();
    }

    @Test
    public void builderChainAndInstall() {
        Application app = ApplicationProvider.getApplicationContext();
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().postResume().visible().get();
        FxConfig config = FxConfig.builder(FxContent.view(new View(app)))
                .anchor(FxGravity.CENTER_END, 0f, 120f)
                .modal()
                .build();
        AppHost host = AppHost.builder(app)
                .blacklist(AppHostTest.BlackActivity.class)
                .blacklist("com.example.SplashActivity")
                .whitelist(Activity.class)
                .filter(a -> !a.isFinishing())
                .attachTo(AppAttachTarget.DECOR)
                .build();
        FxControl control = FloatingX.install("java", config, host);
        control.show();
        assertEquals(FxState.SHOWN, control.getState());
        assertSame(activity, host.getAttachedActivity());
        assertSame(activity, FxAppExtKt.getAttachedActivity(control));
        assertTrue(host.accepts(activity));
        activity.finish();
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-app:testDebugUnitTest`
Expected: 编译失败，`Unresolved reference: appHost` / `attachedActivity`。

- [ ] **Step 3: 实现 FxAppExt.kt**

```kotlin
package com.petterp.floatingx.app

import android.app.Activity
import android.app.Application
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.config.FxInstallScope

/** DSL：`FloatingX.install("tag") { layout(...); appHost(app) { blacklist(SplashActivity::class.java) } }` */
@JvmSynthetic
public fun FxInstallScope.appHost(application: Application, block: AppHost.Builder.() -> Unit = {}): AppHost =
    AppHost.Builder(application).apply(block).build().also { host = it }

/** 浮窗当前挂在哪个 Activity 上；host 不是 AppHost 或尚未挂载时为 null（spec §3） */
public val FxControl.attachedActivity: Activity?
    get() = (host as? AppHost)?.attachedActivity
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-app:testDebugUnitTest`
Expected: 全绿。Java 测试里的 `FxAppExtKt.getAttachedActivity(control)` 依赖文件名 `FxAppExt.kt`——不要给文件加 `@file:JvmName`。

- [ ] **Step 5: 同步 spec 与 CLAUDE.md**

spec §3 的代码块与"行为"列表按本计划裁决修订（用 Edit 精确替换，不重写整节）：
- `AppHost.Builder` 去掉 `modal(...)` 一行，补 `fun theme(@StyleRes themeRes: Int)`；`filter(predicate: (Activity) -> Boolean)` 改为 `filter(filter: AppActivityFilter)   // fun interface，Kotlin 可传 lambda`；`attachTo(target: AttachTarget)` 改为 `attachTo(target: AppAttachTarget)   // DECOR（默认）| CONTENT`。
- DSL 行改为 `public fun FxInstallScope.appHost(app: Application, block: AppHost.Builder.() -> Unit = {}): AppHost   // 创建并设置 host`。
- "行为"第一条里的 `以下用 onActivityResumed 后 decorView.post` 改为 `以下用 onActivityResumed 后主线程 Handler.post`，并在该条末尾追加一句：`换页时同一容器从旧 DecorView 静默挪到新 DecorView，engine 状态 / feature / 动画不重来，位置由 translation 保留、新父首次布局后按锚点校正。`
- "行为"最后追加：`- modal（#212/#151）不在 host 上，而是 FxConfigScope.modal(enabled, dismissOnOutsideTouch) / FxConfig.Builder.modal(...)，对 app 与 scope 的 Layer 容器通用。`
- `bounds()` 一条改为：`bounds()：父容器尺寸 + ViewCompat.getRootWindowInsets 的 systemBars ∪ displayCutout insets；CONTENT 目标时扣掉父容器已被系统栏挤开的偏移。`

spec §5：
- 代码块改为：
  ```kotlin
  public class ViewGroupHost(viewGroup: ViewGroup) : FxHost            // Java：ViewGroupHost.of(viewGroup)
  public class FragmentHost(fragment: Fragment) : FxHost               // compileOnly androidx.fragment
  public fun Activity.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl    // R.id.content；API 29+ destroy 自动 cancel
  public fun ViewGroup.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl
  public fun Fragment.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl    // view 创建后挂载，fragment destroy 自动 cancel
  public fun FxInstallScope.viewGroupHost(viewGroup: ViewGroup): ViewGroupHost
  public fun FxInstallScope.fragmentHost(fragment: Fragment): FragmentHost
  ```
- 第一条行为改为：`ViewGroupHost 用 Layer 容器，bind 即 ready；viewGroup 从 window 卸下 → onHostLost，重新挂上 → onHostReady。FragmentHost 观察 viewLifecycleOwnerLiveData：view 创建后 ready、view 销毁即 lost（修 #244）。tag 为空则不做位置持久化。`

CLAUDE.md 的 "## 3.0 重构进行中" 段落：把"新模块 `floatingx-core`…已落地"改为列出三个新模块及其包名（core / scope / app），测试命令改为 `./gradlew :floatingx-core:test :floatingx-scope:test :floatingx-app:test`，并注明 Plan 2 的计划文件路径。

- [ ] **Step 6: 全量测试**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL，core 130、scope ≥ 19、app ≥ 22 个用例（debug + release 各跑一遍），旧模块占位测试照常。

- [ ] **Step 7: Commit**

```bash
git add floatingx-app/src docs/superpowers/specs CLAUDE.md
git commit -m "feat(app): appHost DSL 与 attachedActivity 扩展；docs: spec §3/§5 同步 Plan 2 裁决"
```

---

## 自查记录

- **Spec 覆盖：** §3 全部行为（跟踪、re-parent 时机、过滤即 detach、destroy 判定、attachedActivity、bounds insets）→ Task 6/7/8；§3 `modal` → Task 2（裁决迁移）；§5 三个入口 + Java `of` + 不进注册表 + Fragment 等 view → Task 3/4/5；§7 Java 样例 → Task 4/8 的 Java 测试；§8 性能（无 post 兜底、换父无分配）→ Task 7 实现；§10 "AppHost：A→B→back 顺序、B 先 destroy、黑名单 detach、父类过滤"与"ViewGroupHost / Fragment：attach 时机、detach 自动 cancel" → Task 6/7/3/5 的用例。§10 的"旋转（demo manifest 去掉 configChanges）"留给 Plan 5 的 demo/instrumentation。
- **占位扫描：** 无 TBD/TODO；每个代码步骤都有完整代码。
- **类型一致性：** `ViewGroupHost.viewGroup`（public val，Task 3/4 测试读取）；`AppHost.attachedActivity/target/application/accepts`（Task 7 定义，Task 8 使用）；`AppHost.contentInsets` 签名在 Task 7 的 Interfaces 与实现一致；`AppActivityFilter.accept`（Task 6）在 Task 7 Builder 与 Task 8 Java 测试一致；`FxInstallScope.host` 是 core 已有的 `var host: FxHost?`。
