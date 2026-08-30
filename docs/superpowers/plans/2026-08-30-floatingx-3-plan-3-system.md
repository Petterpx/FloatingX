# FloatingX 3.0 Plan 3：floatingx-system 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地 `floatingx-system`：`SystemHost`（WindowManager 窗口、`LayoutParams` 默认值 + 用户覆写、悬浮窗权限 Auto/Manual/Skip 三种策略、被拒后 `requestSwap` 降级、`retryPermission`）、`FxWindowContainer`（wrap_content 窗口容器，锚点 gravity 映射到 `LayoutParams.gravity`，避免内容尺寸变化跳动 #187）、`FxPermission` + 透明 `FxPermissionActivity`（任意 context 申请 #192）、`KeyboardFeature`（系统窗口内 EditText 唤起键盘）、返回键监听；并给 core 补两处 Window 容器需要的能力：`FxHost.hostFeatures()` 与手势检测的窗口无关坐标。

**Architecture:** system host 与 app/scope 的差别只在容器形态：Layer 容器是 match_parent 覆盖层、内容用 translation 定位；Window 容器本身就是一个 wrap_content 的 `WindowManager` 窗口，"移动内容"= 改 `LayoutParams.x/y` + `updateViewLayout`。core 的 `LocationFeature` 通过 `host.updateLayout(container, FxLayoutSpec)` 提交布局，`SystemHost` 覆写它把 `spec.anchor.gravity` 映射为 `LayoutParams.gravity` 并把左上角坐标换算成相对该 gravity 边的偏移，内容尺寸变化时 WindowManager 自己保持锚定边不动。权限与键盘等"只有 host 才知道"的行为通过新增的 `FxHost.hostFeatures()` 以 feature 形式挂进 control。

**Tech Stack:** Kotlin 2.2.21、AGP 8.13.2、JDK 17、compileSdk 36、minSdk 21、androidx.core 1.13.1（`WindowInsetsCompat`）、JUnit 4.13.2、Robolectric 4.16.1（sdk=35；`ShadowSettings.setCanDrawOverlays`、`ShadowWindowManagerImpl`、`ShadowActivity.receiveResult`）。

**Spec:** `docs/superpowers/specs/2026-08-29-floatingx-3-modular-architecture-design.md`（§4 由本计划实现；§2.1/§2.4/§2.5 有两处小扩展，见 Task 2/3 的裁决）。Plan 1/2 的遗留项与裁决见各自 plan 文件末尾。

## Global Constraints

- 仓库工具链：AGP `8.13.2`、Gradle `8.14.3`、Kotlin `2.2.21`、JDK 17；库模块用 `build-logic` 的 `floatingx.library` convention plugin（不改）。
- 依赖边界（spec §1）：`floatingx-system` 只允许 `api(project(":floatingx-core"))` + `implementation(androidx.core:core:1.13.1)` + `kotlin-stdlib`；`src/main` 禁止 import `androidx.fragment`、`androidx.lifecycle`、`androidx.compose`、`androidx.appcompat`、`androidx.savedstate`、`kotlinx.coroutines`（Task 1 的边界测试守住）。`android.view.WindowManager` 在本模块**允许**（这是它存在的意义）；core 仍禁止。
- 包名 `com.petterp.floatingx.system`；`namespace` 同名；不得使用 core 的 `internal` API。
- Java 互操作（spec §7）：`SystemHost.builder(context)` / `SystemHost.Builder` 是 Java 入口；接受回调的 Builder 方法用 `fun interface`（`SystemLayoutParamsCustomizer`、`SystemBackListener`、`FxPermissionInterceptor`）；DSL 扩展（`systemHost`）标 `@JvmSynthetic`；`FxPermissionStrategy` 给 Java 提供 `auto()/manual(i)/skip()` 静态工厂。
- 性能（spec §8）：拖动 MOVE 路径无分配（`LayoutParams` 复用、`updateViewLayout` 直接传同一对象）；host/容器不得用 `post/postDelayed` 定位——唯一允许的 `post` 是 `KeyboardFeature` 里等窗口变为可聚焦后 `showSoftInput`（不是定位）。
- 默认 `WindowManager.LayoutParams`（spec §4 + Task 6 裁决）：`type` = O+ `TYPE_APPLICATION_OVERLAY` 否则 `TYPE_PHONE`；`flags` = `FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL or FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS`；`format = PixelFormat.TRANSLUCENT`；`width/height = WRAP_CONTENT`；`gravity` 由锚点映射。用户 customizer 最后执行，可覆盖任何字段。
- 注释、KDoc、日志、提交信息用中文；Conventional Commits 前缀。
- 每个 Task 结束时 `./gradlew :floatingx-core:testDebugUnitTest :floatingx-system:testDebugUnitTest` 必须全绿（Task 2/3 改 core，须额外确认 core 130 个用例不回归；scope/app 的测试在最后一个 Task 用 `./gradlew test` 一并跑）。
- Robolectric sdk=35；Java 测试放 `src/test/java/`，Kotlin 测试放 `src/test/kotlin/`。

---

## 文件结构（本计划新增 / 修改）

```
settings.gradle                                    include ':floatingx-system'
floatingx-core/src/main/kotlin/com/petterp/floatingx/core/
  host/FxHost.kt                                   + hostFeatures(): List<FxFeature>（默认空）
  internal/FxControlImpl.kt                        init/swapHost 接入 hostFeatures
  gesture/FxGestureDetector.kt                     拖动/slop 用窗口无关坐标（rawX 偏移）
floatingx-core/src/test/kotlin/com/petterp/floatingx/core/
  FloatingXEndToEndTest.kt                         + hostFeatures 两个用例（TestHost 加 features 参数）
  TestHost.kt                                      + features 构造参数
  gesture/FxGestureDetectorTest.kt                 + 窗口随手指移动用例

floatingx-system/
  build.gradle.kts / .gitignore / consumer-rules.pro
  src/main/AndroidManifest.xml                     FxPermissionActivity
  src/test/resources/robolectric.properties
  src/main/kotlin/com/petterp/floatingx/system/
    SystemLayoutParamsCustomizer.kt                fun interface
    SystemBackListener.kt                          fun interface
    permission/FxPermission.kt                     isGranted / request / 回调分发
    permission/FxPermissionActivity.kt             透明 Activity，startActivityForResult 系统设置页
    permission/FxPermissionStrategy.kt             Auto / Manual / Skip + FxPermissionInterceptor + FxPermissionRequest
    container/WindowLayoutMath.kt                  锚点 gravity ↔ LayoutParams 的纯换算（internal）
    container/FxWindowContainer.kt                 Window 容器
    feature/KeyboardFeature.kt                     EditText 唤起键盘（host feature）
    feature/SystemWindowFeature.kt                 touchable → FLAG_NOT_TOUCHABLE（internal host feature）
    SystemHost.kt                                  host + Builder
    FxSystemExt.kt                                 FxInstallScope.systemHost DSL
  src/test/kotlin/com/petterp/floatingx/system/
    DependencyBoundaryTest.kt
    WindowLayoutMathTest.kt
    FxWindowContainerTest.kt
    FxPermissionTest.kt
    SystemHostTest.kt
    KeyboardFeatureTest.kt
  src/test/java/com/petterp/floatingx/system/JavaSystemApiTest.java

docs/superpowers/specs/…design.md                  §2.1/§2.4/§4 同步裁决
CLAUDE.md                                          模块表 + 测试命令
```

---

### Task 1: 模块骨架、manifest 与依赖边界测试

**Files:**
- Modify: `settings.gradle`（`include ':floatingx-app'` 之后追加 `include ':floatingx-system'`）
- Create: `floatingx-system/build.gradle.kts`、`.gitignore`、`consumer-rules.pro`、`src/test/resources/robolectric.properties`、`src/main/AndroidManifest.xml`
- Create: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/SystemLayoutParamsCustomizer.kt`、`SystemBackListener.kt`
- Test: `floatingx-system/src/test/kotlin/com/petterp/floatingx/system/DependencyBoundaryTest.kt`

**Interfaces:**
- Produces: `public fun interface SystemLayoutParamsCustomizer { public fun customize(lp: WindowManager.LayoutParams) }`；`public fun interface SystemBackListener { public fun onBackPressed(): Boolean }`；manifest 里的 `FxPermissionActivity` 声明（类在 Task 5 创建——manifest 引用尚不存在的类在 library 模块只在 lint 报错，`assemble/test` 不检查；Task 5 之前不要跑 `lint`）。

- [ ] **Step 1: settings.gradle**

```groovy
include ':floatingx-system'
```

- [ ] **Step 2: 构建文件**

`floatingx-system/build.gradle.kts`：

```kotlin
plugins {
    id("floatingx.library")
}

android {
    namespace = "com.petterp.floatingx.system"
}

dependencies {
    api(project(":floatingx-core"))
    // WindowInsetsCompat / ViewCompat：系统窗口的 safe area
    implementation(libs.androidx.core)
    implementation(libs.kotlin.stdlib)
}
```

`.gitignore`（`/build`、`/.kotlin`）、`consumer-rules.pro`（`# FloatingX system：无需额外混淆规则`）、`robolectric.properties`（`sdk=35`）与 scope/app 模块相同。

`src/main/AndroidManifest.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <application>
        <!-- 透明的权限申请页：从任意 context 启动（Service 可用，#192），不进最近任务、无历史 -->
        <activity
            android:name="com.petterp.floatingx.system.permission.FxPermissionActivity"
            android:excludeFromRecents="true"
            android:exported="false"
            android:noHistory="true"
            android:taskAffinity=""
            android:theme="@android:style/Theme.Translucent.NoTitleBar" />
    </application>
</manifest>
```

- [ ] **Step 3: 两个 fun interface**

`SystemLayoutParamsCustomizer.kt`：

```kotlin
package com.petterp.floatingx.system

import android.view.WindowManager

/** 在默认 LayoutParams 之后执行，可覆盖任何字段（type / flags / softInputMode…，#194/#220/#241/#235/#155/#211） */
public fun interface SystemLayoutParamsCustomizer {
    public fun customize(lp: WindowManager.LayoutParams)
}
```

`SystemBackListener.kt`：

```kotlin
package com.petterp.floatingx.system

/** 系统窗口收到返回键（仅窗口可聚焦时会收到，如 KeyboardFeature 唤起键盘期间）。返回 true 表示已消费 */
public fun interface SystemBackListener {
    public fun onBackPressed(): Boolean
}
```

- [ ] **Step 4: 依赖边界测试**

```kotlin
package com.petterp.floatingx.system

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** system 模块只依赖 core + androidx.core（spec §1）；WindowManager 在本模块允许 */
class DependencyBoundaryTest {

    private val forbidden = listOf(
        "androidx.fragment",
        "androidx.lifecycle",
        "androidx.compose",
        "androidx.appcompat",
        "androidx.savedstate",
        "kotlinx.coroutines",
    )

    @Test
    fun `system main sources import no forbidden packages`() {
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
        assertTrue("system 违反依赖边界：\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
```

- [ ] **Step 5: 验证与提交**

Run: `./gradlew :floatingx-system:testDebugUnitTest`
Expected: BUILD SUCCESSFUL，1 个用例通过。

```bash
git add settings.gradle floatingx-system
git commit -m "build: 新增 floatingx-system 模块骨架、manifest 与依赖边界测试"
```

---

### Task 2: core `FxHost.hostFeatures()`

