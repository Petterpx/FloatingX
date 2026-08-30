# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

FloatingX — an Android floating-window (悬浮窗) library published to Maven Central as
`io.github.petterpx:floatingx-{core,app,scope,system,compose}`. Comments, logs and docs are written
in Chinese; keep that convention when editing library sources.

3.0 is a full rewrite; the 2.x modules (`floatingx/`, `floatingx_compose/`) are gone and the API is
not backwards compatible (see `docs/MIGRATION.md`).

## 模块

所有库模块都用 `build-logic/` 的 `floatingx.library` convention plugin
（minSdk 21 / compileSdk 36 / Java 17 / `explicitApi()` / `jvmDefault=enable` / maven 坐标 / Robolectric）。
模块自己的 `build.gradle.kts` 只声明依赖。

| 模块 | 包 | 内容 | 依赖 |
|---|---|---|---|
| `floatingx-core` | `com.petterp.floatingx.core` | 状态机 `FxEngine`、锚点定位、手势、feature、`FxControl`、`FloatingX` 注册表、`FxSpStorage` | `androidx.annotation`(api)、`androidx.core` |
| `floatingx-app` | `com.petterp.floatingx.app` | `AppHost`：跟随前台 Activity 的全局浮窗；黑白名单 / filter | core、`androidx.core` |
| `floatingx-system` | `com.petterp.floatingx.system` | `SystemHost`：`WindowManager` 窗口、悬浮窗权限、键盘 / 返回键 | core、`androidx.core` |
| `floatingx-scope` | `com.petterp.floatingx.scope` | `ViewGroupHost` / `FragmentHost` 与 `fxScope {}` 局部浮窗 | core、`androidx.fragment`(**compileOnly**) |
| `floatingx-compose` | `com.petterp.floatingx.compose` | `compose {}` DSL、归 control 所有的 `FxComposeOwner`、`stateFlow()` / `positionFlow()`（**minSdk 23**） | core、compose-ui(api)、lifecycle 2.10.0、savedstate、coroutines |
| `app` | `com.petterp.floatingx.demo` | demo + instrumentation 测试工程（minSdk 23） | 以上全部 |

`floatingx-app` 用清单里声明的 `FxAppInitProvider`（ContentProvider）在进程启动时
`FxActivityTracker.init(application)`，所以 install 写在任何时机都能拿到当前前台 Activity。
`floatingx-system` 的清单声明了 `SYSTEM_ALERT_WINDOW` 与权限申请页 `FxPermissionActivity`，
接入方无需自行配置。

依赖边界（CI 有 JUnit 扫描断言）：core 源码不得 import `android.view.WindowManager`、
`androidx.fragment`、`androidx.compose`、`androidx.lifecycle`、`androidx.appcompat`。

## Build & Commands

Java 17 is required (AGP 8.13.2 / Gradle 8.14.3 / Kotlin 2.2.21). Version catalog:
`gradle/libs.versions.toml`.

```bash
./gradlew test                                                       # 全部 JVM/Robolectric 单测（CI 必跑）
./gradlew :floatingx-core:test                                       # 单模块（core 140 / scope 22 / app 32 / system 60 / compose 25 用例）
./gradlew app:assembleDebug                                          # 构建 demo apk
./gradlew app:installDebug                                           # 安装 demo
./gradlew publishToMavenLocal -PisPublish=false -PversionName=3.0.0-SNAPSHOT   # CI 每个 PR 都跑
./gradlew lint                                                       # android lint
```

Instrumentation（需要设备 / 模拟器，CI 用 `reactivecircus/android-emulator-runner`，api-level 34）：

```bash
./gradlew app:installDebug app:installDebugAndroidTest
adb shell appops set com.petterp.floatingx.app SYSTEM_ALERT_WINDOW allow   # 系统浮窗用例需要
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
./gradlew app:connectedDebugAndroidTest
```

**Robolectric 固定 `sdk=35`**（各模块 `src/test/resources/robolectric.properties`）：SDK 36 的沙箱
要求 JDK 21，而本仓库工具链是 JDK 17。改这个值前先确认工具链。

Publishing（release workflow，GitHub Release 触发）：
`./gradlew publishAndReleaseToMavenCentral --no-configuration-cache -PisPublish=true -PversionName=$TAG`

Gradle properties（`settings.gradle` 读进 `rootProject.ext`）：

- `-PversionName` / `-PversionCode` — 默认取 `git describe --tags` 与 `git rev-list HEAD --count`。
- `-PisPublish` — `true` 时应用 `signAllPublications()`（需要 GPG 环境变量）。
- `isDev` in `local.properties`（默认 `true`）— `true` 时 `app` 依赖本地 project，`false` 切到已发布产物。

日志 tag 是 `Fx-<scope>`（`Fx-system` 等）与用户自己传的（demo 用 `Fx-demo`），
所以用 `adb logcat | grep "Fx-"`。只有配置里调过 `enableLog(tag)` 才会有日志。

## Architecture：Host / Engine / Feature

三个正交的角色，取代 2.x 的 Helper → Control → Provider：

1. **Host（`core.host.FxHost`）—— 浮窗挂在哪。**
   `bind(session)` / `createContainer()` / `attach` / `detach` / `bounds()` / `release()`，
   通过 `FxHostSession` 向 engine 报告 `onHostReady` / `onHostLost` / `onBoundsChanged` / `requestSwap`。
   - `AppHost`（app）：容器是 `FxLayerContainer`，挂到当前前台 Activity 的 **DecorView**
     （默认，不是 `R.id.content`，这样拖动才是真正全屏）；换页时把**同一个容器**静默 reparent，
     engine 状态、feature、动画都不重来。被黑白名单/filter 拒绝的页面上整体卸下。
   - `SystemHost`（system）：容器是 `FxWindowContainer`，挂到 `WindowManager`；
     权限三策略 `Auto/Manual/Skip`，被拒时 `requestSwap(fallback)` 降级到 `AppHost`（原 `SYSTEM_AUTO`）。
   - `ViewGroupHost` / `FragmentHost`（scope）：挂到任意 `ViewGroup` / Fragment 根 view；
     不进注册表，生命周期归调用方。

2. **Engine（`core.engine.FxEngine`）—— 状态机 + 命令队列。**
   `INSTALLED → ATTACHED → SHOWN`，终态 `CANCELLED`。host 未 ready 时 `show/hide/moveTo` 入队，
   ready 后按序回放；`onHostLost` 保留 `desiredVisible`，`swapHost` 保留 anchor / listener / feature。
   内容 view 归 engine 所有（不归 host），所以换页、换 host 都不会重建内容。

3. **Feature（`core.feature.FxFeature`）—— 容器行为插件。**
   `onAttach(scope)` / `onDetach` / `onCancel` / `onRemove` / `onConfigChanged` /
   `onContentSizeChanged` / `onBoundsChanged` / `onShow` / `onHide`。
   内置 `LocationFeature`（锚点、margin、overflow、safeArea、吸附、持久化）、
   `GestureFeature`、`AnimationFeature`、`ModalScrimFeature`；
   host 还能通过 `hostFeatures()` 追加（system 的 `SystemWindowFeature` / `KeyboardFeature`），
   compose 模块追加 `ComposeOwnerFeature`。feature 之间不互相引用，共享数据走 `FxFeatureScope`。

**新增行为时优先加一个 `FxFeature`，而不是往容器里塞代码；新增配置项优先加到 `FxConfigScope` +
`FxConfig.Builder`，而不是加构造参数。** 两边都要加：Kotlin DSL 走 `FxConfigScope`，Java 走
`FxConfig.Builder`。

### 配置与内容