**Files:**
- Modify: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/host/FxHost.kt`
- Modify: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/internal/FxControlImpl.kt`
- Modify: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/TestHost.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/FloatingXEndToEndTest.kt`

**Interfaces:**
- Produces: `FxHost.hostFeatures(): List<FxFeature>`（默认 `emptyList()`；实现方必须每次返回同一批实例）。`FxControlImpl`：构造时在 `config.features` 之后加入 `host.hostFeatures()`；`swapHost` 时先移除旧 host 的、再加入新 host 的。

**裁决（spec §2.1/§2.5 扩展）：** 只有 host 知道的行为（键盘唤起要改窗口 flag、`touchable` 要映射成 `FLAG_NOT_TOUCHABLE`）需要访问 config + container，这正是 feature 的能力；给 host 一个提供 feature 的口子比在 core 里加 host 专属钩子干净。`modal` 保持在配置层不变。

- [ ] **Step 1: 写失败测试**

`TestHost.kt` 构造器加参数 `private val features: List<FxFeature> = emptyList()` 并 `override fun hostFeatures(): List<FxFeature> = features`（补 import `com.petterp.floatingx.core.feature.FxFeature`）。

`FloatingXEndToEndTest.kt` 追加（复用文件里已有的 `LifecycleFeature`、`parent()`/`content()` 之类 helper；若 helper 名字不同以文件为准）：

```kotlin
    @Test
    fun `host features attach with the control and detach on host loss`() {
        val f = LifecycleFeature()
        val host = TestHost(parent(), features = listOf(f))
        val control = FloatingX.create(FxConfig.builder(content()).build(), host)
        assertEquals(listOf("attach"), f.calls)
        host.lose()
        assertEquals(listOf("attach", "detach"), f.calls)
        control.cancel()
        assertEquals(listOf("attach", "detach", "cancel"), f.calls)
    }

    @Test
    fun `swapHost drops the old host features and adds the new ones`() {
        val old = LifecycleFeature()
        val new = LifecycleFeature()
        val first = TestHost(parent(), features = listOf(old))
        val second = TestHost(parent(), features = listOf(new))
        val control = FloatingX.create(FxConfig.builder(content()).build(), first)
        control.show()
        first.session!!.requestSwap(second)
        assertEquals(listOf("attach", "detach"), old.calls)
        assertEquals(listOf("attach"), new.calls)
        control.cancel()
        assertEquals(listOf("attach", "detach"), old.calls)   // 旧 host 的 feature 不再收到 cancel
        assertEquals(listOf("attach", "detach", "cancel"), new.calls)
    }
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests '*FloatingXEndToEndTest*'`
Expected: 编译失败（`hostFeatures` 不存在）。

- [ ] **Step 3: 实现**

`FxHost.kt` 在 `release()` 之前加：

```kotlin
    /**
     * host 自带的行为插件（如系统窗口的键盘适配、touchable→窗口 flag 映射）。
     * control 创建时与 config.features 一起挂入，换 host 时随之替换。必须每次返回同一批实例。
     */
    public fun hostFeatures(): List<FxFeature> = emptyList()
```

（补 import `com.petterp.floatingx.core.feature.FxFeature`。）

`FxControlImpl.kt`：
- `init` 里 `features += initialConfig.features` 之后加一行 `features += initialHost.hostFeatures()`。
- `swapHost` 改为：

```kotlin
    override fun swapHost(fallback: FxHost) {
        logger?.d { "[$tag] 切换 host: ${host::class.java.simpleName} -> ${fallback::class.java.simpleName}" }
        host.hostFeatures().forEach { removeFeature(it) }
        host.release()
        // 先把内容从旧容器摘下来：否则旧容器的 layout 监听会一直挂在内容 view 上（泄漏旧容器）
        container.releaseContent()
        host = fallback
        container = fallback.createContainer()
        contentView?.let { container.setContent(it) }
        fallback.hostFeatures().forEach { addFeature(it) }
        fallback.bind(engine)
    }
```

`removeFeature`/`addFeature` 有 `main()` 检查，swap 在主线程发生，无需改动。`update(config)` 里的集合差只针对 `config.features`，host feature 不在其中，不会被误删。

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest`
Expected: 全绿（130 + 2）。

- [ ] **Step 5: Commit**

```bash
git add floatingx-core/src
git commit -m "feat(core): FxHost.hostFeatures()——host 自带 feature 随 control 挂载、换 host 时替换"
```

---

### Task 3: core 手势检测改用窗口无关坐标

**Files:**
- Modify: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/gesture/FxGestureDetector.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/gesture/FxGestureDetectorTest.kt`

**问题：** 检测器用 `ev.getX()`（相对容器）算 slop 与拖动增量。Layer 容器不动所以没问题；Window 容器本身随手指移动，相对坐标几乎不变 → 永远起不了拖、增量为 0。`rawX - x` 是该事件所在窗口的屏幕偏移（对所有 pointer 相同），用它把每个 pointer 的坐标换算成屏幕坐标即可，Layer 容器下等价。`canDragFrom / hasScrollableChildAt` 仍用相对坐标（它们判断的是落点在内容的哪里）。

- [ ] **Step 1: 写失败测试**

在 `FxGestureDetectorTest.kt` 里追加（沿用该文件已有的 detector / callback / `down()`/`move()`/`up()` helper；若 helper 只接受 x/y，则本用例自行构造事件——下面给出完整写法，不依赖 helper）：

```kotlin
    /** 模拟窗口随手指移动：每个 MOVE 的相对坐标不变，只有 raw 坐标在走 */
    @Test
    fun `drag deltas follow raw coordinates when the window moves under the finger`() {
        val calls = mutableListOf<String>()
        val detector = FxGestureDetector(touchSlop = 8f, defaultLongPressTimeout = 500L, callback = object : FxGestureDetector.Callback {
            override fun onClick() { calls += "click" }
            override fun onLongPress() { calls += "longPress" }
            override fun onDragStart() { calls += "dragStart" }
            override fun onDrag(dx: Float, dy: Float) { calls += "drag:${dx.toInt()},${dy.toInt()}" }
            override fun onDragEnd() { calls += "dragEnd" }
            override fun canDragFrom(x: Float, y: Float): Boolean { calls += "canDrag:${x.toInt()},${y.toInt()}"; return true }
            override fun hasScrollableChildAt(x: Float, y: Float) = false
        })
        fun event(action: Int, rawX: Float, rawY: Float, windowX: Float, windowY: Float): MotionEvent =
            MotionEvent.obtain(0L, 0L, action, rawX, rawY, 0).apply { offsetLocation(-windowX, -windowY) }   // getX = raw - window

        // 手指按在窗口 (100,100) 内的 (10,10)
        detector.onTouch(event(MotionEvent.ACTION_DOWN, 110f, 110f, 100f, 100f))
        // 手指移到 (140,110)，窗口已经跟着挪到 (130,100)：相对坐标仍是 (10,10)
        detector.onTouch(event(MotionEvent.ACTION_MOVE, 140f, 110f, 130f, 100f))
        detector.onTouch(event(MotionEvent.ACTION_MOVE, 160f, 130f, 150f, 120f))
        detector.onTouch(event(MotionEvent.ACTION_UP, 160f, 130f, 150f, 120f))

        assertEquals(listOf("canDrag:10,10", "dragStart", "drag:30,0", "drag:20,20", "dragEnd"), calls)
    }
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests '*FxGestureDetectorTest*'`
Expected: 该用例失败（没有 dragStart/drag，因为相对坐标没动）。

- [ ] **Step 3: 实现**

`FxGestureDetector.kt` 改动点（其余不变）：

```kotlin
    /** 当前事件的窗口屏幕偏移：rawX - x 对同一事件的所有 pointer 相同。窗口随手指移动时（Window 容器）相对坐标不可用 */
    private fun absX(ev: MotionEvent, idx: Int): Float = ev.getX(idx) + (ev.rawX - ev.x)
    private fun absY(ev: MotionEvent, idx: Int): Float = ev.getY(idx) + (ev.rawY - ev.y)
```

- `onIntercept` 的 MOVE 分支：`val x = absX(ev, idx); val y = absY(ev, idx)`。
- `onTouch` 的 MOVE 分支同上；`ACTION_POINTER_UP` 分支：`lastX = absX(ev, newIdx); lastY = absY(ev, newIdx)`。
- `begin(ev)`：

```kotlin
    private fun begin(ev: MotionEvent) {
        reset()
        pointerId = ev.getPointerId(0)
        downX = ev.rawX
        downY = ev.rawY
        lastX = downX
        lastY = downY
        // 落点判断用相对坐标：dragRegion / 可滚动子 view 都是内容坐标系
        canDrag = config.drag != FxDrag.DISABLED && callback.canDragFrom(ev.x, ev.y)
        childScrollable = callback.hasScrollableChildAt(ev.x, ev.y)
        if (config.longPress || config.drag == FxDrag.AFTER_LONG_PRESS) {
            val timeout = if (config.longPressTimeout > 0) config.longPressTimeout else defaultLongPressTimeout
            handler.postDelayed(longPressRunnable, timeout)
        }
    }
```

类 KDoc 补一句："slop 与拖动增量按屏幕坐标计算（rawX 偏移），落点判断按容器相对坐标"。

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest`
Expected: 全绿（既有 19 个手势用例不变——Robolectric 的 `MotionEvent.obtain(x, y)` raw 与 x 相同，Layer 语义等价）。

- [ ] **Step 5: Commit**

```bash
git add floatingx-core/src
git commit -m "fix(core): 手势 slop 与拖动增量改用屏幕坐标，Window 容器随手指移动时可正常拖动"
```

---

### Task 4: `WindowLayoutMath` + `FxWindowContainer`

**Files:**
- Create: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/container/WindowLayoutMath.kt`
- Create: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/container/FxWindowContainer.kt`
- Test: `floatingx-system/src/test/kotlin/com/petterp/floatingx/system/WindowLayoutMathTest.kt`
- Test: `floatingx-system/src/test/kotlin/com/petterp/floatingx/system/FxWindowContainerTest.kt`

**Interfaces:**
- Consumes: `FxContainer`（core，含 `isLayer`、`releaseContent`、`touchHandler`、`onContentSizeChanged`、`onBoundsChanged`）、`FxGravity/FxHorizontal/FxVertical`、`FxPoint/FxSize/FxInsets`、`WindowInsetsCompat`。
- Produces:
  - `internal object WindowLayoutMath { fun apply(lp: WindowManager.LayoutParams, x: Float, y: Float, gravity: FxGravity, ltr: Boolean, boundsW: Int, boundsH: Int, contentW: Int, contentH: Int) }`：把"左上角在屏幕坐标系的 (x,y)"写成 `lp.gravity + lp.x/lp.y`。`boundsW/H` 或 `contentW/H` 为 0 时退化为 `TOP|LEFT` + 直接坐标。
  - `public class FxWindowContainer(context, wm: WindowManager, lp: WindowManager.LayoutParams, backListener: SystemBackListener?) : FrameLayout, FxContainer`：`isLayer=false`；`public val windowParams: WindowManager.LayoutParams`（同一对象，host 用它 `addView`；不叫 layoutParams 是为了避开 `View.layoutParams`）；`public fun applyLayout(x, y, gravity, ltr)`；`public fun setBounds(w, h)`；`public var isAttachedToWm: Boolean`（host 维护）；`public val windowInsets: FxInsets`（最近一次 `onApplyWindowInsets` 的 systemBars∪cutout）；`public fun setWindowFocusable(focusable: Boolean)`（切 `FLAG_NOT_FOCUSABLE` 并 `updateViewLayout`）；`public fun setWindowTouchable(touchable: Boolean)`（切 `FLAG_NOT_TOUCHABLE`）。