`FxConfig` 不可变；Kotlin 用 `FxConfigScope` DSL（`FloatingX.install(tag) {}` 是 `FxInstallScope`，
多一个 `host`），Java 用 `FxConfig.builder(FxContent.layout(id))`。
`control.update {}` 基于旧配置局部修改。内容是 `FxContent.Layout / Static / Provider`。

### 定位

纯 Kotlin 的几何类型（`core.layout.FxGeometry`，刻意不用 `android.graphics.*`），
`FxLayoutResolver` / `FxAdsorbResolver` 因此可以在纯 JVM 里表驱动测试。
存的是 `FxAnchor(gravity, dx, dy)` 而不是左上角坐标——这是 3.0 修掉一大票尺寸/旋转 issue 的根因。

### Java interop

公开 API 全是 Kotlin，但都留了 Java 入口：DSL-only 入口标 `@JvmSynthetic`；
`FxConfig.Builder` / `AppHost.Builder` / `SystemHost.Builder` / `ViewGroupHost.of` /
`FxPermissionStrategy.auto()/manual()/skip()` 是 Java 侧入口；`FxListener` 全默认方法
（convention plugin 开了 `jvmDefault=enable`）；`AppActivityFilter` /
`SystemLayoutParamsCustomizer` / `FxRegion` 是 `fun interface`。
**改公开 API 后必须保证 `app/src/main/java/com/petterp/floatingx/demo/java/JavaDemo.java` 仍然编译。**

## Validation

三层：

1. `./gradlew test` — 五个模块共 279 个 JVM / Robolectric 用例。
2. `./gradlew app:connectedDebugAndroidTest` — `app/src/androidTest/`：
   `AppHostReparentTest`（换页 reparent + 黑名单）、`RotationTest`（旋转重建 + 锚点持久化）、
   `SystemWindowResizeTest`（WM 窗口 resize 时 LayoutParams 无跳变）、
   `ComposeOwnerSurvivalTest`（owner 跨 Activity 存活）、`ModalScrimTest`（modal 拦截）。
   共用工具在 `TestUtil.kt`（主线程调用 + `await` 条件等待，禁止裸 sleep）。
3. demo 手工验证 —— `app` 模块，`./gradlew app:installDebug`：

**能力页**（`demo/pages/`）：`AppHostActivity`、`SystemHostActivity`、`ScopeHostActivity`、
`GestureActivity`、`LayoutActivity`、`MultiWindowActivity`、`ModalActivity`、
`ComposeActivity` / `ComposeSecondActivity`、`SecondActivity`、`BlackActivity`、`ImmersedActivity`。

**回归页**（`demo/regression/`，按 issue 编号命名）：`Issue187Activity`（尺寸变化锚点不动）、
`Issue210Activity`（Compose 跨页存活）、`Issue221Activity`（黑名单命中子类）、
`Issue240Activity`（越界不被裁剪）、`Issue244Activity`（Fragment 内浮窗）。

全局浮窗集中在 `demo/DemoWindows.kt` 安装（页面按钮只操作 `FxControl`）；
Java 样例在 `demo/java/JavaDemo.java`；页面骨架 DSL 在 `demo/ui/DemoPage.kt`，
浮窗内容在 `demo/ui/DemoContent.kt`。demo 的 Activity **一律不配 `configChanges`**（旋转走 recreate）。

## 计划与裁决归档

设计与实施计划都在 `docs/superpowers/`：

- Spec：`docs/superpowers/specs/2026-08-29-floatingx-3-modular-architecture-design.md`
  （§1.1 版本矩阵、§2 core、§3–6 各模块、§7 公开 API、§9 issue 覆盖矩阵、§10 测试策略）。
- Plans：`docs/superpowers/plans/`（plan 1 core、plan 2 scope+app、plan 3 system、plan 4 compose、
  plan 5 demo）。

用户文档：`README.md` / `README_EN.md`（结构一致，中英对照）、`docs/MIGRATION.md`（2.x → 3.0 对照）。
改公开 API 时三份都要同步。