**行为约定：**
- 内容 child 用 `WRAP_CONTENT`，位于容器 (0,0)；容器本身就是窗口（wrap_content）。
- `applyLayout` 记录 `posX/posY`（左上角屏幕坐标，Float），通过 `WindowLayoutMath.apply` 写 lp，若已 attach 则 `wm.updateViewLayout(this, lp)`。`setContentPosition(x,y)` = `applyLayout(x, y, lastGravity, isLtr)`（默认 host 路径）。`contentPosition()` 与 `contentPositionOnScreen()` 都返回 `FxPoint(posX, posY)`（`FLAG_LAYOUT_IN_SCREEN` + `NO_LIMITS` 下 lp 坐标即屏幕坐标）。
- **首帧闪现防护：** 首次 `applyLayout` 之前容器 `visibility = GONE`（窗口不显示、不占触摸）；之后按 `setContentVisible` 请求的可见性显示。`setContentVisible(false)` → 内容 INVISIBLE + 容器 GONE（窗口收起，不挡触摸）；`true` → 内容 VISIBLE + 容器 VISIBLE（若已定位）。
- `hitTest(x,y)`：内容可见且 `0 ≤ x < contentW && 0 ≤ y < contentH`。
- 触摸：`onInterceptTouchEvent/onTouchEvent` 转发 `touchHandler`，与 `FxLayerContainer` 相同；无 modal 逻辑。
- `dispatchKeyEvent`：`KEYCODE_BACK` 的 `ACTION_UP` → `backListener?.onBackPressed()`，为 true 则消费。
- `onConfigurationChanged` → `onBoundsChanged?.invoke()`；`ViewCompat.setOnApplyWindowInsetsListener` 里更新 `windowInsets`，变化时 `onBoundsChanged?.invoke()`。
- 内容 `OnLayoutChangeListener` 尺寸变化 → `onContentSizeChanged`（与 Layer 相同）。

- [ ] **Step 1: 写失败测试**

`WindowLayoutMathTest.kt`（Robolectric 只是为了能 new `WindowManager.LayoutParams`）：

```kotlin
package com.petterp.floatingx.system

import android.view.Gravity
import android.view.WindowManager
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.system.container.WindowLayoutMath
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WindowLayoutMathTest {

    private fun lp() = WindowManager.LayoutParams()

    private fun apply(x: Float, y: Float, g: FxGravity, ltr: Boolean = true, bw: Int = 1080, bh: Int = 1920, cw: Int = 100, ch: Int = 50) =
        lp().also { WindowLayoutMath.apply(it, x, y, g, ltr, bw, bh, cw, ch) }

    @Test
    fun `top start keeps coordinates`() {
        val lp = apply(20f, 30f, FxGravity.TOP_START)
        assertEquals(Gravity.TOP or Gravity.LEFT, lp.gravity)
        assertEquals(20, lp.x); assertEquals(30, lp.y)
    }

    @Test
    fun `bottom end measures from the far edges`() {
        val lp = apply(1080f - 100f - 20f, 1920f - 50f - 30f, FxGravity.BOTTOM_END)
        assertEquals(Gravity.BOTTOM or Gravity.RIGHT, lp.gravity)
        assertEquals(20, lp.x); assertEquals(30, lp.y)
    }

    @Test
    fun `center measures from the center`() {
        val lp = apply((1080 - 100) / 2f + 5f, (1920 - 50) / 2f - 7f, FxGravity.CENTER)
        assertEquals(Gravity.CENTER, lp.gravity)
        assertEquals(5, lp.x); assertEquals(-7, lp.y)
    }

    @Test
    fun `rtl swaps start and end`() {
        val lp = apply(20f, 0f, FxGravity.TOP_END, ltr = false)
        assertEquals(Gravity.TOP or Gravity.LEFT, lp.gravity)
        assertEquals(20, lp.x)
    }

    @Test
    fun `unknown bounds fall back to top left`() {
        val lp = apply(300f, 400f, FxGravity.BOTTOM_END, bw = 0, bh = 0)
        assertEquals(Gravity.TOP or Gravity.LEFT, lp.gravity)
        assertEquals(300, lp.x); assertEquals(400, lp.y)
    }

    @Test
    fun `unknown content size falls back to top left`() {
        val lp = apply(300f, 400f, FxGravity.CENTER_END, cw = 0, ch = 0)
        assertEquals(Gravity.TOP or Gravity.LEFT, lp.gravity)
        assertEquals(300, lp.x); assertEquals(400, lp.y)
    }
}
```

`FxWindowContainerTest.kt`：

```kotlin
package com.petterp.floatingx.system

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.system.container.FxWindowContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FxWindowContainerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private fun lp() = WindowManager.LayoutParams().apply {
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
    }

    private fun container(back: SystemBackListener? = null): FxWindowContainer = FxWindowContainer(context, wm, lp(), back).also { c ->
        c.setContent(View(context).apply { layoutParams = ViewGroup.LayoutParams(100, 50) })
        c.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        c.layout(0, 0, c.measuredWidth, c.measuredHeight)
    }

    @Test
    fun `is not a layer and wraps content`() {
        val c = container()
        assertFalse(c.isLayer)
        assertEquals(100f, c.contentSize().width, 0f)
        assertEquals(50f, c.contentSize().height, 0f)
    }

    @Test
    fun `window stays gone until first layout is applied`() {
        val c = container()
        c.setContentVisible(true)
        assertEquals(View.GONE, c.visibility)
        c.setBounds(1080, 1920)
        c.applyLayout(10f, 20f, FxGravity.TOP_START, ltr = true)
        assertEquals(View.VISIBLE, c.visibility)
        c.setContentVisible(false)
        assertEquals(View.GONE, c.visibility)
        assertEquals(View.INVISIBLE, c.contentView!!.visibility)
    }

    @Test
    fun `applyLayout writes layout params and updates the window when attached`() {
        val c = container()
        c.setBounds(1080, 1920)
        wm.addView(c, c.windowParams)
        c.isAttachedToWm = true
        c.applyLayout(1080f - 100f - 20f, 30f, FxGravity.TOP_END, ltr = true)
        assertEquals(20, c.windowParams.x)
        assertEquals(30, c.windowParams.y)
        assertEquals(android.view.Gravity.TOP or android.view.Gravity.RIGHT, c.windowParams.gravity)
        assertEquals(960f, c.contentPosition().x, 0f)
        assertEquals(960f, c.contentPositionOnScreen().x, 0f)
        assertTrue(shadowOf(wm).views.contains(c))
    }

    @Test
    fun `setContentPosition keeps the last gravity`() {
        val c = container()
        c.setBounds(1080, 1920)
        c.applyLayout(0f, 0f, FxGravity.BOTTOM_END, ltr = true)
        c.setContentPosition(980f, 1870f)
        assertEquals(android.view.Gravity.BOTTOM or android.view.Gravity.RIGHT, c.windowParams.gravity)
        assertEquals(0, c.windowParams.x); assertEquals(0, c.windowParams.y)
    }

    @Test
    fun `hitTest is bounded by the content`() {
        val c = container()
        c.setBounds(1080, 1920)
        c.applyLayout(0f, 0f, FxGravity.TOP_START, ltr = true)
        c.setContentVisible(true)
        assertTrue(c.hitTest(50f, 25f))
        assertFalse(c.hitTest(150f, 25f))
        c.setContentVisible(false)
        assertFalse(c.hitTest(50f, 25f))
    }

    @Test
    fun `focusable and touchable toggles flip the window flags`() {
        val c = container()
        c.setWindowFocusable(true)
        assertEquals(0, c.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        c.setWindowFocusable(false)
        assertTrue(c.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        c.setWindowTouchable(false)
        assertTrue(c.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        c.setWindowTouchable(true)
        assertEquals(0, c.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    @Test
    fun `back key goes to the listener`() {
        var hits = 0
        val c = container(SystemBackListener { hits++; true })
        val consumed = c.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        assertTrue(consumed)
        assertEquals(1, hits)
    }

    @Test
    fun `releaseContent removes the content view`() {
        val c = container()
        c.releaseContent()
        assertEquals(0, c.childCount)
        assertEquals(null, c.contentView)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-system:testDebugUnitTest`
Expected: 编译失败（`WindowLayoutMath` / `FxWindowContainer` 不存在）。

- [ ] **Step 3: 实现**

`WindowLayoutMath.kt`：

```kotlin
package com.petterp.floatingx.system.container

import android.view.Gravity
import android.view.WindowManager
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxHorizontal
import com.petterp.floatingx.core.layout.FxVertical

/**
 * 把 core 的"内容左上角屏幕坐标 + 锚点 gravity"换算成 WindowManager.LayoutParams 的 gravity + 偏移（spec §2.3 / #187）。
 * 让 WindowManager 自己保持锚定边：内容尺寸变化时窗口从锚定边生长，不会先跳到旧坐标再被修正。
 * 屏幕尺寸或内容尺寸未知（0）时退化为 TOP|LEFT + 直接坐标。
 */
internal object WindowLayoutMath {

    fun apply(
        lp: WindowManager.LayoutParams,
        x: Float,
        y: Float,
        gravity: FxGravity,
        ltr: Boolean,
        boundsW: Int,
        boundsH: Int,
        contentW: Int,
        contentH: Int,
    ) {
        if (boundsW <= 0 || boundsH <= 0 || contentW <= 0 || contentH <= 0) {
            lp.gravity = Gravity.TOP or Gravity.LEFT
            lp.x = x.toInt()
            lp.y = y.toInt()
            return
        }
        val h = when (gravity.horizontal) {
            FxHorizontal.START -> if (ltr) Gravity.LEFT else Gravity.RIGHT
            FxHorizontal.END -> if (ltr) Gravity.RIGHT else Gravity.LEFT
            FxHorizontal.CENTER -> Gravity.CENTER_HORIZONTAL
        }
        val v = when (gravity.vertical) {
            FxVertical.TOP -> Gravity.TOP
            FxVertical.BOTTOM -> Gravity.BOTTOM
            FxVertical.CENTER -> Gravity.CENTER_VERTICAL
        }
        lp.gravity = h or v
        lp.x = when (h) {
            Gravity.LEFT -> x
            Gravity.RIGHT -> boundsW - x - contentW
            else -> x - (boundsW - contentW) / 2f
        }.toInt()
        lp.y = when (v) {
            Gravity.TOP -> y
            Gravity.BOTTOM -> boundsH - y - contentH
            else -> y - (boundsH - contentH) / 2f
        }.toInt()
    }
}
```

`FxWindowContainer.kt`：

```kotlin
package com.petterp.floatingx.system.container

import android.content.Context
import android.content.res.Configuration
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxContainerTouchHandler
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxInsets
import com.petterp.floatingx.core.layout.FxPoint
import com.petterp.floatingx.core.layout.FxSize
import com.petterp.floatingx.system.SystemBackListener

/**
 * Window 容器（spec §2.1 / §4）：容器本身就是一个 wrap_content 的 WindowManager 窗口，
 * "移动内容" = 改 LayoutParams.x/y + updateViewLayout。
 * - 首次 applyLayout 之前窗口 GONE（不闪现在 0,0，也不挡触摸）
 * - 隐藏 = 内容 INVISIBLE + 窗口 GONE（窗口收起后不再拦截其下的触摸）
 * - 锚点 gravity 映射到 LayoutParams.gravity，内容尺寸变化时锚定边不动（#187）
 */
public class FxWindowContainer(
    context: Context,
    private val wm: WindowManager,
    public val windowParams: WindowManager.LayoutParams,
    private val backListener: SystemBackListener?,
) : FrameLayout(context), FxContainer {

    override val view: ViewGroup get() = this
    override var contentView: View? = null
        private set
    override val isLtr: Boolean get() = layoutDirection != View.LAYOUT_DIRECTION_RTL
    override val isLayer: Boolean get() = false
    override var touchHandler: FxContainerTouchHandler? = null
    override var onContentSizeChanged: ((FxSize) -> Unit)? = null
    override var onBoundsChanged: (() -> Unit)? = null

    /** host 在 addView/removeView 时维护；只有挂在 WindowManager 上才能 updateViewLayout */
    public var isAttachedToWm: Boolean = false

    /** 最近一次 onApplyWindowInsets 的 systemBars ∪ displayCutout */
    public var windowInsets: FxInsets = FxInsets.NONE
        private set

    private var boundsW = 0
    private var boundsH = 0
    private var posX = 0f
    private var posY = 0f
    private var lastGravity = FxGravity.TOP_START
    private var positioned = false
    private var contentVisibleRequested = false
    private var lastW = 0
    private var lastH = 0

    private val contentLayoutListener = View.OnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
        val w = r - l
        val h = b - t
        if (w != lastW || h != lastH) {
            lastW = w
            lastH = h
            onContentSizeChanged?.invoke(FxSize(w.toFloat(), h.toFloat()))
        }
    }

    init {
        setWillNotDraw(true)
        isClickable = false
        isFocusable = false
        visibility = View.GONE
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val i = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val next = FxInsets(i.left.toFloat(), i.top.toFloat(), i.right.toFloat(), i.bottom.toFloat())
            if (next != windowInsets) {
                windowInsets = next
                onBoundsChanged?.invoke()
            }
            insets
        }
    }

    // ---------- host 侧 API ----------

    /** 屏幕尺寸，gravity 换算需要；host 在 bind/attach/bounds 变化时调用 */
    public fun setBounds(width: Int, height: Int) {
        boundsW = width
        boundsH = height
    }

    /** 提交一次布局：左上角屏幕坐标 + 锚点 gravity（SystemHost.updateLayout 调用） */
    public fun applyLayout(x: Float, y: Float, gravity: FxGravity, ltr: Boolean) {
        posX = x
        posY = y
        lastGravity = gravity
        val c = contentView
        WindowLayoutMath.apply(windowParams, x, y, gravity, ltr, boundsW, boundsH, c?.width ?: 0, c?.height ?: 0)
        if (!positioned) {
            positioned = true
            if (contentVisibleRequested) visibility = View.VISIBLE
        }
        if (isAttachedToWm) wm.updateViewLayout(this, windowParams)
    }

    /** 切换窗口是否可聚焦（KeyboardFeature 唤起键盘时需要焦点） */
    public fun setWindowFocusable(focusable: Boolean) {
        updateFlag(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, !focusable)
    }

    /** touchable=false → FLAG_NOT_TOUCHABLE，触摸全部透传给下层（spec §4） */
    public fun setWindowTouchable(touchable: Boolean) {
        updateFlag(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, !touchable)
    }

    private fun updateFlag(flag: Int, enabled: Boolean) {
        val next = if (enabled) windowParams.flags or flag else windowParams.flags and flag.inv()
        if (next == windowParams.flags) return
        windowParams.flags = next
        if (isAttachedToWm) wm.updateViewLayout(this, windowParams)
    }

    // ---------- FxContainer ----------

    override fun setContent(view: View) {
        releaseContent()
        (view.parent as? ViewGroup)?.removeView(view)
        val lp = (view.layoutParams as? LayoutParams)
            ?: LayoutParams(view.layoutParams?.width ?: LayoutParams.WRAP_CONTENT, view.layoutParams?.height ?: LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.TOP or Gravity.START
        addView(view, lp)
        view.addOnLayoutChangeListener(contentLayoutListener)
        contentView = view
    }

    override fun releaseContent() {
        val c = contentView ?: return
        c.removeOnLayoutChangeListener(contentLayoutListener)
        removeView(c)
        contentView = null
        lastW = 0
        lastH = 0
    }

    override fun contentSize(): FxSize = contentView?.let { FxSize(it.width.toFloat(), it.height.toFloat()) } ?: FxSize.EMPTY

    override fun setContentPosition(x: Float, y: Float) = applyLayout(x, y, lastGravity, isLtr)

    override fun contentPosition(): FxPoint = FxPoint(posX, posY)

    /** FLAG_LAYOUT_IN_SCREEN + FLAG_LAYOUT_NO_LIMITS 下 LayoutParams 坐标就是屏幕坐标 */
    override fun contentPositionOnScreen(): FxPoint = FxPoint(posX, posY)

    override fun setContentVisible(visible: Boolean) {
        contentVisibleRequested = visible
        contentView?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        visibility = if (visible && positioned) View.VISIBLE else View.GONE
    }

    override fun hitTest(x: Float, y: Float): Boolean {
        val c = contentView ?: return false
        if (c.visibility != View.VISIBLE) return false
        return x >= 0f && x < c.width && y >= 0f && y < c.height
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = touchHandler?.onIntercept(ev) ?: false

    override fun onTouchEvent(ev: MotionEvent): Boolean = touchHandler?.onTouch(ev) ?: false

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            if (backListener?.onBackPressed() == true) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        onBoundsChanged?.invoke()
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-system:testDebugUnitTest`
Expected: 全绿（1 + 6 + 8）。`shadowOf(wm).views` 需要 `import org.robolectric.Shadows.shadowOf`，`ShadowWindowManagerImpl.getViews()` 在 Robolectric 4.16 可用。

- [ ] **Step 5: Commit**

```bash
git add floatingx-system/src
git commit -m "feat(system): FxWindowContainer——wrap_content 窗口容器与锚点 gravity 到 LayoutParams 的映射（#187）"
```

---

### Task 5: `FxPermission` / `FxPermissionActivity` / 权限策略

**Files:**
- Create: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/permission/FxPermissionStrategy.kt`
- Create: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/permission/FxPermission.kt`
- Create: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/permission/FxPermissionActivity.kt`
- Test: `floatingx-system/src/test/kotlin/com/petterp/floatingx/system/FxPermissionTest.kt`

**Interfaces:**
- Produces:
  - `public fun interface FxPermissionCallback { public fun onResult(granted: Boolean) }`
  - `public sealed class FxPermissionStrategy { object Auto; class Manual(val interceptor: FxPermissionInterceptor); object Skip; companion { @JvmStatic auto(); manual(i); skip() } }`
  - `public fun interface FxPermissionInterceptor { public fun onRequest(request: FxPermissionRequest) }`
  - `public interface FxPermissionRequest { fun proceed(); fun deny(); fun useFallback() }`
  - `public object FxPermission { @JvmStatic fun isGranted(context): Boolean; @JvmStatic fun request(context, callback: FxPermissionCallback) }`；内部 `SparseArray<FxPermissionCallback>` 按 requestId 分发；`internal fun dispatch(id, granted)`。
  - `internal class FxPermissionActivity : Activity()`：`onCreate` 若无 savedInstanceState 则 `startActivityForResult(Intent(ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName"), REQUEST)`；`onActivityResult` → `FxPermission.dispatch(id, FxPermission.isGranted(this))` + `finish()`；启动设置页失败（`ActivityNotFoundException`）→ dispatch(false) + finish。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.system

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.system.permission.FxPermission
import com.petterp.floatingx.system.permission.FxPermissionActivity
import com.petterp.floatingx.system.permission.FxPermissionStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class FxPermissionTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `isGranted follows Settings canDrawOverlays`() {
        ShadowSettings.setCanDrawOverlays(false)
        assertFalse(FxPermission.isGranted(app))
        ShadowSettings.setCanDrawOverlays(true)
        assertTrue(FxPermission.isGranted(app))
    }

    @Test
    @Config(sdk = [22])
    fun `isGranted is always true below M`() {
        assertTrue(FxPermission.isGranted(app))
    }

    @Test
    fun `request already granted answers immediately without starting anything`() {
        ShadowSettings.setCanDrawOverlays(true)
        var result: Boolean? = null
        FxPermission.request(app) { result = it }
        assertEquals(true, result)
        assertEquals(null, shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `request launches the transparent activity from a non activity context`() {
        ShadowSettings.setCanDrawOverlays(false)
        FxPermission.request(app) { }
        val intent = shadowOf(app).nextStartedActivity
        assertNotNull(intent)
        assertEquals(FxPermissionActivity::class.java.name, intent!!.component!!.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `activity opens the overlay settings page and reports the result to the right callback`() {
        ShadowSettings.setCanDrawOverlays(false)
        val results = mutableListOf<Pair<String, Boolean>>()
        FxPermission.request(app) { results += "a" to it }
        val launch = shadowOf(app).nextStartedActivity!!
        FxPermission.request(app) { results += "b" to it }
        shadowOf(app).nextStartedActivity   // 消费掉第二个启动 intent

        val controller = Robolectric.buildActivity(FxPermissionActivity::class.java, launch).create()
        val activity = controller.get()
        val settings = shadowOf(activity).nextStartedActivityForResult
        assertNotNull(settings)
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, settings!!.intent.action)
        assertEquals("package:${app.packageName}", settings.intent.data.toString())

        ShadowSettings.setCanDrawOverlays(true)
        shadowOf(activity).receiveResult(settings.intent, Activity.RESULT_OK, null)
        assertEquals(listOf("a" to true), results)   // 只回调请求 a，b 仍在等自己的 Activity
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `strategy factories are java friendly`() {
        assertTrue(FxPermissionStrategy.auto() is FxPermissionStrategy.Auto)
        assertTrue(FxPermissionStrategy.skip() is FxPermissionStrategy.Skip)
        val manual = FxPermissionStrategy.manual { it.deny() }
        assertTrue(manual is FxPermissionStrategy.Manual)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-system:testDebugUnitTest --tests '*FxPermissionTest*'`
Expected: 编译失败。

- [ ] **Step 3: 实现**

`FxPermissionStrategy.kt`：

```kotlin
package com.petterp.floatingx.system.permission

/** 权限申请的三种策略（spec §4） */
public sealed class FxPermissionStrategy {

    /** 默认：无权限时自动弹透明页申请，拒绝后走 fallback（若有） */
    public object Auto : FxPermissionStrategy()

    /** 把决定权交给拦截器：由业务方决定何时 proceed / deny / useFallback */
    public class Manual(public val interceptor: FxPermissionInterceptor) : FxPermissionStrategy()

    /** 不检查权限直接挂窗口（业务方已自行申请，或 type 不需要权限） */
    public object Skip : FxPermissionStrategy()

    public companion object {
        @JvmStatic public fun auto(): FxPermissionStrategy = Auto
        @JvmStatic public fun manual(interceptor: FxPermissionInterceptor): FxPermissionStrategy = Manual(interceptor)
        @JvmStatic public fun skip(): FxPermissionStrategy = Skip
    }
}

public fun interface FxPermissionInterceptor {
    public fun onRequest(request: FxPermissionRequest)
}

/** Manual 策略下交给拦截器的句柄；三个方法只应调用一个 */
public interface FxPermissionRequest {
    /** 弹系统设置页申请 */
    public fun proceed()

    /** 放弃：状态停在 INSTALLED，之后可 SystemHost.retryPermission() */
    public fun deny()

    /** 直接降级到 Builder.fallback 指定的 host（未配置则等同 deny） */
    public fun useFallback()
}

public fun interface FxPermissionCallback {
    public fun onResult(granted: Boolean)
}
```

`FxPermission.kt`：

```kotlin
package com.petterp.floatingx.system.permission

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.SparseArray

/** 悬浮窗权限：检查 + 从任意 context 申请（#192）。回调按 requestId 分发，多次并发申请互不干扰 */
public object FxPermission {

    internal const val EXTRA_REQUEST_ID: String = "fx_request_id"

    private val callbacks = SparseArray<FxPermissionCallback>()
    private var nextId = 1

    @JvmStatic
    public fun isGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    /** 已有权限立即回调 true；否则启动透明页，结果在设置页返回后回调（主线程） */
    @JvmStatic
    public fun request(context: Context, callback: FxPermissionCallback) {
        if (isGranted(context)) {
            callback.onResult(true)
            return
        }
        val id = nextId++
        callbacks.put(id, callback)
        val intent = Intent(context, FxPermissionActivity::class.java)
            .putExtra(EXTRA_REQUEST_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    internal fun dispatch(id: Int, granted: Boolean) {
        val cb = callbacks.get(id) ?: return
        callbacks.remove(id)
        cb.onResult(granted)
    }
}
```

`FxPermissionActivity.kt`：

```kotlin
package com.petterp.floatingx.system.permission

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings

/** 透明页：只负责打开系统"显示在其他应用上层"设置并把结果交回 FxPermission。manifest 里 noHistory + excludeFromRecents */
internal class FxPermissionActivity : Activity() {

    private var requestId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestId = intent?.getIntExtra(FxPermission.EXTRA_REQUEST_ID, 0) ?: 0
        if (savedInstanceState != null) return   // 进程恢复：设置页仍在栈上，等 onActivityResult
        try {
            val settings = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            @Suppress("DEPRECATION")
            startActivityForResult(settings, REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            FxPermission.dispatch(requestId, FxPermission.isGranted(this))
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE) return
        // 设置页没有 resultCode 语义，以真实权限状态为准
        FxPermission.dispatch(requestId, FxPermission.isGranted(this))
        finish()
    }

    private companion object {
        const val REQUEST_CODE = 0x5001
    }
}
```

`internal class` 在 manifest 里被引用没问题（运行时反射实例化，不看 Kotlin 可见性）。

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-system:testDebugUnitTest`
Expected: 全绿。若 `shadowOf(activity).receiveResult` 不存在（API 名随版本变动），用 `controller.get()` 上的 `shadowOf(activity).callOnActivityResult(requestCode, resultCode, data)`；两者都没有就 `activity.javaClass.getDeclaredMethod("onActivityResult", …)` 反射调用并在测试里注明。

- [ ] **Step 5: Commit**

```bash
git add floatingx-system/src
git commit -m "feat(system): 悬浮窗权限检查、透明申请页与 Auto/Manual/Skip 策略（#192）"
```

---

### Task 6: `SystemHost` + `Builder` + `SystemWindowFeature`

**Files:**
- Create: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/feature/SystemWindowFeature.kt`
- Create: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/SystemHost.kt`
- Test: `floatingx-system/src/test/kotlin/com/petterp/floatingx/system/SystemHostTest.kt`

**Interfaces:**
- Consumes: Task 2 `hostFeatures()`、Task 4 容器、Task 5 权限、core `FxLayoutSpec`、`FxHostSession.requestSwap`。
- Produces:
  - `internal class SystemWindowFeature : FxFeature`：`onAttach` 与 `onConfigChanged` 把 `config.gesture.touchable` 同步到 `FxWindowContainer.setWindowTouchable`（容器不是 `FxWindowContainer` 时忽略）。
  - `public class SystemHost private constructor(...) : FxHost`：`public val windowLayoutParams: WindowManager.LayoutParams`（每次返回**拷贝**，`WindowManager.LayoutParams(lp)`）、`public fun retryPermission()`、`public val isPermissionGranted: Boolean`。
  - `public class Builder(context: Context)`：`layoutParams(customizer: SystemLayoutParamsCustomizer)`、`permission(strategy: FxPermissionStrategy)`、`fallback(host: FxHost)`、`keyboard(vararg editTextIds: Int)`（Task 7 的 `KeyboardFeature`；本 Task 先保存 ids，`hostFeatures()` 里 `if (keyboardIds.isNotEmpty()) KeyboardFeature(keyboardIds)`——Task 7 才创建该类，所以本 Task 的 Builder **先不加** `keyboard()`，Task 7 补）、`onBackPressed(listener: SystemBackListener)`、`build()`；`companion { @JvmStatic builder(context) }`。

**行为约定（spec §4 + 裁决）：**
1. `context`：`application` 若传入的是 Activity（`context.applicationContext`），系统窗口不能持有 Activity。
2. `bind(session)`：`Skip` 或已有权限 → `onHostReady()`；`Manual` → `interceptor.onRequest(request)`；`Auto` → `FxPermission.request(context) { granted -> if (!released) if (granted) ready() else denied() }`。
3. `denied()`：`fallback != null` → `session.requestSwap(fallback)`；否则 `Log.w("Fx-system", "悬浮窗权限被拒，浮窗停留在 INSTALLED；获得权限后调用 SystemHost.retryPermission()")`。
4. `retryPermission()`：released → 忽略；有权限 → 若 `session` 非空且未 ready 过 → `onHostReady()`（engine 对非 INSTALLED 幂等）；否则再按策略申请一次。
5. `createContainer()`：`lp = defaultLayoutParams()` 然后 `customizer?.customize(lp)`；`FxWindowContainer(context, wm, lp, backListener)`；`container.setBounds(screenW, screenH)`。
6. `attach(container)`：`container.setBounds(...)`；`wm.addView(container.view, container.windowParams)`；成功则 `container.isAttachedToWm = true`；捕获 `WindowManager.BadTokenException` / `SecurityException` → `Log.e` + 保持未挂载（权限中途被撤销的极端情况，不崩溃）。
7. `detach(container)`：若 `isAttachedToWm` → `wm.removeViewImmediate(container.view)`，置 false。
8. `updateLayout(container, spec)`：`(container as FxWindowContainer).applyLayout(spec.x, spec.y, spec.gravity, spec.ltr)`。
9. `bounds()`：R+ `wm.maximumWindowMetrics.bounds` 尺寸，否则 `wm.defaultDisplay.getRealSize`；insets = 当前容器的 `windowInsets`（未 attach 时 NONE）。
10. `hostFeatures()`：`listOf(SystemWindowFeature())`（lazy，稳定实例）。
11. `release()`：`released = true`、`session = null`。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.system

import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.gesture.FxGesture
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxRect
import com.petterp.floatingx.system.container.FxWindowContainer
import com.petterp.floatingx.system.permission.FxPermissionRequest
import com.petterp.floatingx.system.permission.FxPermissionStrategy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class SystemHostTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /** 降级目标：挂在一个 FrameLayout 上的最小 Layer host */
    private class LayerHost(private val parent: FrameLayout) : FxHost {
        override val context: Context get() = parent.context
        override fun bind(session: FxHostSession) = session.onHostReady()
        override fun createContainer(): FxContainer = FxLayerContainer(parent.context)
        override fun attach(container: FxContainer) { parent.addView(container.view) }
        override fun detach(container: FxContainer) { parent.removeView(container.view) }
        override fun bounds(): FxBounds = FxBounds(FxRect(0f, 0f, parent.width.toFloat(), parent.height.toFloat()))
        override fun release() {}
    }

    private fun content(): FxContent = FxContent.provider { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }
    private fun config(gesture: FxGesture = FxGesture.Normal): FxConfig = FxConfig.builder(content()).anchor(FxGravity.TOP_START).gesture(gesture).build()
    private fun install(host: SystemHost, config: FxConfig = config()): FxControl = FloatingX.install("sys", config, host)
    private fun window(control: FxControl): FxWindowContainer = control.contentView!!.parent as FxWindowContainer

    @After
    fun tearDown() = FloatingX.uninstallAll()

    @Test
    fun `granted permission mounts a window`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = install(SystemHost.builder(app).build())
        control.show()
        assertEquals(FxState.SHOWN, control.state)
        assertTrue(shadowOf(wm).views.contains(window(control)))
        assertTrue(window(control).isAttachedToWm)
        control.cancel()
        assertFalse(shadowOf(wm).views.contains(window(control)))
    }

    @Test
    fun `default layout params`() {
        ShadowSettings.setCanDrawOverlays(true)
        val host = SystemHost.builder(app).build()
        install(host)
        val lp = host.windowLayoutParams
        val expectedType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        assertEquals(expectedType, lp.type)
        assertEquals(WindowManager.LayoutParams.WRAP_CONTENT, lp.width)
        assertEquals(WindowManager.LayoutParams.WRAP_CONTENT, lp.height)
        assertEquals(PixelFormat.TRANSLUCENT, lp.format)
        val required = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        assertEquals(required, lp.flags and required)
    }

    @Test
    fun `customizer runs last and windowLayoutParams is a snapshot`() {
        ShadowSettings.setCanDrawOverlays(true)
        val host = SystemHost.builder(app)
            .layoutParams { it.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY; it.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE }
            .build()
        val control = install(host)
        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, window(control).windowParams.type)
        val snapshot = host.windowLayoutParams
        assertNotSame(window(control).windowParams, snapshot)
        snapshot.x = 999
        assertEquals(0, window(control).windowParams.x)
    }

    @Test
    fun `skip strategy never checks permission`() {
        ShadowSettings.setCanDrawOverlays(false)
        val control = install(SystemHost.builder(app).permission(FxPermissionStrategy.skip()).build())
        control.show()
        assertEquals(FxState.SHOWN, control.state)
        assertEquals(null, shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `auto strategy requests and mounts once granted`() {
        ShadowSettings.setCanDrawOverlays(false)
        val host = SystemHost.builder(app).build()
        val control = install(host)
        control.show()
        assertEquals(FxState.INSTALLED, control.state)
        val launch = shadowOf(app).nextStartedActivity
        assertEquals(com.petterp.floatingx.system.permission.FxPermissionActivity::class.java.name, launch!!.component!!.className)
        // 模拟用户在设置页授权后返回
        ShadowSettings.setCanDrawOverlays(true)
        val id = launch.getIntExtra("fx_request_id", 0)
        com.petterp.floatingx.system.permission.FxPermission.dispatch(id, true)
        assertEquals(FxState.SHOWN, control.state)
    }

    @Test
    fun `denied with fallback swaps to the fallback host`() {
        ShadowSettings.setCanDrawOverlays(false)
        val parent = FrameLayout(app)
        val fallback = LayerHost(parent)
        val control = install(SystemHost.builder(app).fallback(fallback).build())
        control.show()
        val launch = shadowOf(app).nextStartedActivity!!
        com.petterp.floatingx.system.permission.FxPermission.dispatch(launch.getIntExtra("fx_request_id", 0), false)
        assertSame(fallback, control.host)
        assertEquals(FxState.SHOWN, control.state)
        assertEquals(1, parent.childCount)
    }

    @Test
    fun `denied without fallback stays installed and retryPermission recovers`() {
        ShadowSettings.setCanDrawOverlays(false)
        val host = SystemHost.builder(app).build()
        val control = install(host)
        control.show()
        val launch = shadowOf(app).nextStartedActivity!!
        com.petterp.floatingx.system.permission.FxPermission.dispatch(launch.getIntExtra("fx_request_id", 0), false)
        assertEquals(FxState.INSTALLED, control.state)
        ShadowSettings.setCanDrawOverlays(true)
        host.retryPermission()
        assertEquals(FxState.SHOWN, control.state)
    }

    @Test
    fun `manual strategy hands the decision to the interceptor`() {
        ShadowSettings.setCanDrawOverlays(false)
        var request: FxPermissionRequest? = null
        val parent = FrameLayout(app)
        val fallback = LayerHost(parent)
        val control = install(SystemHost.builder(app).permission(FxPermissionStrategy.manual { request = it }).fallback(fallback).build())
        control.show()
        assertEquals(FxState.INSTALLED, control.state)
        assertEquals(null, shadowOf(app).nextStartedActivity)
        request!!.useFallback()
        assertSame(fallback, control.host)
        assertEquals(FxState.SHOWN, control.state)
    }

    @Test
    fun `updateLayout maps the anchor gravity into layout params`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = install(SystemHost.builder(app).build(), FxConfig.builder(content()).anchor(FxGravity.BOTTOM_END).build())
        control.show()
        val w = window(control)
        w.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        w.layout(0, 0, w.measuredWidth, w.measuredHeight)   // 触发 onContentSizeChanged → relayout
        assertEquals(Gravity.BOTTOM or Gravity.RIGHT, w.windowParams.gravity)
        assertEquals(0, w.windowParams.x)
        assertEquals(0, w.windowParams.y)
        val b = control.host.bounds()
        assertEquals(b.rect.width - 100f, control.position.x, 0f)
        assertEquals(b.rect.height - 50f, control.position.y, 0f)
    }

    @Test
    fun `moveTo writes layout params`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = install(SystemHost.builder(app).build())
        control.show()
        val w = window(control)
        w.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        w.layout(0, 0, w.measuredWidth, w.measuredHeight)
        control.moveTo(300f, 400f, animate = false)
        assertEquals(300, w.windowParams.x)
        assertEquals(400, w.windowParams.y)
        assertEquals(300f, control.position.x, 0f)
    }

    @Test
    fun `touchable false maps to FLAG_NOT_TOUCHABLE and follows config updates`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = install(SystemHost.builder(app).build(), config(FxGesture.Normal.copy(touchable = false)))
        control.show()
        val w = window(control)
        assertTrue(w.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        control.update(control.config.toBuilder().gesture(FxGesture.Normal).build())
        assertEquals(0, w.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    @Test
    fun `bounds is the real screen size`() {
        ShadowSettings.setCanDrawOverlays(true)
        val host = SystemHost.builder(app).build()
        install(host)
        val b = host.bounds()
        assertTrue(b.rect.width > 0f)
        assertTrue(b.rect.height > 0f)
    }

    @Test
    fun `activity context is unwrapped to the application`() {
        val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        assertSame(app, SystemHost.builder(activity).build().context)
    }
}
```

`FxGesture` 若不是 data class（无 `copy`），改用 `FxConfigScope`/`FxGestureScope`：`FxConfig.builder(content()).gesture(FxGesture.Normal).build()` 后 `FxConfigScope(base).apply { gesture { touchable = false } }.build()`——以 core 实际 API 为准，测试意图不变。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-system:testDebugUnitTest --tests '*SystemHostTest*'`
Expected: 编译失败（`SystemHost` 不存在）。

- [ ] **Step 3: 实现**

`feature/SystemWindowFeature.kt`：

```kotlin
package com.petterp.floatingx.system.feature

import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope
import com.petterp.floatingx.system.container.FxWindowContainer

/** 把 config.gesture.touchable 映射成窗口的 FLAG_NOT_TOUCHABLE（spec §4）；Layer 容器里 GestureFeature 自己透传，这里忽略 */
internal class SystemWindowFeature : FxFeature {

    private var container: FxWindowContainer? = null

    override fun onAttach(scope: FxFeatureScope) {
        val c = scope.container as? FxWindowContainer ?: return
        container = c
        c.setWindowTouchable(scope.config.gesture.touchable)
    }

    override fun onDetach() {
        container = null
    }

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        if (old.gesture.touchable != new.gesture.touchable) container?.setWindowTouchable(new.gesture.touchable)
    }
}
```

`SystemHost.kt`：

```kotlin
package com.petterp.floatingx.system

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.WindowManager
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.host.FxLayoutSpec
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxInsets
import com.petterp.floatingx.core.layout.FxRect
import com.petterp.floatingx.system.container.FxWindowContainer
import com.petterp.floatingx.system.feature.SystemWindowFeature
import com.petterp.floatingx.system.permission.FxPermission
import com.petterp.floatingx.system.permission.FxPermissionRequest
import com.petterp.floatingx.system.permission.FxPermissionStrategy

/**
 * 系统级 host（spec §4）：容器是一个 WindowManager 窗口。
 * - 权限：Auto（默认，自动弹透明页申请）/ Manual（交给拦截器）/ Skip
 * - 被拒：有 fallback → requestSwap 降级（原 SYSTEM_AUTO）；无 → 停在 INSTALLED，之后 retryPermission()
 * - 默认 LayoutParams 见 defaultLayoutParams()，customizer 最后执行可覆盖任何字段
 */
public class SystemHost private constructor(
    override val context: Context,
    private val customizer: SystemLayoutParamsCustomizer?,
    private val strategy: FxPermissionStrategy,
    private val fallback: FxHost?,
    private val backListener: SystemBackListener?,
) : FxHost {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var session: FxHostSession? = null
    private var container: FxWindowContainer? = null
    private var released = false
    private val features: List<FxFeature> by lazy { listOf<FxFeature>(SystemWindowFeature()) }
    private val screen = Point()

    /** 当前窗口 LayoutParams 的只读快照（拷贝） */
    public val windowLayoutParams: WindowManager.LayoutParams
        get() = WindowManager.LayoutParams().also { it.copyFrom(container?.windowParams ?: buildLayoutParams()) }

    public val isPermissionGranted: Boolean get() = FxPermission.isGranted(context)

    // ---------- FxHost ----------

    override fun bind(session: FxHostSession) {
        check(!released) { "SystemHost 已 release，不能复用；请新建一个 SystemHost" }
        this.session = session
        checkPermission()
    }

    override fun createContainer(): FxContainer =
        FxWindowContainer(context, wm, buildLayoutParams(), backListener).also {
            container = it
            readScreen()
            it.setBounds(screen.x, screen.y)
        }

    override fun attach(container: FxContainer) {
        val c = container as FxWindowContainer
        readScreen()
        c.setBounds(screen.x, screen.y)
        try {
            wm.addView(c, c.windowParams)
            c.isAttachedToWm = true
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "系统浮窗 addView 失败（权限被撤销或 type 不允许）", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "系统浮窗 addView 失败（权限被撤销或 type 不允许）", e)
        }
    }

    override fun detach(container: FxContainer) {
        val c = container as FxWindowContainer
        if (!c.isAttachedToWm) return
        wm.removeViewImmediate(c)
        c.isAttachedToWm = false
    }

    override fun updateLayout(container: FxContainer, spec: FxLayoutSpec) {
        (container as FxWindowContainer).applyLayout(spec.x, spec.y, spec.gravity, spec.ltr)
    }

    override fun bounds(): FxBounds {
        readScreen()
        container?.setBounds(screen.x, screen.y)
        val insets = container?.takeIf { it.isAttachedToWm }?.windowInsets ?: FxInsets.NONE
        return FxBounds(FxRect(0f, 0f, screen.x.toFloat(), screen.y.toFloat()), insets)
    }

    override fun hostFeatures(): List<FxFeature> = features

    override fun release() {
        released = true
        session = null
        container = null
    }

    // ---------- 权限 ----------

    /** 权限被拒后业务方拿到权限时调用；已有权限则直接挂载 */
    public fun retryPermission() {
        if (released) return
        checkPermission()
    }

    private fun checkPermission() {
        when {
            strategy is FxPermissionStrategy.Skip || FxPermission.isGranted(context) -> session?.onHostReady()
            strategy is FxPermissionStrategy.Manual -> strategy.interceptor.onRequest(request)
            else -> requestPermission()
        }
    }

    private fun requestPermission() {
        FxPermission.request(context) { granted ->
            if (released) return@request
            if (granted) session?.onHostReady() else denied()
        }
    }

    private fun denied() {
        val fb = fallback
        if (fb != null) {
            session?.requestSwap(fb)
        } else {
            Log.w(TAG, "悬浮窗权限被拒，浮窗停留在 INSTALLED；获得权限后调用 SystemHost.retryPermission()")
        }
    }

    private val request = object : FxPermissionRequest {
        override fun proceed() { if (!released) requestPermission() }
        override fun deny() { if (!released) denied() }
        override fun useFallback() { if (!released) denied() }
    }

    // ---------- internal ----------

    private fun readScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            screen.set(b.width(), b.height())
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealSize(screen)
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams = defaultLayoutParams().also { customizer?.customize(it) }

    private fun defaultLayoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        format = PixelFormat.TRANSLUCENT
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        // NO_LIMITS：半隐 / overflow 要把内容放到屏幕外；core 已按 safe area clamp，不会误出界
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        gravity = android.view.Gravity.TOP or android.view.Gravity.START
    }

    // ---------- Builder ----------

    public class Builder(context: Context) {
        private val context: Context = if (context is Activity) context.applicationContext else context
        private var customizer: SystemLayoutParamsCustomizer? = null
        private var strategy: FxPermissionStrategy = FxPermissionStrategy.Auto
        private var fallback: FxHost? = null
        private var backListener: SystemBackListener? = null

        /** 在默认 LayoutParams 之后执行，可覆盖 type / flags / softInputMode 等任何字段 */
        public fun layoutParams(customizer: SystemLayoutParamsCustomizer): Builder = apply { this.customizer = customizer }

        public fun permission(strategy: FxPermissionStrategy): Builder = apply { this.strategy = strategy }

        /** 权限被拒时降级到的 host（通常是 AppHost） */
        public fun fallback(host: FxHost): Builder = apply { fallback = host }

        public fun onBackPressed(listener: SystemBackListener): Builder = apply { backListener = listener }

        public fun build(): SystemHost = SystemHost(context, customizer, strategy, fallback, backListener)
    }

    public companion object {
        private const val TAG = "Fx-system"

        @JvmStatic
        public fun builder(context: Context): Builder = Builder(context)
    }
}
```

`WindowManager.LayoutParams.copyFrom` 返回变化位掩码，忽略即可。

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-system:testDebugUnitTest`
Expected: 全绿。`updateLayout maps the anchor gravity` 用例若 `w.layout()` 没触发 `onContentSizeChanged`（内容 view 未被 FrameLayout 布局到 100×50），先检查 `content()` 的 `layoutParams` 是否被 `setContent` 保留；不要改实现来迁就测试。

- [ ] **Step 5: Commit**

```bash
git add floatingx-system/src
git commit -m "feat(system): SystemHost——WindowManager 窗口、LayoutParams 默认值与覆写、权限策略与降级、retryPermission"
```

---

### Task 7: `KeyboardFeature` + `Builder.keyboard()`

**Files:**
- Create: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/feature/KeyboardFeature.kt`
- Modify: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/SystemHost.kt`（Builder 加 `keyboard(vararg editTextIds: Int)`；`hostFeatures()` 追加）
- Modify: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/container/FxWindowContainer.kt`（`dispatchKeyEventPreIme` BACK → `onImeBack` 回调）
- Test: `floatingx-system/src/test/kotlin/com/petterp/floatingx/system/KeyboardFeatureTest.kt`

**Interfaces:**
- Produces: `public class KeyboardFeature(private val editTextIds: IntArray) : FxFeature`；`FxWindowContainer.onImeBack: (() -> Unit)?`；`SystemHost.Builder.keyboard(vararg editTextIds: Int)`。

**行为：** `onAttach`：容器不是 `FxWindowContainer` → `logger?.e` 并返回；对每个 id 找到内容里的 view，设 `OnTouchListener`：`ACTION_DOWN` → `container.setWindowFocusable(true)`（去掉 `FLAG_NOT_FOCUSABLE` 并 `updateViewLayout`）+ `v.post { v.requestFocus(); imm.showSoftInput(v, 0) }`（等窗口真的可聚焦，唯一允许的 post）；返回 false 不吞事件。`container.onImeBack` = `{ hideKeyboard(); container.setWindowFocusable(false) }`；`onDetach` 恢复 not-focusable、清监听。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.system

import android.app.Application
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.system.container.FxWindowContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class KeyboardFeatureTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val editId = View.generateViewId()

    private fun content(): FxContent = FxContent.provider { ctx ->
        FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(300, 100)
            addView(EditText(ctx).apply { id = editId })
        }
    }

    @After
    fun tearDown() = FloatingX.uninstallAll()

    @Test
    fun `touching the edit text makes the window focusable and back restores it`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = FloatingX.install("kb", FxConfig.builder(content()).build(), SystemHost.builder(app).keyboard(editId).build())
        control.show()
        val window = control.contentView!!.parent as FxWindowContainer
        assertTrue(window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)

        val edit = control.contentView!!.findViewById<EditText>(editId)
        edit.dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0))
        assertEquals(0, window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        window.dispatchKeyEventPreIme(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        assertTrue(window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
    }

    @Test
    fun `detach restores the not focusable flag`() {
        ShadowSettings.setCanDrawOverlays(true)
        val control = FloatingX.install("kb2", FxConfig.builder(content()).build(), SystemHost.builder(app).keyboard(editId).build())
        control.show()
        val window = control.contentView!!.parent as FxWindowContainer
        control.contentView!!.findViewById<EditText>(editId).dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0))
        assertEquals(0, window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        control.cancel()
        assertTrue(window.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-system:testDebugUnitTest --tests '*KeyboardFeatureTest*'`
Expected: 编译失败（`keyboard` 不存在）。

- [ ] **Step 3: 实现**

`FxWindowContainer.kt` 追加：

```kotlin
    /** 键盘弹出期间按返回键（IME 之前收到）：KeyboardFeature 用它收起键盘并恢复不可聚焦 */
    public var onImeBack: (() -> Unit)? = null

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            val cb = onImeBack
            if (cb != null) {
                cb()
                return true
            }
        }
        return super.dispatchKeyEventPreIme(event)
    }
```

`feature/KeyboardFeature.kt`：

```kotlin
package com.petterp.floatingx.system.feature

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope
import com.petterp.floatingx.system.container.FxWindowContainer

/**
 * 系统窗口里的 EditText 唤起键盘（spec §4）：窗口默认 FLAG_NOT_FOCUSABLE 收不到输入，
 * 触摸到指定 EditText 时临时去掉该 flag 并弹键盘，返回键收起键盘后恢复。
 * 只对 Window 容器生效。
 */
public class KeyboardFeature(private val editTextIds: IntArray) : FxFeature {

    private var container: FxWindowContainer? = null
    private val bound = mutableListOf<View>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttach(scope: FxFeatureScope) {
        val c = scope.container as? FxWindowContainer
        if (c == null) {
            scope.logger?.e("KeyboardFeature 仅支持系统窗口容器，当前为 ${scope.container::class.java.simpleName}")
            return
        }
        container = c
        val root = scope.container.contentView ?: return
        for (id in editTextIds) {
            val v = root.findViewById<View>(id) ?: continue
            v.setOnTouchListener { view, ev ->
                if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                    c.setWindowFocusable(true)
                    // 窗口变为可聚焦要等 WindowManager 应用新 LayoutParams，下一轮再请求焦点与键盘（非定位用途的 post）
                    view.post {
                        view.requestFocus()
                        imm(view.context)?.showSoftInput(view, 0)
                    }
                }
                false
            }
            bound += v
        }
        c.onImeBack = {
            bound.firstOrNull { it.hasFocus() }?.let { imm(it.context)?.hideSoftInputFromWindow(it.windowToken, 0) }
            c.setWindowFocusable(false)
        }
    }

    override fun onDetach() {
        bound.forEach { it.setOnTouchListener(null) }
        bound.clear()
        container?.let {
            it.onImeBack = null
            it.setWindowFocusable(false)
        }
        container = null
    }

    private fun imm(context: Context): InputMethodManager? =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
}
```

`SystemHost.kt` 改动：构造器加 `private val keyboardIds: IntArray`；`features` 改为
`listOfNotNull<FxFeature>(SystemWindowFeature(), if (keyboardIds.isNotEmpty()) KeyboardFeature(keyboardIds) else null)`；
Builder 加：

```kotlin
        private var keyboardIds: IntArray = IntArray(0)

        /** 这些 EditText 被触摸时窗口临时可聚焦并弹出键盘 */
        public fun keyboard(vararg editTextIds: Int): Builder = apply { keyboardIds = editTextIds }
```

`build()` 传入 `keyboardIds`。

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-system:testDebugUnitTest`
Expected: 全绿。`detach restores` 用例：cancel 时 engine 先 `performDetach`（feature.onDetach → 恢复 flag）再 `host.detach`，顺序正确。

- [ ] **Step 5: Commit**

```bash
git add floatingx-system/src
git commit -m "feat(system): KeyboardFeature——系统窗口内 EditText 唤起键盘并在返回时恢复不可聚焦"
```

---

### Task 8: `systemHost` DSL、Java API 测试、文档同步

**Files:**
- Create: `floatingx-system/src/main/kotlin/com/petterp/floatingx/system/FxSystemExt.kt`
- Test: `floatingx-system/src/test/java/com/petterp/floatingx/system/JavaSystemApiTest.java`
- Test: `SystemHostTest.kt` 追加 1 个 DSL 用例
- Modify: spec §2.1（`hostFeatures`）、§2.4（手势坐标一句）、§4（`keyboard` 走 host feature、`FLAG_LAYOUT_NO_LIMITS` 默认、fun interface 名、`retryPermission`/`isPermissionGranted`、Activity context 解包、`addView` 失败处理）；`CLAUDE.md` 模块表加 system。

- [ ] **Step 1: 写失败测试**

`SystemHostTest.kt` 追加：

```kotlin
    @Test
    fun `systemHost dsl installs with a fallback`() {
        ShadowSettings.setCanDrawOverlays(true)
        val parent = FrameLayout(app)
        val control = FloatingX.install("dsl") {
            view { ctx -> View(ctx).apply { layoutParams = ViewGroup.LayoutParams(100, 50) } }
            systemHost(app) { fallback(LayerHost(parent)); layoutParams { it.alpha = 0.9f } }
        }
        control.show()
        assertTrue(control.host is SystemHost)
        assertEquals(0.9f, window(control).windowParams.alpha, 0f)
    }
```

`JavaSystemApiTest.java`：

```java
package com.petterp.floatingx.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.view.View;
import android.view.WindowManager;

import androidx.test.core.app.ApplicationProvider;

import com.petterp.floatingx.core.FloatingX;
import com.petterp.floatingx.core.FxControl;
import com.petterp.floatingx.core.FxState;
import com.petterp.floatingx.core.config.FxConfig;
import com.petterp.floatingx.core.config.FxContent;
import com.petterp.floatingx.core.layout.FxGravity;
import com.petterp.floatingx.system.permission.FxPermissionStrategy;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowSettings;

/** spec §7 的 Java 样例必须能编译并工作 */
@RunWith(RobolectricTestRunner.class)
public class JavaSystemApiTest {

    @After
    public void tearDown() {
        FloatingX.uninstallAll();
    }

    @Test
    public void builderChainAndInstall() {
        ShadowSettings.setCanDrawOverlays(true);
        Application app = ApplicationProvider.getApplicationContext();
        FxConfig config = FxConfig.builder(FxContent.view(new View(app)))
                .anchor(FxGravity.CENTER_END, 0f, 120f)
                .build();
        SystemHost host = SystemHost.builder(app)
                .layoutParams(lp -> lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                .permission(FxPermissionStrategy.auto())
                .keyboard(android.R.id.edit)
                .onBackPressed(() -> true)
                .build();
        FxControl control = FloatingX.install("java", config, host);
        control.show();
        assertEquals(FxState.SHOWN, control.getState());
        assertTrue(host.isPermissionGranted());
        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, host.getWindowLayoutParams().type);
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-system:testDebugUnitTest`
Expected: 编译失败（`systemHost` 不存在）。

- [ ] **Step 3: 实现 FxSystemExt.kt**

```kotlin
package com.petterp.floatingx.system

import android.content.Context
import com.petterp.floatingx.core.config.FxInstallScope

/** DSL：`FloatingX.install("tag") { layout(...); systemHost(app) { fallback(appHost(app)) } }` */
@JvmSynthetic
public fun FxInstallScope.systemHost(context: Context, block: SystemHost.Builder.() -> Unit = {}): SystemHost =
    SystemHost.Builder(context).apply(block).build().also { host = it }
```

- [ ] **Step 4: 文档同步**

spec（用 Edit 精确替换）：
- §2.1 `FxHost` 代码块末尾加一行 `fun hostFeatures(): List<FxFeature> = emptyList()   // host 自带 feature（键盘、窗口 flag 映射），换 host 时替换`。
- §2.4 末尾补一句：`slop 与拖动增量按屏幕坐标（rawX 偏移）计算，落点判断按容器相对坐标——Window 容器随手指移动时相对坐标不可用。`
- §4 代码块：`layoutParams(customizer: SystemLayoutParamsCustomizer)`、`permission(strategy: FxPermissionStrategy)   // auto()（默认）| manual(interceptor) | skip()`、`keyboard(vararg editTextIds: Int)   // hostFeatures() 提供 KeyboardFeature`、`onBackPressed(listener: SystemBackListener)`；`SystemHost` 加 `public fun retryPermission()` 与 `public val isPermissionGranted`；`FxPermissionInterceptor { fun onRequest(request: FxPermissionRequest) }`。
- §4 默认 LayoutParams 一条：flags 加 `FLAG_LAYOUT_NO_LIMITS`（半隐/overflow 需要；core 已按 safe area clamp），`format = TRANSLUCENT`。
- §4 追加：`Builder 传入 Activity 时解包为 applicationContext；addView 抛 BadTokenException/SecurityException 时记录日志、窗口保持未挂载（不崩溃），retryPermission() 可再试。`
- §4 权限一条：`回调通过 FxPermission 内部的 SparseArray<FxPermissionCallback> 按 requestId 分发`。

CLAUDE.md 模块表加 `floatingx-system`（包 `com.petterp.floatingx.system`），测试命令加 `:floatingx-system:test`，Plan 3 计划文件路径。

- [ ] **Step 5: 全量测试与提交**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL；core 132、scope 20、app 30、system ≥ 35（debug/release 各一遍）。

```bash
git add floatingx-system/src docs/superpowers/specs CLAUDE.md
git commit -m "feat(system): systemHost DSL 与 Java API 测试；docs: spec §2.1/§2.4/§4 同步 Plan 3 裁决"
```

---

## 自查记录

- **Spec 覆盖：** §4 Builder 五个方法（layoutParams / permission / fallback / keyboard / onBackPressed）→ Task 6/7；`windowLayoutParams` 快照 → Task 6；默认 LP + customizer 最后执行 → Task 6；`touchable=false → FLAG_NOT_TOUCHABLE` → Task 6 `SystemWindowFeature`；键盘临时可聚焦 → Task 7；任意 context 申请权限 + SparseArray 分发 + Manual 拦截器 → Task 5；被拒 → swap / INSTALLED + retry → Task 6；`bounds()` WindowMetrics/RealSize + insets → Task 4/6；§2.3 gravity 映射 → Task 4；§10 "SystemHost：canDrawOverlays、LP 默认值 + customizer、fallback swap" → Task 6 用例。
- **占位扫描：** 无。
- **类型一致性：** `FxWindowContainer.windowParams`（Task 4 定义，Task 6/7/8 一律用它）；`applyLayout(x, y, gravity: FxGravity, ltr)` 在 Task 4 定义、Task 6 `updateLayout` 调用；`FxPermission.dispatch(id, granted)` internal，Task 6 测试同模块可用；`FxPermissionStrategy.Auto/Skip` 为 object，`when` 用 `is` 判断；`hostFeatures()` Task 2 定义、Task 6/7 覆写。

## 执行记录（SDD ledger 归档，供 Plan 4–5 参考）

分支 feat/3.0-system，11 个功能提交 + 2 个修复波提交（0ab5c34..dcb6d6f）；最终 ./gradlew test：core 133 / scope 22 / app 32 / system 60，0 失败；system lint 0 警告。

### Pre-flight scan

| Pair / Task | Produces vs consumes | Finding |
|---|---|---|
| T1 ↔ T5 | T1 manifest 引用 `permission.FxPermissionActivity`（T5 才创建）；库模块 assemble/test 不校验 manifest 类存在，只有 lint 会 | 一致（T1–T4 不跑 lint，计划已注明） |
| T2 ↔ T6/T7 | T2 `FxHost.hostFeatures()` 默认空 + FxControlImpl init/swap 接入；T6 返回 SystemWindowFeature，T7 追加 KeyboardFeature，要求实例稳定（lazy） | 一致 |
| T3 ↔ T4/T6 | T3 检测器 slop/增量用 rawX 偏移；T4 容器 hitTest/触摸转发不变；Layer 语义等价（Robolectric obtain 的 raw==x） | 一致 |
| T4 ↔ T6 | `windowParams`、`applyLayout(x,y,gravity,ltr)`、`setBounds`、`isAttachedToWm`、`windowInsets`、`setWindowTouchable/Focusable`；T6 全部按此调用 | 一致（layoutParams→windowParams 已在写计划时改名） |
| T5 ↔ T6 | `FxPermission.request(context, FxPermissionCallback)`、`dispatch(id, granted)` internal、`EXTRA_REQUEST_ID = "fx_request_id"`；T6 测试用同一字符串 | 一致 |
| T6 ↔ T7 | T6 Builder 无 keyboard()，T7 补 keyboardIds 构造参数 + `features` 改 listOfNotNull | 一致 |
| T6 ↔ T8 | `SystemHost.Builder` 全部方法 + `isPermissionGranted` + `windowLayoutParams` 在 Java 测试使用 | 一致 |
| T6 自身 | 测试用 `FxGesture.Normal.copy(touchable=false)`——FxGesture 是否 data class 待实现者核对，计划给了 DSL 替代 | 一致 |
| T4 自身 | 首帧闪现防护：positioned 前 GONE；setContentVisible(true) 在 positioned 后才 VISIBLE | 一致 |
| T5 自身 | `shadowOf(activity).receiveResult` API 名可能因 Robolectric 版本而异，计划给了两级兜底 | 一致 |

- Ruling: 就地建分支 feat/3.0-system；实现者一律 `model: opus`（用户 CLAUDE.md）；评审按复杂度 sonnet/opus；完成后本地 ff 合回 main、不 push（沿用 Plan 1/2 用户选择）。
- Ruling: spec §4 默认 flags 加 FLAG_LAYOUT_NO_LIMITS（半隐/overflow 必需；core 已按 safe area clamp）— 若错，代价是用户可用 customizer 去掉该 flag。
- Ruling: `keyboard()` 保留在 SystemHost.Builder，但通过新增的 `FxHost.hostFeatures()` 以 feature 落地（core 小扩展）；`touchable→FLAG_NOT_TOUCHABLE` 同路径 — 若错，代价是一个 core 钩子。

### Tasks
- Task 1: complete (commits 0ab5c34..d8843fe, review clean)
- Task 2: minor (deferred): config.features 与 hostFeatures() 出现同一实例时无去重；host feature 与 update(config) 的交互无直接测试。
- Task 2: complete (commits d8843fe..54cf347, review clean)
- Task 3: minor (deferred): FxGestureDetector 文件级 KDoc "收到的坐标均为容器坐标" 一句与新增说明并列易误读。
- Task 3: complete (commits 54cf347..e8dc048, review clean)
- Task 4: Ruling: 评审 Important 1（容器在 onConfigurationChanged/insets 变化时用旧 boundsW/H 派发，同步链路里 host 无法先刷新）— 由容器自行 `refreshBounds()`（通过 wm 读屏幕尺寸）再派发 onBoundsChanged，host 的 bounds() 复用容器缓存；并入 Task 6 实施 — 若错，代价是容器多持有一份屏幕尺寸读取逻辑。
- Task 4: Ruling: 评审 Important 2（touchHandler 转发 / onContentSizeChanged 仅真变化 / onConfigurationChanged→onBoundsChanged / insets→onBoundsChanged 无测试）与 Minor 4（toInt 改 roundToInt）并入 Task 6 实施；Important 3 由 Task 6 既有的 gravity 回环用例覆盖。
- Task 4: minor (deferred): hitTest 假设内容 left/top=0 且无 margin；BACK 的 ACTION_DOWN 未消费；onConfigurationChanged 对任何配置变化都派发（relayout 幂等）；GONE 根 view 下 ViewRootImpl 仍会 measure/layout 的假设需真机验证（Plan 5）。
- Task 4: complete (commits e8dc048..8b2f3b2, review approved; 3 Important 并入 Task 6)
- Task 5: Ruling: spec §4 要求的 `noHistory` 与 startActivityForResult 流程冲突（被设置页遮住即被系统 finish，onActivityResult 不回调）— 去掉 noHistory，保留 excludeFromRecents + taskAffinity="" + NEW_TASK，onDestroy 兜底派发未完成回调；request 标 @MainThread；spec §4 由 Task 8 同步 — 若错，代价是权限页在极端情况下多留一个不可见 task 直到回调完成。
- Task 5: fix round 1/5 (Critical noHistory + 2 Important 派出; 与 Task 6 实现并行，文件无重叠)
- Task 5: minor (deferred): 进程死亡后静态回调表丢失，恢复的权限页 dispatch 为空操作（"进程死亡即请求作废"，SystemHost 可在需要时重申请）。
- Task 5: complete (commits 8b2f3b2..c118273 + 57cb4e6, review clean after 1 fix round)
- Task 6: implementer concerns: attach 失败（BadToken/SecurityException）后 engine 停在 ATTACHED 无窗口、retryPermission 无法恢复；release() 额外兜底 detach。交评审判定。
- Task 6: Ruling: 评审 Important（attach 抛 BadToken/SecurityException 后 engine 停在 ATTACHED/SHOWN 无窗口，retryPermission 无法恢复）— 在 retryPermission() 里：容器存在且未挂到 WM 且有权限时先 onHostLost 再 onHostReady 让 engine 重新 attachAndRestore；并入 Task 7 实施 — 若错，代价是一次多余的 detach/attach。
- Task 6: minor 并入 Task 7：setBounds 改 internal/@VisibleForTesting；删 SystemHost.readScreen 重复（container 为空时返回零 rect）；SystemWindowFeature.onAttach 无条件赋值 container；默认 LP gravity 写 TOP|LEFT。
- Task 6: minor (deferred): Builder 只解包裸 Activity（ContextThemeWrapper(activity) 仍持有 Activity）；denied() 用 Log.w 不受 FxLogger 门控（host 拿不到 logger）。
- Task 6: complete (commits 57cb4e6..2dc6166, review approved; 1 Important 并入 Task 7)
- Task 7: implementer concerns: 键盘弹出时按返回会同时触发 SystemBackListener（DOWN 被 pre-IME 吞、UP 仍到 dispatchKeyEvent）；IME 被点击外部收起时 focusable 不恢复（真机验证）。
- Task 7: Ruling: 评审 Important（BACK 的 DOWN 被 pre-IME 吞掉后 UP 仍触发 SystemBackListener）— FxWindowContainer 记录 imeBackConsumed 标志，对应 UP 也吞掉且不调 listener；放入最终修复波 — 若错，代价是键盘收起那一次返回不透传给业务（本就不该）。
- Task 7: minor (deferred): 点击外部收起 IME 时 focusable 不恢复（真机验证，Plan 5）；KeyboardFeature.onAttach 在 contentView 为空提前返回前已赋 container。
- Task 7: complete (commits 2dc6166..0672a4f, review approved; 1 Important 入最终修复波)
- Task 8: minor (deferred): spec §7 Kotlin 样例 `host = systemHost(app){}` 冗余赋值；§2.1 createContainer(context) 与实现 createContainer() 不一致（Plan 1 遗留）。
- Task 8: complete (commits 0672a4f..9cd5428, review clean)
- Final review (opus, 0ab5c34..9cd5428): 1 Critical / 5 Important / 11 minor；除 insets 来源外所有裁决被确认。
- Ruling: 系统窗口 safe-area insets 改为屏幕级（R+ `wm.currentWindowMetrics.windowInsets` 在 refreshBounds() 里读；R 以下 NONE），删除"窗口自身 onApplyWindowInsets → onBoundsChanged"路径；spec §4 insets 一条同步 — 若错，代价是 R 以下系统窗口无 safe area（2.x 亦如此）。
- Final fix wave（一次派出）：Critical 1；Important 2 RTL layoutDirection、3 后台申请守卫 + startActivity try/catch、4 Builder 解包 ContextWrapper 链 + theme()、5 拖动集成测试、6 双 BACK（imeBackConsumed + CANCEL/失焦清标志）；minor 7 Skip 恢复、8 attach/detach 加宽 catch、9 KeyboardFeature 内容替换重绑、17 lint 抑制；文档：FxGestureDetector KDoc、spec §7 样例、§2.1 createContainer()、§4 insets/LP 陷阱/fallback context/SYSTEM_ALERT_WINDOW 说明。
- Final review minor (deferred → Plan 5 真机清单)：GONE 首帧假设、拖过状态栏边界、外部点击收起 IME 后 focusable、运行时撤权后 addView 失败、RTL 锚点；createWindowContext（R+）；hitTest 在 Window 容器为死代码；每帧 FxPoint/FxLayoutSpec 分配（Plan 1 遗留）。
- Final fix wave: commits 9cd5428..dcb6d6f（Critical 1 + Important 2–6 + minor 7–10 + docs 11–13）; scoped re-review 派出
- Final re-review: 13/13 ADDRESSED；minor 观察：insets 只在 create/attach/配置变化时刷新（immersive 切换不触发）；FxPermission catch(Exception) 偏宽；size 取 maximumWindowMetrics 而 insets 取 currentWindowMetrics（非视觉 context 下一致）。
