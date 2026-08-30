# FloatingX 3.0 Plan 1：工程骨架 + floatingx-core 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把仓库升级到版本矩阵 A 的工具链，建立 `build-logic` convention plugin，并完整实现 `floatingx-core`（几何/锚点定位、吸附、状态机、手势、Layer 容器、Feature 插件、配置 DSL/Builder、Control、注册表、Activity 跟踪），全部带 JVM/Robolectric 测试。

**Architecture:** core 对平台零感知：`FxHost`（谁承载）+ `FxEngine`（状态机 + 命令队列）+ `FxFeature`（容器行为插件）。位置真值只有 `anchor`，`FxLayoutResolver` 是纯函数；手势判定在 `FxGestureDetector` 中，容器只转发。本计划不含 app/system/scope/compose 模块与 demo 重写（后续 Plan 2–5），旧的 `floatingx` / `floatingx_compose` 模块与 demo 在本计划中保持可编译、不删除。

**Tech Stack:** Kotlin 2.2.21、AGP 8.13.2、Gradle 8.14.3、JDK 17、compileSdk 36、androidx.core 1.13.1、JUnit 4.13.2、Robolectric 4.16.1、androidx.test 1.7.0。

**Spec:** `docs/superpowers/specs/2026-08-29-floatingx-3-modular-architecture-design.md`（§1、§2、§8、§10 由本计划实现）

## Global Constraints

- 仓库工具链：AGP `8.13.2`、Gradle `8.14.3`（现有 wrapper 不动）、Kotlin `2.2.21`、JDK 17。
- 所有模块 `minSdk = 21`、`compileSdk = 36`、`targetSdk = 36`。
- 库模块 Kotlin：`explicitApi()`、`jvmTarget = 17`、`jvmDefault = ENABLE`（Java 实现监听器接口时只需覆写用到的方法）。**不做** `languageVersion/apiVersion` 降级锁定，产物 metadata 跟随 Kotlin 2.2（使用方 Kotlin ≥ 2.1 可用）。
- core 只允许依赖 `androidx.core:core:1.13.1` + `kotlin-stdlib`；`src/main` 禁止 import `android.view.WindowManager`、`androidx.fragment`、`androidx.compose`、`androidx.lifecycle`、`androidx.appcompat`、`androidx.savedstate`、`kotlinx.coroutines`（Task 2 的测试守住）。
- 包名：core 一律在 `com.petterp.floatingx.core` 及其子包；`namespace = "com.petterp.floatingx.core"`。
- 注释、日志、KDoc 用中文；日志 tag 形如 `Fx-<tag>`。
- 几何类型（`FxPoint/FxSize/FxRect/FxInsets`）是 core 自己的纯 Kotlin data class，**不用** `android.graphics.*`，保证定位逻辑纯 JVM 可测。
- 性能：触摸 MOVE 路径无对象分配（`FxPoint` 仅在拖动结束/提交时创建）、移动只写 `translationX/Y`、定位不使用 `post/postDelayed`（唯一定时器是长按）。
- 每个 Task 结束时 `./gradlew :floatingx-core:test` 必须全绿；提交信息用中文、Conventional Commits 前缀（`feat:` / `build:` / `test:`）。
- 所有 `./gradlew` 命令在仓库根目录执行；用 `--offline` 之外的正常模式（首次需要下载新 AGP/Kotlin）。

---

## 文件结构（本计划新增 / 修改）

```
build-logic/
  settings.gradle.kts                       版本目录接入
  build.gradle.kts                          kotlin-dsl + AGP/KGP/maven-publish 依赖
  src/main/kotlin/floatingx.library.gradle.kts   库模块 convention plugin
floatingx-core/
  build.gradle.kts
  consumer-rules.pro
  src/main/kotlin/com/petterp/floatingx/core/
    FloatingX.kt                            全局注册表
    FxControl.kt                            公开控制接口 + update{} 扩展
    FxListener.kt                           事件监听器
    FxState.kt                              状态枚举
    FxLogger.kt                             日志接口 + Logcat 实现
    FxActivityTracker.kt                    前台 Activity 跟踪（app 模块使用）
    internal/FxControlImpl.kt               Control 实现（engine + container + features 装配）
    layout/FxGeometry.kt                    FxPoint/FxSize/FxRect/FxInsets/FxMargin/FxOverflow/FxBounds
    layout/FxGravity.kt                     FxHorizontal/FxVertical/FxGravity
    layout/FxAnchor.kt
    layout/FxLayoutInput.kt
    layout/FxLayoutResolver.kt              锚点 ↔ 坐标 纯函数
    layout/FxAdsorb.kt                      FxEdge/FxHalfHide/FxAdsorb
    layout/FxAdsorbResolver.kt              吸附目标点 纯函数
    storage/FxStorage.kt                    持久化接口
    storage/FxSpStorage.kt                  SharedPreferences 实现
    host/FxHost.kt  host/FxHostSession.kt  host/FxLayoutSpec.kt
    engine/FxCommand.kt  engine/FxEngineDelegate.kt  engine/FxEngine.kt
    gesture/FxGesture.kt                    FxDrag/FxChildPriority/FxRegion/FxGesture
    gesture/FxGestureDetector.kt
    container/FxContainer.kt                容器接口 + FxContainerTouchHandler
    container/FxLayerContainer.kt           Layer 容器（app/scope 用）
    container/FxViewHolder.kt
    config/FxContent.kt
    config/FxConfig.kt                      不可变配置 + Java Builder
    config/FxConfigScope.kt                 Kotlin DSL（FxConfigScope / FxInstallScope / FxGestureScope）
    animation/FxAnimation.kt  animation/FxAnimations.kt
    feature/FxFeature.kt  feature/FxFeatureScope.kt
    feature/LocationFeature.kt  feature/GestureFeature.kt  feature/AnimationFeature.kt
    feature/ModalScrimFeature.kt
  src/test/kotlin/com/petterp/floatingx/core/...   与上面一一对应的测试
  src/test/java/com/petterp/floatingx/core/JavaApiTest.java
  src/test/resources/robolectric.properties
gradle/libs.versions.toml                   版本矩阵 A
build.gradle / settings.gradle              接入 build-logic、新模块
floatingx/build.gradle、floatingx_compose/build.gradle、app/build.gradle   仅做 Kotlin DSL 迁移保持可编译
.github/workflows/android.yml               跑 test + publishToMavenLocal
```

---

### Task 1: 工具链升级到版本矩阵 A（旧模块保持可编译）

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle`
- Modify: `gradle.properties`
- Modify: `floatingx/build.gradle`、`floatingx_compose/build.gradle`、`app/build.gradle`

**Interfaces:**
- Produces: 版本目录别名 `libs.versions.{minSdk,compileSdk,targetSdk,kotlin,agp}`、`libs.plugin.{agp,kotlin,maven.publish}`、`libs.{androidx.core,junit,robolectric,androidx.test.core,androidx.test.runner,androidx.test.ext.junit}`，Task 2 的 build-logic 依赖它们。

- [ ] **Step 1: 重写版本目录**

用下面内容整体替换 `gradle/libs.versions.toml`（旧别名保留，供旧模块与 demo 使用）：

```toml
[versions]
minSdk = "21"
compileSdk = "36"
targetSdk = "36"
jvmTarget = "17"
agp = "8.13.2"
kotlin = "2.2.21"
vanniktech-maven-publish = "0.34.0"
compose-bom = "2026.06.01"
compose-ui = "1.11.4"
androidx-core = "1.13.1"
lifecycle = "2.11.0"
savedstate = "1.5.0"
fragment = "1.8.9"
coroutines = "1.11.0"
appcompat = "1.8.0"
material = "1.14.0"
constraintlayout = "2.2.2"
leakcanary = "2.14"
codelocatorCore = "2.0.4"
junit = "4.13.2"
robolectric = "4.16.1"
androidx-test = "1.7.0"
androidx-test-ext-junit = "1.3.0"
espresso = "3.7.0"

# 旧模块 / demo 沿用的别名
simpleMinSdk = "21"
simpleComposeSdk = "36"
lifecycleRuntimeKtx = "2.11.0"

[libraries]
androidx-core = { module = "androidx.core:core", version.ref = "androidx-core" }
appcompat = { module = "androidx.appcompat:appcompat", version.ref = "appcompat" }
fragment = { module = "androidx.fragment:fragment", version.ref = "fragment" }
lifecycle-runtime = { module = "androidx.lifecycle:lifecycle-runtime", version.ref = "lifecycle" }
lifecycle-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel", version.ref = "lifecycle" }
lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
savedstate = { module = "androidx.savedstate:savedstate", version.ref = "savedstate" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
material = { module = "com.google.android.material:material", version.ref = "material" }
constraintlayout = { module = "androidx.constraintlayout:constraintlayout", version.ref = "constraintlayout" }
codelocator-core = { module = "com.bytedance.tools.codelocator:codelocator-core", version.ref = "codelocatorCore" }
leakcanary-android = { module = "com.squareup.leakcanary:leakcanary-android", version.ref = "leakcanary" }

compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-versioned = { module = "androidx.compose.ui:ui", version.ref = "compose-ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }

junit = { module = "junit:junit", version.ref = "junit" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
androidx-test-core = { module = "androidx.test:core", version.ref = "androidx-test" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidx-test" }
androidx-test-rules = { module = "androidx.test:rules", version.ref = "androidx-test" }
androidx-test-ext-junit = { module = "androidx.test.ext:junit", version.ref = "androidx-test-ext-junit" }
espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }

# build-logic 用
plugin-agp = { module = "com.android.tools.build:gradle", version.ref = "agp" }
plugin-kotlin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" }
plugin-maven-publish = { module = "com.vanniktech:gradle-maven-publish-plugin", version.ref = "vanniktech-maven-publish" }

[plugins]
vanniketch-maven-publish = { id = "com.vanniktech.maven.publish", version.ref = "vanniktech-maven-publish" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
android-lirary = { id = "com.android.library", version.ref = "agp" }
android-application = { id = "com.android.application", version.ref = "agp" }
jetbrains-kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

- [ ] **Step 2: 调整 gradle.properties 内存**

把 `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8` 改为：

```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8 -XX:+UseParallelGC
```

- [ ] **Step 3: 迁移三个旧构建脚本的 `kotlinOptions`（KGP 2.2 已将其废弃）**

`floatingx/build.gradle` 与 `floatingx_compose/build.gradle` 中，删除：

```groovy
    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }
```

并在文件末尾（`dependencies {}` 之后）追加：

```groovy
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}
```

`app/build.gradle` 同样删除 `kotlinOptions {}`，追加同一段 `kotlin { compilerOptions { ... } }`；并把 `packagingOptions {` 改名为 `packaging {`。

- [ ] **Step 4: 同步并验证构建**

Run: `./gradlew --stop && ./gradlew :app:assembleDebug :floatingx:assembleRelease --stacktrace`
Expected: `BUILD SUCCESSFUL`。若出现 `Unsupported property` / `kotlinOptions` 报错，按报错逐条删除对应旧配置；若 `vanniktech` 插件与 AGP 8.13 不兼容报错，把 `vanniktech-maven-publish` 改为 `0.37.0` 重试。

- [ ] **Step 5: 验证旧发布流程仍可用**

Run: `./gradlew publishToMavenLocal -PisPublish=false -PversionName=3.0.0-SNAPSHOT`
Expected: `BUILD SUCCESSFUL`，`~/.m2/repository/io/github/petterpx/floatingx/3.0.0-SNAPSHOT/` 存在。

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml gradle.properties build.gradle floatingx/build.gradle floatingx_compose/build.gradle app/build.gradle
git commit -m "build: 升级工具链到 AGP 8.13.2 / Kotlin 2.2.21 / compileSdk 36"
```

---

### Task 2: build-logic convention plugin + floatingx-core 骨架 + 依赖边界测试 + CI

**Files:**
- Create: `build-logic/settings.gradle.kts`、`build-logic/build.gradle.kts`、`build-logic/src/main/kotlin/floatingx.library.gradle.kts`
- Create: `floatingx-core/build.gradle.kts`、`floatingx-core/consumer-rules.pro`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/FxLogger.kt`
- Create: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/DependencyBoundaryTest.kt`
- Create: `floatingx-core/src/test/resources/robolectric.properties`
- Modify: `settings.gradle`（`pluginManagement` 里 `includeBuild`，`include ':floatingx-core'`）
- Modify: `.github/workflows/android.yml`

**Interfaces:**
- Produces: Gradle 插件 id `floatingx.library`（后续 app/system/scope/compose 模块都用它）；`FxLogger { fun d(message: () -> String); fun e(message: String, error: Throwable? = null) }`、`FxLogcatLogger(tag: String)`。

- [ ] **Step 1: 写 build-logic 三个文件**

`build-logic/settings.gradle.kts`：

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
rootProject.name = "build-logic"
```

`build-logic/build.gradle.kts`：

```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugin.agp)
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.maven.publish)
}
```

`build-logic/src/main/kotlin/floatingx.library.gradle.kts`：

```kotlin
import com.android.build.api.dsl.LibraryExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * FloatingX 库模块统一约定：
 * - minSdk 21 / compileSdk 36 / Java 17
 * - explicitApi
 * - maven-publish 坐标 io.github.petterpx:<module-name>
 * - Robolectric 单测配置
 */
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<LibraryExtension> {
    compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()
    defaultConfig {
        minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = false
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

extensions.configure<KotlinAndroidProjectExtension> {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

extensions.configure<MavenPublishBaseExtension> {
    val versionName = findProperty("versionName")?.toString()?.takeIf { it.isNotBlank() } ?: "3.0.0-SNAPSHOT"
    val isPublish = findProperty("isPublish")?.toString()?.toBoolean() ?: false
    if (isPublish) signAllPublications()
    coordinates("io.github.petterpx", project.name, versionName)
}

dependencies {
    "testImplementation"(libs.findLibrary("junit").get())
    "testImplementation"(libs.findLibrary("robolectric").get())
    "testImplementation"(libs.findLibrary("androidx-test-core").get())
    "androidTestImplementation"(libs.findLibrary("androidx-test-ext-junit").get())
    "androidTestImplementation"(libs.findLibrary("androidx-test-runner").get())
}
```

若 `jvmDefault` 在 KGP 2.2.21 中不存在（编译 build-logic 报 unresolved），改为 `freeCompilerArgs.add("-jvm-default=enable")`。

- [ ] **Step 2: 接入 settings.gradle**

在 `settings.gradle` 的 `pluginManagement {` 块**第一行**加入 `includeBuild("build-logic")`，并在 `include ':floatingx_compose'` 之后加 `include ':floatingx-core'`：

```groovy
pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        maven { url 'https://jitpack.io' }
    }
}
```

- [ ] **Step 3: 创建 core 模块**

`floatingx-core/build.gradle.kts`：

```kotlin
plugins {
    id("floatingx.library")
}

android {
    namespace = "com.petterp.floatingx.core"
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.kotlin.stdlib)
}
```

`floatingx-core/consumer-rules.pro`：

```
-dontwarn com.petterp.floatingx.**
```

`floatingx-core/src/test/resources/robolectric.properties`：

```properties
sdk=36
```

`floatingx-core/src/main/kotlin/com/petterp/floatingx/core/FxLogger.kt`：

```kotlin
package com.petterp.floatingx.core

import android.util.Log

/** 日志接口；未配置时为 null，所有调用点都要 `logger?.d { }`，保证零开销 */
public interface FxLogger {
    /** 惰性拼接，只有启用日志时才会执行 lambda */
    public fun d(message: () -> String)
    public fun e(message: String, error: Throwable? = null)
}

/** 默认 Logcat 实现，tag 形如 `Fx-<tag>` */
public class FxLogcatLogger(tag: String) : FxLogger {
    private val tag: String = if (tag.startsWith("Fx-")) tag else "Fx-$tag"
    override fun d(message: () -> String) {
        Log.d(tag, message())
    }
    override fun e(message: String, error: Throwable?) {
        Log.e(tag, message, error)
    }
}
```

- [ ] **Step 4: 写依赖边界测试（先失败：故意加一条违规 import 验证它会抓到）**

`floatingx-core/src/test/kotlin/com/petterp/floatingx/core/DependencyBoundaryTest.kt`：

```kotlin
package com.petterp.floatingx.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** core 不得依赖任何平台专属包（spec §1）。Gradle 单测的 working dir 是模块目录 */
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
    fun `core main sources import no platform specific packages`() {
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
        assertTrue("core 不允许依赖平台专属包：\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
```

为验证测试真的会抓到违规，临时新建 `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/Tmp.kt`：

```kotlin
package com.petterp.floatingx.core
import android.view.WindowManager
internal val tmp: WindowManager? = null
```

- [ ] **Step 5: 运行测试验证它会失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*DependencyBoundaryTest*"`
Expected: FAIL，消息包含 `Tmp.kt:2: import android.view.WindowManager`。

- [ ] **Step 6: 删除 Tmp.kt，测试转绿**

Run: `rm floatingx-core/src/main/kotlin/com/petterp/floatingx/core/Tmp.kt && ./gradlew :floatingx-core:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 7: 验证发布坐标**

Run: `./gradlew :floatingx-core:publishToMavenLocal -PisPublish=false -PversionName=3.0.0-SNAPSHOT`
Expected: `BUILD SUCCESSFUL`，目录 `~/.m2/repository/io/github/petterpx/floatingx-core/3.0.0-SNAPSHOT/` 下有 `.aar` 与 `.pom`，`.pom` 里 `artifactId` 为 `floatingx-core`。

- [ ] **Step 8: 更新 CI**

用下面内容替换 `.github/workflows/android.yml`：

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4
      - name: Unit tests
        run: ./gradlew test --stacktrace
      - name: Publish to maven local
        run: ./gradlew publishToMavenLocal -PisPublish=false -PversionName=3.0.0-SNAPSHOT --stacktrace
```

- [ ] **Step 9: Commit**

```bash
git add build-logic floatingx-core settings.gradle .github/workflows/android.yml
git commit -m "build: 新增 build-logic convention plugin 与 floatingx-core 骨架（含依赖边界测试）"
```

---

### Task 3: 几何类型 + FxGravity/FxAnchor + FxLayoutResolver（纯 JVM）

**Files:**
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/layout/FxGeometry.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/layout/FxGravity.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/layout/FxAnchor.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/layout/FxLayoutInput.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/layout/FxLayoutResolver.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/layout/FxLayoutResolverTest.kt`

**Interfaces:**
- Produces: `FxPoint(x, y)`、`FxSize(width, height).isValid`、`FxRect(left, top, right, bottom)`、`FxInsets`、`FxMargin`、`FxOverflow(top, bottom, left, right)`、`FxBounds(rect, insets)`、`FxGravity`（9 值，含 `horizontal/vertical`）、`FxAnchor(gravity, dx, dy)`、`FxLayoutInput(bounds, size, ltr, margin, overflow, safeArea).area`、`FxLayoutResolver.resolve/clamp/toAnchor`。后续 Task 4/10 依赖。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.core.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class FxLayoutResolverTest {

    // 1080x1920 屏，状态栏 80，导航栏 120；内容 100x200
    private val bounds = FxBounds(FxRect(0f, 0f, 1080f, 1920f), FxInsets(top = 80f, bottom = 120f))
    private val size = FxSize(100f, 200f)

    private fun input(
        ltr: Boolean = true,
        margin: FxMargin = FxMargin.NONE,
        overflow: FxOverflow = FxOverflow.NONE,
        safeArea: Boolean = true,
        size: FxSize = this.size,
    ) = FxLayoutInput(bounds, size, ltr, margin, overflow, safeArea)

    private fun assertPoint(x: Float, y: Float, actual: FxPoint) {
        assertEquals("x", x, actual.x, 0.001f)
        assertEquals("y", y, actual.y, 0.001f)
    }

    @Test
    fun `area subtracts insets and margin`() {
        val a = input(margin = FxMargin.all(16f)).area
        assertEquals(FxRect(16f, 96f, 1064f, 1784f), a)
    }

    @Test
    fun `top start lands on safe area corner`() {
        assertPoint(0f, 80f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_START), input()))
    }

    @Test
    fun `bottom end subtracts size and insets`() {
        assertPoint(980f, 1600f, FxLayoutResolver.resolve(FxAnchor(FxGravity.BOTTOM_END), input()))
    }

    @Test
    fun `center centers within area`() {
        assertPoint(490f, 840f, FxLayoutResolver.resolve(FxAnchor(FxGravity.CENTER), input()))
    }

    @Test
    fun `dx dy offset inward from the anchored edge`() {
        assertPoint(10f, 100f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_START, 10f, 20f), input()))
        assertPoint(970f, 1580f, FxLayoutResolver.resolve(FxAnchor(FxGravity.BOTTOM_END, 10f, 20f), input()))
    }

    @Test
    fun `margin shrinks area`() {
        assertPoint(16f, 96f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_START), input(margin = FxMargin.all(16f))))
    }

    @Test
    fun `safeArea false ignores insets`() {
        assertPoint(0f, 0f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_START), input(safeArea = false)))
    }

    @Test
    fun `rtl swaps start and end`() {
        assertPoint(980f, 80f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_START), input(ltr = false)))
        assertPoint(0f, 80f, FxLayoutResolver.resolve(FxAnchor(FxGravity.TOP_END), input(ltr = false)))
    }

    @Test
    fun `clamp keeps point inside area`() {
        assertPoint(0f, 1600f, FxLayoutResolver.clamp(FxPoint(-50f, 5000f), input()))
    }

    @Test
    fun `clamp with overflow allows leaving that side`() {
        assertPoint(0f, -300f, FxLayoutResolver.clamp(FxPoint(-50f, -300f), input(overflow = FxOverflow(top = true))))
        assertPoint(-50f, 80f, FxLayoutResolver.clamp(FxPoint(-50f, -300f), input(overflow = FxOverflow(left = true))))
    }

    @Test
    fun `content wider than area aligns to start`() {
        assertPoint(0f, 80f, FxLayoutResolver.resolve(FxAnchor(FxGravity.CENTER_END), input(size = FxSize(2000f, 200f))))
    }

    @Test
    fun `toAnchor picks nearest horizontal and vertical edge`() {
        val anchor = FxLayoutResolver.toAnchor(FxPoint(900f, 100f), input())
        assertEquals(FxGravity.TOP_END, anchor.gravity)
        assertEquals(80f, anchor.dx, 0.001f)
        assertEquals(20f, anchor.dy, 0.001f)
    }

    @Test
    fun `toAnchor in rtl maps physical right edge to START`() {
        val anchor = FxLayoutResolver.toAnchor(FxPoint(900f, 100f), input(ltr = false))
        assertEquals(FxGravity.TOP_START, anchor.gravity)
        assertEquals(80f, anchor.dx, 0.001f)
    }

    @Test
    fun `resolve then toAnchor round trips for edge anchors`() {
        listOf(FxGravity.TOP_START, FxGravity.TOP_END, FxGravity.BOTTOM_START, FxGravity.BOTTOM_END).forEach { g ->
            listOf(true, false).forEach { ltr ->
                val anchor = FxAnchor(g, 30f, 40f)
                val back = FxLayoutResolver.toAnchor(FxLayoutResolver.resolve(anchor, input(ltr = ltr)), input(ltr = ltr))
                assertEquals("gravity ltr=$ltr", g, back.gravity)
                assertEquals(30f, back.dx, 0.001f)
                assertEquals(40f, back.dy, 0.001f)
            }
        }
    }
}
```

- [ ] **Step 2: 运行，确认编译失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxLayoutResolverTest*"`
Expected: 编译错误 `Unresolved reference: FxBounds` 等。

- [ ] **Step 3: 实现几何类型与解析器**

`layout/FxGeometry.kt`：

```kotlin
package com.petterp.floatingx.core.layout

/*
 * 纯 Kotlin 几何类型。刻意不用 android.graphics.*，
 * 使 FxLayoutResolver / FxAdsorbResolver 可以在纯 JVM 里测试。
 */

public data class FxPoint(val x: Float, val y: Float) {
    public companion object {
        @JvmField public val ZERO: FxPoint = FxPoint(0f, 0f)
    }
}

public data class FxSize(val width: Float, val height: Float) {
    /** 宽高都大于 0 才可用于定位；内容尚未测量时为 false */
    public val isValid: Boolean get() = width > 0f && height > 0f

    public companion object {
        @JvmField public val EMPTY: FxSize = FxSize(0f, 0f)
    }
}

public data class FxRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    public val width: Float get() = right - left
    public val height: Float get() = bottom - top
    public val centerX: Float get() = (left + right) / 2f
    public val centerY: Float get() = (top + bottom) / 2f

    public fun inset(l: Float, t: Float, r: Float, b: Float): FxRect = FxRect(left + l, top + t, right - r, bottom - b)
    public fun inset(insets: FxInsets): FxRect = inset(insets.left, insets.top, insets.right, insets.bottom)
}

/** safe area（状态栏 / 导航栏 / 刘海）四边 */
public data class FxInsets(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f) {
    public companion object {
        @JvmField public val NONE: FxInsets = FxInsets()
    }
}

/** 用户配置的四边留白 */
public data class FxMargin(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f) {
    public companion object {
        @JvmField public val NONE: FxMargin = FxMargin()
        @JvmStatic public fun all(value: Float): FxMargin = FxMargin(value, value, value, value)
    }
}

/** 允许内容超出可用区的哪些边（#235） */
public data class FxOverflow(val top: Boolean = false, val bottom: Boolean = false, val left: Boolean = false, val right: Boolean = false) {
    public companion object {
        @JvmField public val NONE: FxOverflow = FxOverflow()
        @JvmField public val ALL: FxOverflow = FxOverflow(top = true, bottom = true, left = true, right = true)
    }
}

/** host 提供的父区域：rect 为容器整体，insets 为其中的 safe area */
public data class FxBounds(val rect: FxRect, val insets: FxInsets = FxInsets.NONE)
```

`layout/FxGravity.kt`：

```kotlin
package com.petterp.floatingx.core.layout

public enum class FxHorizontal { START, CENTER, END }
public enum class FxVertical { TOP, CENTER, BOTTOM }

/** 锚点所依附的边/角。START/END 为逻辑方向，RTL 时由 ltr 参数解析 */
public enum class FxGravity(public val horizontal: FxHorizontal, public val vertical: FxVertical) {
    TOP_START(FxHorizontal.START, FxVertical.TOP),
    TOP_CENTER(FxHorizontal.CENTER, FxVertical.TOP),
    TOP_END(FxHorizontal.END, FxVertical.TOP),
    CENTER_START(FxHorizontal.START, FxVertical.CENTER),
    CENTER(FxHorizontal.CENTER, FxVertical.CENTER),
    CENTER_END(FxHorizontal.END, FxVertical.CENTER),
    BOTTOM_START(FxHorizontal.START, FxVertical.BOTTOM),
    BOTTOM_CENTER(FxHorizontal.CENTER, FxVertical.BOTTOM),
    BOTTOM_END(FxHorizontal.END, FxVertical.BOTTOM);

    public companion object {
        @JvmStatic
        public fun of(horizontal: FxHorizontal, vertical: FxVertical): FxGravity =
            entries.first { it.horizontal == horizontal && it.vertical == vertical }
    }
}
```

`layout/FxAnchor.kt`：

```kotlin
package com.petterp.floatingx.core.layout

/**
 * 位置的唯一真值：依附的边/角 + 从该边向内的偏移。
 * START → x = area.left + dx；END → x = area.right - w - dx；CENTER → x = centerX - w/2 + dx。
 */
public data class FxAnchor(val gravity: FxGravity, val dx: Float = 0f, val dy: Float = 0f) {
    public companion object {
        @JvmField public val DEFAULT: FxAnchor = FxAnchor(FxGravity.TOP_START)
    }
}
```

`layout/FxLayoutInput.kt`：

```kotlin
package com.petterp.floatingx.core.layout

/** 一次定位需要的全部输入 */
public data class FxLayoutInput(
    val bounds: FxBounds,
    val size: FxSize,
    val ltr: Boolean = true,
    val margin: FxMargin = FxMargin.NONE,
    val overflow: FxOverflow = FxOverflow.NONE,
    val safeArea: Boolean = true,
) {
    /** 可用区：bounds 扣掉 insets（safeArea 时）再扣 margin */
    public val area: FxRect = (if (safeArea) bounds.rect.inset(bounds.insets) else bounds.rect)
        .inset(margin.left, margin.top, margin.right, margin.bottom)
}
```

`layout/FxLayoutResolver.kt`：

```kotlin
package com.petterp.floatingx.core.layout

/** 锚点 ↔ 左上角坐标 的纯函数集合，无任何 Android 依赖 */
public object FxLayoutResolver {

    /** 锚点 → 内容左上角坐标（已 clamp） */
    @JvmStatic
    public fun resolve(anchor: FxAnchor, input: FxLayoutInput): FxPoint {
        val a = input.area
        val w = input.size.width
        val h = input.size.height
        val x = when (physical(anchor.gravity.horizontal, input.ltr)) {
            FxHorizontal.START -> a.left + anchor.dx
            FxHorizontal.END -> a.right - w - anchor.dx
            FxHorizontal.CENTER -> a.centerX - w / 2f + anchor.dx
        }
        val y = when (anchor.gravity.vertical) {
            FxVertical.TOP -> a.top + anchor.dy
            FxVertical.BOTTOM -> a.bottom - h - anchor.dy
            FxVertical.CENTER -> a.centerY - h / 2f + anchor.dy
        }
        return clamp(FxPoint(x, y), input)
    }

    /** 钳制到可用区；overflow 打开的边不设限 */
    @JvmStatic
    public fun clamp(point: FxPoint, input: FxLayoutInput): FxPoint {
        val a = input.area
        val w = input.size.width
        val h = input.size.height
        val minX = if (input.overflow.left) Float.NEGATIVE_INFINITY else a.left
        val maxX = if (input.overflow.right) Float.POSITIVE_INFINITY else a.right - w
        val minY = if (input.overflow.top) Float.NEGATIVE_INFINITY else a.top
        val maxY = if (input.overflow.bottom) Float.POSITIVE_INFINITY else a.bottom - h
        return FxPoint(clampAxis(point.x, minX, maxX, a.left), clampAxis(point.y, minY, maxY, a.top))
    }

    /** 左上角坐标 → 最近的物理边组合（左/右 × 上/下）对应的逻辑锚点；拖动结束后调用 */
    @JvmStatic
    public fun toAnchor(point: FxPoint, input: FxLayoutInput): FxAnchor {
        val a = input.area
        val w = input.size.width
        val h = input.size.height
        val distLeft = point.x - a.left
        val distRight = (a.right - w) - point.x
        val nearLeft = distLeft <= distRight
        val dx = if (nearLeft) distLeft else distRight
        val distTop = point.y - a.top
        val distBottom = (a.bottom - h) - point.y
        val nearTop = distTop <= distBottom
        val dy = if (nearTop) distTop else distBottom
        val horizontal = physical(if (nearLeft) FxHorizontal.START else FxHorizontal.END, input.ltr)
        val vertical = if (nearTop) FxVertical.TOP else FxVertical.BOTTOM
        return FxAnchor(FxGravity.of(horizontal, vertical), dx, dy)
    }

    /** 逻辑 START/END ↔ 物理左/右；LTR 恒等，RTL 互换。该映射自反，两个方向都用它 */
    private fun physical(h: FxHorizontal, ltr: Boolean): FxHorizontal = if (ltr) h else when (h) {
        FxHorizontal.START -> FxHorizontal.END
        FxHorizontal.END -> FxHorizontal.START
        FxHorizontal.CENTER -> FxHorizontal.CENTER
    }

    /** 内容比可用区还大（max < min）时靠 start 对齐，避免 coerceIn 抛异常 */
    private fun clampAxis(value: Float, min: Float, max: Float, fallback: Float): Float =
        if (max < min) fallback else value.coerceIn(min, max)
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxLayoutResolverTest*"`
Expected: 15 tests PASS。

- [ ] **Step 5: Commit**

```bash
git add floatingx-core/src
git commit -m "feat(core): 几何类型、FxGravity/FxAnchor 与锚点定位解析器"
```

---

### Task 4: 吸附策略 FxAdsorb + FxAdsorbResolver（纯 JVM）

**Files:**
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/layout/FxAdsorb.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/layout/FxAdsorbResolver.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/layout/FxAdsorbResolverTest.kt`

**Interfaces:**
- Consumes: Task 3 的 `FxPoint/FxLayoutInput/FxLayoutResolver.clamp`。
- Produces: `FxEdge { START, END, TOP, BOTTOM }`、`FxHalfHide(start, end)`、`FxAdsorb.None` / `FxAdsorb.Edges(edges, halfHide, rebound)` + 工厂 `none()/horizontal()/vertical()/all()`、`FxAdsorbResolver.target(point, input, adsorb): FxPoint`。Task 10 的 `LocationFeature.onDragEnd` 依赖。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.core.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FxAdsorbResolverTest {

    private val bounds = FxBounds(FxRect(0f, 0f, 1080f, 1920f), FxInsets(top = 80f, bottom = 120f))
    private val size = FxSize(100f, 200f)
    private fun input(ltr: Boolean = true) = FxLayoutInput(bounds, size, ltr)

    private fun assertPoint(x: Float, y: Float, actual: FxPoint) {
        assertEquals("x", x, actual.x, 0.001f)
        assertEquals("y", y, actual.y, 0.001f)
    }

    @Test
    fun `none only clamps`() {
        assertPoint(0f, 500f, FxAdsorbResolver.target(FxPoint(-50f, 500f), input(), FxAdsorb.none()))
    }

    @Test
    fun `horizontal snaps to nearest side keeping y`() {
        assertPoint(0f, 500f, FxAdsorbResolver.target(FxPoint(200f, 500f), input(), FxAdsorb.horizontal()))
        assertPoint(980f, 500f, FxAdsorbResolver.target(FxPoint(900f, 500f), input(), FxAdsorb.horizontal()))
    }

    @Test
    fun `half hide uses independent start and end ratios`() {
        val adsorb = FxAdsorb.horizontal(FxHalfHide(start = 0.2f, end = 0.8f))
        assertPoint(-20f, 500f, FxAdsorbResolver.target(FxPoint(200f, 500f), input(), adsorb))
        assertPoint(1060f, 500f, FxAdsorbResolver.target(FxPoint(900f, 500f), input(), adsorb))
    }

    @Test
    fun `vertical snaps to nearest top or bottom keeping x`() {
        assertPoint(500f, 80f, FxAdsorbResolver.target(FxPoint(500f, 100f), input(), FxAdsorb.vertical()))
        assertPoint(500f, 1600f, FxAdsorbResolver.target(FxPoint(500f, 1500f), input(), FxAdsorb.vertical()))
    }

    @Test
    fun `all picks the globally nearest edge`() {
        assertPoint(500f, 80f, FxAdsorbResolver.target(FxPoint(500f, 100f), input(), FxAdsorb.all()))
        assertPoint(980f, 900f, FxAdsorbResolver.target(FxPoint(950f, 900f), input(), FxAdsorb.all()))
    }

    @Test
    fun `rtl applies end ratio to the physical left side`() {
        val adsorb = FxAdsorb.horizontal(FxHalfHide(start = 0.2f, end = 0.8f))
        assertPoint(-80f, 500f, FxAdsorbResolver.target(FxPoint(200f, 500f), input(ltr = false), adsorb))
    }

    @Test
    fun `only enabled edges are considered`() {
        val onlyStart = FxAdsorb.Edges(setOf(FxEdge.START))
        assertPoint(0f, 500f, FxAdsorbResolver.target(FxPoint(900f, 500f), input(), onlyStart))
    }

    @Test
    fun `point outside area is clamped before snapping`() {
        assertPoint(980f, 1600f, FxAdsorbResolver.target(FxPoint(2000f, 3000f), input(), FxAdsorb.horizontal()))
    }

    @Test
    fun `half hide ratio must be within 0 and 1`() {
        assertThrows(IllegalArgumentException::class.java) { FxHalfHide(1.5f) }
    }
}
```

- [ ] **Step 2: 运行确认编译失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxAdsorbResolverTest*"`
Expected: `Unresolved reference: FxAdsorb`。

- [ ] **Step 3: 实现**

`layout/FxAdsorb.kt`：

```kotlin
package com.petterp.floatingx.core.layout

/** 可吸附的逻辑边 */
public enum class FxEdge { START, END, TOP, BOTTOM }

/** 半隐比例：贴 START 边时隐藏 start 比例，贴 END 边时隐藏 end 比例（#204 左右可不同） */
public data class FxHalfHide(val start: Float, val end: Float = start) {
    init {
        require(start in 0f..1f && end in 0f..1f) { "halfHide 比例必须在 0..1 之间: start=$start end=$end" }
    }
}

/** 拖动结束后的吸附策略 */
public sealed class FxAdsorb {

    public object None : FxAdsorb()

    /**
     * @param edges 允许吸附的边
     * @param halfHide 贴左右边时的半隐比例，null 表示不半隐
     * @param rebound 拖动过程中允许暂时超出可用区、松手后回弹
     */
    public data class Edges(
        val edges: Set<FxEdge>,
        val halfHide: FxHalfHide? = null,
        val rebound: Boolean = true,
    ) : FxAdsorb()

    public companion object {
        @JvmStatic public fun none(): FxAdsorb = None

        @JvmStatic @JvmOverloads
        public fun horizontal(halfHide: FxHalfHide? = null, rebound: Boolean = true): FxAdsorb =
            Edges(setOf(FxEdge.START, FxEdge.END), halfHide, rebound)

        @JvmStatic @JvmOverloads
        public fun vertical(rebound: Boolean = true): FxAdsorb =
            Edges(setOf(FxEdge.TOP, FxEdge.BOTTOM), null, rebound)

        @JvmStatic @JvmOverloads
        public fun all(halfHide: FxHalfHide? = null, rebound: Boolean = true): FxAdsorb =
            Edges(FxEdge.entries.toSet(), halfHide, rebound)
    }
}
```

`layout/FxAdsorbResolver.kt`：

```kotlin
package com.petterp.floatingx.core.layout

/** 吸附目标点的纯函数 */
public object FxAdsorbResolver {

    /** 拖动结束后的停靠点。None → 仅 clamp */
    @JvmStatic
    public fun target(point: FxPoint, input: FxLayoutInput, adsorb: FxAdsorb): FxPoint {
        val p = FxLayoutResolver.clamp(point, input)
        val edges = adsorb as? FxAdsorb.Edges ?: return p
        val edge = nearestEdge(p, input, edges.edges) ?: return p
        val a = input.area
        val w = input.size.width
        val h = input.size.height
        val hide = edges.halfHide
        return when (edge) {
            FxEdge.START -> p.copy(x = if (input.ltr) a.left - w * (hide?.start ?: 0f) else a.right - w + w * (hide?.start ?: 0f))
            FxEdge.END -> p.copy(x = if (input.ltr) a.right - w + w * (hide?.end ?: 0f) else a.left - w * (hide?.end ?: 0f))
            FxEdge.TOP -> p.copy(y = a.top)
            FxEdge.BOTTOM -> p.copy(y = a.bottom - h)
        }
    }

    /** 距离最近的启用边（逻辑边，已考虑 ltr）；edges 为空返回 null */
    @JvmStatic
    public fun nearestEdge(point: FxPoint, input: FxLayoutInput, edges: Set<FxEdge>): FxEdge? {
        if (edges.isEmpty()) return null
        val a = input.area
        val distLeft = point.x - a.left
        val distRight = (a.right - input.size.width) - point.x
        var best: FxEdge? = null
        var bestDist = Float.MAX_VALUE
        for (edge in edges) {
            val d = when (edge) {
                FxEdge.START -> if (input.ltr) distLeft else distRight
                FxEdge.END -> if (input.ltr) distRight else distLeft
                FxEdge.TOP -> point.y - a.top
                FxEdge.BOTTOM -> (a.bottom - input.size.height) - point.y
            }
            if (d < bestDist) { bestDist = d; best = edge }
        }
        return best
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxAdsorbResolverTest*"`
Expected: 9 tests PASS。

- [ ] **Step 5: Commit**

```bash
git add floatingx-core/src
git commit -m "feat(core): 吸附策略 FxAdsorb 与半隐比例解析"
```

---

### Task 5: 持久化 FxStorage + FxSpStorage（Robolectric）

**Files:**
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/storage/FxStorage.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/storage/FxSpStorage.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/storage/FxSpStorageTest.kt`

**Interfaces:**
- Produces: `FxStorage { save(key, anchor); load(key): FxAnchor?; clear(key) }`、`FxSpStorage(context, name = "floatingx_anchor")`。Task 9 的 `FxConfig.storage`、Task 10 的 `commitAnchor` 依赖。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.core.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxSpStorageTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val storage = FxSpStorage(context, "test_fx")

    @Test
    fun `save then load round trips`() {
        val anchor = FxAnchor(FxGravity.BOTTOM_END, 12.5f, -3f)
        storage.save("tag:1", anchor)
        assertEquals(anchor, storage.load("tag:1"))
    }

    @Test
    fun `missing key returns null`() {
        assertNull(storage.load("nope"))
    }

    @Test
    fun `corrupt value returns null`() {
        context.getSharedPreferences("test_fx", Context.MODE_PRIVATE).edit().putString("bad", "garbage").commit()
        assertNull(storage.load("bad"))
    }

    @Test
    fun `clear removes key`() {
        storage.save("k", FxAnchor(FxGravity.CENTER))
        storage.clear("k")
        assertNull(storage.load("k"))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxSpStorageTest*"`
Expected: `Unresolved reference: FxSpStorage`。

- [ ] **Step 3: 实现**

`storage/FxStorage.kt`：

```kotlin
package com.petterp.floatingx.core.storage

import com.petterp.floatingx.core.layout.FxAnchor

/**
 * 锚点持久化。key 由框架生成（`"$tag:$orientation"`），横竖屏分别记忆。
 * 写入点只有三个：拖动/吸附结束、moveTo 完成、update { anchor }。
 */
public interface FxStorage {
    public fun save(key: String, anchor: FxAnchor)
    public fun load(key: String): FxAnchor?
    public fun clear(key: String)
}
```

`storage/FxSpStorage.kt`：

```kotlin
package com.petterp.floatingx.core.storage

import android.content.Context
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity

/** 基于 SharedPreferences 的默认实现，值格式 `GRAVITY,dx,dy` */
public class FxSpStorage @JvmOverloads constructor(
    context: Context,
    name: String = "floatingx_anchor",
) : FxStorage {

    private val sp = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun save(key: String, anchor: FxAnchor) {
        sp.edit().putString(key, "${anchor.gravity.name},${anchor.dx},${anchor.dy}").apply()
    }

    override fun load(key: String): FxAnchor? {
        val raw = sp.getString(key, null) ?: return null
        val parts = raw.split(',')
        if (parts.size != 3) return null
        val gravity = runCatching { FxGravity.valueOf(parts[0]) }.getOrNull() ?: return null
        val dx = parts[1].toFloatOrNull() ?: return null
        val dy = parts[2].toFloatOrNull() ?: return null
        return FxAnchor(gravity, dx, dy)
    }

    override fun clear(key: String) {
        sp.edit().remove(key).apply()
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxSpStorageTest*"`
Expected: 4 tests PASS。

- [ ] **Step 5: Commit**

```bash
git add floatingx-core/src
git commit -m "feat(core): FxStorage 持久化接口与 SharedPreferences 实现"
```

---

### Task 6: 状态机 FxEngine + Host / Container / Session 接口（纯 JVM）

**Files:**
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/FxState.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/host/FxHost.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/host/FxHostSession.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/host/FxLayoutSpec.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/container/FxContainer.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/engine/FxCommand.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/engine/FxEngineDelegate.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/engine/FxEngine.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/engine/FxEngineTest.kt`

**Interfaces:**
- Consumes: Task 3 的 `FxBounds/FxGravity/FxPoint/FxSize`。
- Produces（公开）：`FxState { INSTALLED, ATTACHED, SHOWN, CANCELLED }`；`FxHost { context; bind(session); createContainer(); attach(c); detach(c); updateLayout(c, spec)（默认实现 = c.setContentPosition）; bounds(); release() }`；`FxHostSession { onHostReady(); onHostLost(); onBoundsChanged(); requestSwap(fallback) }`；`FxLayoutSpec(x, y, gravity, ltr)`；`FxContainer`（见代码）与 `FxContainerTouchHandler { onIntercept(ev); onTouch(ev) }`。
- Produces（internal）：`FxCommand.MoveTo(x, y, animate)` / `FxCommand.MoveBy(dx, dy, animate)`；`FxEngineDelegate { performAttach(); performDetach(); performShow(); performHide(); perform(command); onBoundsChanged(); swapHost(fallback); onStateChanged(old, new) }`；`FxEngine(delegate) : FxHostSession { state; desiredVisible; hostReady; show(); hide(); dispatch(command); cancel() }`。
- 语义（spec §2.2）：未 ready 时 `show/hide` 只记 `desiredVisible`，`MoveTo/MoveBy` 入队；`onHostReady` → attach → 回放队列 → 若 desiredVisible 则 show；`onHostLost` → detach 回到 INSTALLED 但保留 desiredVisible；`cancel` 清队列、终态。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.core.engine

import android.content.Context
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FxEngineTest {

    private class FakeDelegate : FxEngineDelegate {
        val calls = mutableListOf<String>()
        val transitions = mutableListOf<String>()
        var swappedTo: FxHost? = null
        override fun performAttach() { calls += "attach" }
        override fun performDetach() { calls += "detach" }
        override fun performShow() { calls += "show" }
        override fun performHide() { calls += "hide" }
        override fun perform(command: FxCommand) { calls += "perform:$command" }
        override fun onBoundsChanged() { calls += "bounds" }
        override fun swapHost(fallback: FxHost) { swappedTo = fallback }
        override fun onStateChanged(old: FxState, new: FxState) { transitions += "$old->$new" }
    }

    private val stubHost = object : FxHost {
        override val context: Context get() = error("unused")
        override fun bind(session: FxHostSession) = Unit
        override fun createContainer(): FxContainer = error("unused")
        override fun attach(container: FxContainer) = Unit
        override fun detach(container: FxContainer) = Unit
        override fun bounds(): FxBounds = error("unused")
        override fun release() = Unit
    }

    private val delegate = FakeDelegate()
    private val engine = FxEngine(delegate)

    @Test
    fun `show before host ready only records desire`() {
        engine.show()
        assertEquals(FxState.INSTALLED, engine.state)
        assertTrue(engine.desiredVisible)
        assertTrue(delegate.calls.isEmpty())
    }

    @Test
    fun `host ready attaches then shows when desired`() {
        engine.show()
        engine.onHostReady()
        assertEquals(listOf("attach", "show"), delegate.calls)
        assertEquals(FxState.SHOWN, engine.state)
        assertEquals(listOf("INSTALLED->ATTACHED", "ATTACHED->SHOWN"), delegate.transitions)
    }

    @Test
    fun `host ready without show only attaches`() {
        engine.onHostReady()
        assertEquals(listOf("attach"), delegate.calls)
        assertEquals(FxState.ATTACHED, engine.state)
    }

    @Test
    fun `commands queued before ready replay in order before show`() {
        val move = FxCommand.MoveTo(10f, 20f, animate = false)
        val by = FxCommand.MoveBy(1f, 1f, animate = true)
        engine.dispatch(move)
        engine.show()
        engine.dispatch(by)
        engine.onHostReady()
        assertEquals(listOf("attach", "perform:$move", "perform:$by", "show"), delegate.calls)
    }

    @Test
    fun `commands after ready execute immediately`() {
        engine.onHostReady()
        val move = FxCommand.MoveTo(1f, 2f, animate = true)
        engine.dispatch(move)
        assertEquals(listOf("attach", "perform:$move"), delegate.calls)
    }

    @Test
    fun `host lost keeps desired visibility and restores on ready`() {
        engine.show(); engine.onHostReady()
        engine.onHostLost()
        assertEquals(FxState.INSTALLED, engine.state)
        assertTrue(engine.desiredVisible)
        assertEquals("detach", delegate.calls.last())
        engine.onHostReady()
        assertEquals(listOf("attach", "show", "detach", "attach", "show"), delegate.calls)
        assertEquals(FxState.SHOWN, engine.state)
    }

    @Test
    fun `hide from shown goes back to attached`() {
        engine.show(); engine.onHostReady()
        engine.hide()
        assertEquals(FxState.ATTACHED, engine.state)
        assertFalse(engine.desiredVisible)
        assertEquals("hide", delegate.calls.last())
        engine.onHostLost(); engine.onHostReady()
        assertEquals(FxState.ATTACHED, engine.state)
        assertEquals(listOf("attach", "show", "hide", "detach", "attach"), delegate.calls)
    }

    @Test
    fun `hide before ready is a no-op besides desire`() {
        engine.show(); engine.hide()
        engine.onHostReady()
        assertEquals(listOf("attach"), delegate.calls)
    }

    @Test
    fun `bounds changed only forwarded while attached`() {
        engine.onBoundsChanged()
        assertTrue(delegate.calls.isEmpty())
        engine.onHostReady()
        engine.onBoundsChanged()
        assertEquals(listOf("attach", "bounds"), delegate.calls)
    }

    @Test
    fun `cancel detaches clears queue and is terminal`() {
        engine.dispatch(FxCommand.MoveTo(0f, 0f, false))
        engine.show(); engine.onHostReady()
        engine.cancel()
        assertEquals(FxState.CANCELLED, engine.state)
        assertEquals("detach", delegate.calls.last())
        assertEquals("SHOWN->CANCELLED", delegate.transitions.last())
        assertThrows(IllegalStateException::class.java) { engine.show() }
        assertThrows(IllegalStateException::class.java) { engine.dispatch(FxCommand.MoveBy(1f, 1f, false)) }
        engine.onHostReady()   // 终态后 host 事件被忽略
        assertEquals(FxState.CANCELLED, engine.state)
    }

    @Test
    fun `cancel while installed does not detach`() {
        engine.cancel()
        assertTrue(delegate.calls.isEmpty())
        assertEquals(listOf("INSTALLED->CANCELLED"), delegate.transitions)
    }

    @Test
    fun `request swap detaches and hands over fallback`() {
        engine.show(); engine.onHostReady()
        engine.requestSwap(stubHost)
        assertEquals(FxState.INSTALLED, engine.state)
        assertTrue(engine.desiredVisible)
        assertEquals(stubHost, delegate.swappedTo)
        assertEquals("detach", delegate.calls.last())
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxEngineTest*"`
Expected: `Unresolved reference: FxEngine`。

- [ ] **Step 3: 实现接口与状态机**

`FxState.kt`：

```kotlin
package com.petterp.floatingx.core

/** 浮窗生命周期状态（spec §2.2） */
public enum class FxState {
    /** 已创建，容器尚未挂到任何 host（或 host 暂不可用） */
    INSTALLED,
    /** 容器已挂载但内容不可见 */
    ATTACHED,
    /** 内容可见 */
    SHOWN,
    /** 已销毁，终态 */
    CANCELLED,
}
```

`host/FxLayoutSpec.kt`：

```kotlin
package com.petterp.floatingx.core.host

import com.petterp.floatingx.core.layout.FxGravity

/**
 * 一次布局提交：内容左上角坐标 + 当前锚点的 gravity + 布局方向。
 * Layer 容器只用 x/y；Window 容器可把 gravity 映射到 LayoutParams.gravity（spec §2.3）。
 */
public data class FxLayoutSpec(val x: Float, val y: Float, val gravity: FxGravity, val ltr: Boolean)
```

`host/FxHostSession.kt`：

```kotlin
package com.petterp.floatingx.core.host

/** host → engine 的事件通道 */
public interface FxHostSession {
    /** 已有可挂载的父容器且尺寸有效 */
    public fun onHostReady()
    /** 父容器消失（Activity destroy / ViewGroup detach / 权限撤销） */
    public fun onHostLost()
    /** 旋转、insets、分屏等导致可用区变化 */
    public fun onBoundsChanged()
    /** 当前 host 不可用且有替代方案（如 system 权限被拒降级到 app） */
    public fun requestSwap(fallback: FxHost)
}
```

`host/FxHost.kt`：

```kotlin
package com.petterp.floatingx.core.host

import android.content.Context
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.layout.FxBounds

/**
 * 谁来承载浮窗（spec §2.1）。app / system / scope 模块各自实现。
 * 实现约定：bind() 之后一旦具备挂载条件必须回调 session.onHostReady()，
 * 失去条件回调 onHostLost()；release() 后不得再回调 session。
 */
public interface FxHost {
    /** 创建容器与内容 view 所用的 context */
    public val context: Context

    public fun bind(session: FxHostSession)

    /** 由 host 决定容器形态：Layer（覆盖层）或 Window（WindowManager 窗口） */
    public fun createContainer(): FxContainer

    public fun attach(container: FxContainer)

    public fun detach(container: FxContainer)

    /** 应用一次布局。Layer 容器默认只改内容坐标；Window 容器覆写为写 LayoutParams */
    public fun updateLayout(container: FxContainer, spec: FxLayoutSpec) {
        container.setContentPosition(spec.x, spec.y)
    }

    /** 当前可用区域与 safe area insets */
    public fun bounds(): FxBounds

    /** control cancel 或 swap 时调用，释放监听/回调 */
    public fun release()
}
```

`container/FxContainer.kt`：

```kotlin
package com.petterp.floatingx.core.container

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.petterp.floatingx.core.layout.FxPoint
import com.petterp.floatingx.core.layout.FxSize

/** 容器把触摸事件交给它（GestureFeature 实现） */
public interface FxContainerTouchHandler {
    public fun onIntercept(ev: MotionEvent): Boolean
    public fun onTouch(ev: MotionEvent): Boolean
}

/**
 * 承载用户内容 view 的容器抽象。两种实现：
 * - FxLayerContainer：match_parent 覆盖层，内容用 translation 定位（app / scope）
 * - Window 容器（system 模块）：wrap_content 窗口，坐标写回 LayoutParams
 */
public interface FxContainer {
    /** 真正被加到 host 上的 View */
    public val view: ViewGroup
    public val contentView: View?
    public val isLtr: Boolean

    public fun setContent(view: View)
    public fun contentSize(): FxSize
    public fun setContentPosition(x: Float, y: Float)
    /** 内容相对容器的坐标 */
    public fun contentPosition(): FxPoint
    /** 内容的屏幕坐标 */
    public fun contentPositionOnScreen(): FxPoint
    public fun setContentVisible(visible: Boolean)
    /** 容器坐标系下的点是否落在内容上 */
    public fun hitTest(x: Float, y: Float): Boolean

    public var touchHandler: FxContainerTouchHandler?
    /** 内容 view 宽高变化（仅在尺寸真的变了时回调） */
    public var onContentSizeChanged: ((FxSize) -> Unit)?
    /** 容器自身尺寸变化 */
    public var onBoundsChanged: (() -> Unit)?
}
```

`engine/FxCommand.kt`：

```kotlin
package com.petterp.floatingx.core.engine

/** 需要容器已挂载才能执行的命令；未 ready 时由 FxEngine 排队 */
internal sealed class FxCommand {
    data class MoveTo(val x: Float, val y: Float, val animate: Boolean) : FxCommand()
    data class MoveBy(val dx: Float, val dy: Float, val animate: Boolean) : FxCommand()
}
```

`engine/FxEngineDelegate.kt`：

```kotlin
package com.petterp.floatingx.core.engine

import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.host.FxHost

/** FxEngine 只做状态与排队，所有 View 相关动作通过它回调给 FxControlImpl */
internal interface FxEngineDelegate {
    fun performAttach()
    fun performDetach()
    fun performShow()
    fun performHide()
    fun perform(command: FxCommand)
    fun onBoundsChanged()
    fun swapHost(fallback: FxHost)
    fun onStateChanged(old: FxState, new: FxState)
}
```

`engine/FxEngine.kt`：

```kotlin
package com.petterp.floatingx.core.engine

import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession

/**
 * 显示状态机 + 命令队列（spec §2.2）。
 *
 *   INSTALLED --ready--> ATTACHED --show--> SHOWN
 *       ^                   |  ^              |
 *       +----- hostLost ----+  +---- hide ----+
 *   任意 --cancel--> CANCELLED
 */
internal class FxEngine(private val delegate: FxEngineDelegate) : FxHostSession {

    var state: FxState = FxState.INSTALLED
        private set

    /** 用户意图：最近一次是 show 还是 hide。hostLost 不改它，ready 后按它恢复 */
    var desiredVisible: Boolean = false
        private set

    var hostReady: Boolean = false
        private set

    private val pending = ArrayDeque<FxCommand>()

    fun show() {
        checkAlive()
        desiredVisible = true
        when (state) {
            FxState.INSTALLED -> if (hostReady) attachAndRestore()
            FxState.ATTACHED -> transition(FxState.SHOWN) { delegate.performShow() }
            FxState.SHOWN, FxState.CANCELLED -> Unit
        }
    }

    fun hide() {
        checkAlive()
        desiredVisible = false
        if (state == FxState.SHOWN) transition(FxState.ATTACHED) { delegate.performHide() }
    }

    fun dispatch(command: FxCommand) {
        checkAlive()
        if (state == FxState.INSTALLED) pending.addLast(command) else delegate.perform(command)
    }

    fun cancel() {
        if (state == FxState.CANCELLED) return
        pending.clear()
        val old = state
        if (old != FxState.INSTALLED) delegate.performDetach()
        state = FxState.CANCELLED
        delegate.onStateChanged(old, state)
    }

    override fun onHostReady() {
        if (state == FxState.CANCELLED) return
        hostReady = true
        if (state == FxState.INSTALLED) attachAndRestore()
    }

    override fun onHostLost() {
        hostReady = false
        if (state == FxState.ATTACHED || state == FxState.SHOWN) transition(FxState.INSTALLED) { delegate.performDetach() }
    }

    override fun onBoundsChanged() {
        if (state == FxState.ATTACHED || state == FxState.SHOWN) delegate.onBoundsChanged()
    }

    override fun requestSwap(fallback: FxHost) {
        if (state == FxState.CANCELLED) return
        onHostLost()
        delegate.swapHost(fallback)
    }

    private fun attachAndRestore() {
        transition(FxState.ATTACHED) { delegate.performAttach() }
        while (pending.isNotEmpty()) delegate.perform(pending.removeFirst())
        if (desiredVisible) transition(FxState.SHOWN) { delegate.performShow() }
    }

    private inline fun transition(to: FxState, action: () -> Unit) {
        val old = state
        action()
        state = to
        delegate.onStateChanged(old, to)
    }

    private fun checkAlive() = check(state != FxState.CANCELLED) { "FxControl 已被 cancel，不能再操作" }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxEngineTest*"`
Expected: 12 tests PASS。

- [ ] **Step 5: Commit**

```bash
git add floatingx-core/src
git commit -m "feat(core): FxEngine 显示状态机与命令队列，FxHost/FxContainer 抽象"
```

---

### Task 7: 手势模型 FxGesture + FxGestureDetector（Robolectric）

**Files:**
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/gesture/FxGesture.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/gesture/FxGestureDetector.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/gesture/FxGestureDetectorTest.kt`

**Interfaces:**
- Produces（公开）：`FxDrag { IMMEDIATE, AFTER_LONG_PRESS, DISABLED }`、`FxChildPriority { AUTO, PARENT, CHILD }`、`fun interface FxRegion { contains(x, y, content: View) }` + `FxRegion.child(id)` / `FxRegion.rect(l, t, r, b)`、`FxGesture(click, longPress, drag, dragRegion, childPriority, touchable, longPressTimeout)` + 预设 `Normal/ClickOnly/DisplayOnly/LongPressToDrag`。
- Produces（internal）：`FxGestureDetector(touchSlop, defaultLongPressTimeout, callback, handler)` 与 `FxGestureDetector.Callback { onClick(); onLongPress(); onDragStart(); onDrag(dx, dy); onDragEnd(); canDragFrom(x, y); hasScrollableChildAt(x, y) }`；`var config: FxGesture`；`onIntercept(ev)`、`onTouch(ev)`、`cancel()`。Task 10 的 `GestureFeature` 实现 Callback。
- 坐标约定：detector 收到的 x/y 是**容器坐标**；`onDrag` 的 dx/dy 是相对上一事件的增量；起拖时第一段增量从 DOWN 点算起（含 slop 距离）。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.core.gesture

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class FxGestureDetectorTest {

    private class Recorder : FxGestureDetector.Callback {
        val events = mutableListOf<String>()
        var dragAllowed = true
        var scrollableChild = false
        override fun onClick() { events += "click" }
        override fun onLongPress() { events += "longPress" }
        override fun onDragStart() { events += "dragStart" }
        override fun onDrag(dx: Float, dy: Float) { events += "drag:${dx.toInt()},${dy.toInt()}" }
        override fun onDragEnd() { events += "dragEnd" }
        override fun canDragFrom(x: Float, y: Float): Boolean = dragAllowed
        override fun hasScrollableChildAt(x: Float, y: Float): Boolean = scrollableChild
    }

    private val recorder = Recorder()
    private val detector = FxGestureDetector(touchSlop = 8f, defaultLongPressTimeout = 400L, callback = recorder)
    private var time = 0L

    private fun single(action: Int, x: Float, y: Float): MotionEvent = MotionEvent.obtain(0L, ++time, action, x, y, 0)

    /** points = (pointerId, x, y)；主指抬起后剩余手指保留自己的 id */
    private fun multi(action: Int, actionIndex: Int, vararg points: Triple<Int, Float, Float>): MotionEvent {
        val props = Array(points.size) { i -> MotionEvent.PointerProperties().apply { id = points[i].first; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coords = Array(points.size) { i -> MotionEvent.PointerCoords().apply { x = points[i].second; y = points[i].third } }
        val fullAction = action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        return MotionEvent.obtain(0L, ++time, fullAction, points.size, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
    }

    private fun down(x: Float, y: Float) = detector.onTouch(single(MotionEvent.ACTION_DOWN, x, y))
    private fun move(x: Float, y: Float) = detector.onTouch(single(MotionEvent.ACTION_MOVE, x, y))
    private fun up(x: Float, y: Float) = detector.onTouch(single(MotionEvent.ACTION_UP, x, y))
    private fun idle(ms: Long) = ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)

    @Test
    fun `tap within slop fires click on up`() {
        down(10f, 10f); up(12f, 12f)
        assertEquals(listOf("click"), recorder.events)
    }

    @Test
    fun `click disabled fires nothing`() {
        detector.config = FxGesture(click = false)
        down(10f, 10f); up(12f, 12f)
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `moving beyond slop drags and never clicks`() {
        down(10f, 10f); move(30f, 10f); move(50f, 10f); up(50f, 10f)
        assertEquals(listOf("dragStart", "drag:20,0", "drag:20,0", "dragEnd"), recorder.events)
    }

    @Test
    fun `long press fires during press not on up`() {
        down(10f, 10f)
        idle(401)
        assertEquals(listOf("longPress"), recorder.events)
        up(10f, 10f)
        assertEquals(listOf("longPress"), recorder.events)
    }

    @Test
    fun `long press works without click listener`() {
        detector.config = FxGesture(click = false)
        down(10f, 10f); idle(401)
        assertEquals(listOf("longPress"), recorder.events)
    }

    @Test
    fun `movement beyond slop cancels pending long press`() {
        down(10f, 10f); move(30f, 10f); idle(401)
        assertFalse("longPress" in recorder.events)
    }

    @Test
    fun `custom long press timeout is honoured`() {
        detector.config = FxGesture(longPressTimeout = 100L)
        down(10f, 10f); idle(101)
        assertEquals(listOf("longPress"), recorder.events)
    }

    @Test
    fun `after long press mode drags only once long press fired`() {
        detector.config = FxGesture.LongPressToDrag
        down(10f, 10f); move(40f, 10f)
        assertTrue(recorder.events.isEmpty())          // 长按前移动：既不拖也（因移动）不再长按
        up(40f, 10f)
        assertTrue(recorder.events.isEmpty())

        recorder.events.clear()
        down(10f, 10f); idle(401)
        move(40f, 10f); up(40f, 10f)
        assertEquals(listOf("longPress", "dragStart", "drag:30,0", "dragEnd"), recorder.events)
    }

    @Test
    fun `drag disabled never drags and moved up is not a click`() {
        detector.config = FxGesture.ClickOnly
        down(10f, 10f); move(100f, 10f); up(100f, 10f)
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `touchable false ignores everything`() {
        detector.config = FxGesture.DisplayOnly
        assertFalse(down(10f, 10f))
        assertFalse(detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f)))
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `drag region can deny drag`() {
        recorder.dragAllowed = false
        down(10f, 10f); move(100f, 10f); up(100f, 10f)
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `secondary pointer up does not end drag`() {
        down(10f, 10f); move(40f, 10f)
        detector.onTouch(multi(MotionEvent.ACTION_POINTER_DOWN, 1, Triple(0, 40f, 10f), Triple(1, 100f, 100f)))
        detector.onTouch(multi(MotionEvent.ACTION_POINTER_UP, 1, Triple(0, 40f, 10f), Triple(1, 100f, 100f)))
        move(60f, 10f); up(60f, 10f)
        assertEquals(listOf("dragStart", "drag:30,0", "drag:20,0", "dragEnd"), recorder.events)
    }

    @Test
    fun `primary pointer up transfers drag to remaining pointer`() {
        down(10f, 10f); move(40f, 10f)
        detector.onTouch(multi(MotionEvent.ACTION_POINTER_DOWN, 1, Triple(0, 40f, 10f), Triple(1, 100f, 100f)))
        detector.onTouch(multi(MotionEvent.ACTION_POINTER_UP, 0, Triple(0, 40f, 10f), Triple(1, 100f, 100f)))
        detector.onTouch(multi(MotionEvent.ACTION_MOVE, 0, Triple(1, 110f, 100f)))
        detector.onTouch(multi(MotionEvent.ACTION_UP, 0, Triple(1, 110f, 100f)))
        assertEquals(listOf("dragStart", "drag:30,0", "drag:10,0", "dragEnd"), recorder.events)
    }

    @Test
    fun `intercept steals at slop with parent priority`() {
        detector.config = FxGesture(childPriority = FxChildPriority.PARENT)
        assertFalse(detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f)))
        assertFalse(detector.onIntercept(single(MotionEvent.ACTION_MOVE, 12f, 10f)))
        assertTrue(detector.onIntercept(single(MotionEvent.ACTION_MOVE, 30f, 10f)))
        assertEquals(listOf("dragStart"), recorder.events)
        move(50f, 10f); up(50f, 10f)
        assertEquals(listOf("dragStart", "drag:40,0", "dragEnd"), recorder.events)
    }

    @Test
    fun `intercept never steals with child priority`() {
        detector.config = FxGesture(childPriority = FxChildPriority.CHILD)
        detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f))
        assertFalse(detector.onIntercept(single(MotionEvent.ACTION_MOVE, 100f, 10f)))
    }

    @Test
    fun `auto priority defers to scrollable child for immediate drag`() {
        recorder.scrollableChild = true
        detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f))
        assertFalse(detector.onIntercept(single(MotionEvent.ACTION_MOVE, 100f, 10f)))
    }

    @Test
    fun `auto priority still drags scrollable child after long press`() {
        recorder.scrollableChild = true
        detector.config = FxGesture.LongPressToDrag
        detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f))
        idle(401)
        assertTrue(detector.onIntercept(single(MotionEvent.ACTION_MOVE, 12f, 10f)))
    }

    @Test
    fun `up seen by intercept means child consumed stream so no click`() {
        detector.onIntercept(single(MotionEvent.ACTION_DOWN, 10f, 10f))
        detector.onIntercept(single(MotionEvent.ACTION_UP, 10f, 10f))
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `cancel ends an active drag`() {
        down(10f, 10f); move(40f, 10f)
        detector.cancel()
        assertEquals(listOf("dragStart", "drag:30,0", "dragEnd"), recorder.events)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxGestureDetectorTest*"`
Expected: `Unresolved reference: FxGestureDetector`。

- [ ] **Step 3: 实现**

`gesture/FxGesture.kt`：

```kotlin
package com.petterp.floatingx.core.gesture

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes

/** 拖动触发方式 */
public enum class FxDrag { IMMEDIATE, AFTER_LONG_PRESS, DISABLED }

/** 与内容里可滚动子 view 的冲突策略：AUTO = 落点下有可滚动子 view 时不抢；PARENT = 超过 slop 就抢；CHILD = 永不抢 */
public enum class FxChildPriority { AUTO, PARENT, CHILD }

/** 允许起拖的区域，x/y 为相对内容 view 左上角的坐标 */
public fun interface FxRegion {
    public fun contains(x: Float, y: Float, content: View): Boolean

    public companion object {
        /** 只有按在某个子 view 上才能拖（#165） */
        @JvmStatic
        public fun child(@IdRes id: Int): FxRegion = FxRegion { x, y, content ->
            val child = content.findViewById<View>(id) ?: return@FxRegion false
            if (child === content) return@FxRegion true
            val group = content as? ViewGroup ?: return@FxRegion false
            val rect = Rect(0, 0, child.width, child.height)
            group.offsetDescendantRectToMyCoords(child, rect)
            rect.contains(x.toInt(), y.toInt())
        }

        @JvmStatic
        public fun rect(left: Float, top: Float, right: Float, bottom: Float): FxRegion =
            FxRegion { x, y, _ -> x >= left && x <= right && y >= top && y <= bottom }
    }
}

/**
 * 可组合的手势配置（spec §2.4），替代 2.x 的 FxDisplayMode 枚举。
 * @param longPressTimeout 0 表示使用系统 ViewConfiguration.getLongPressTimeout()
 */
public data class FxGesture @JvmOverloads constructor(
    val click: Boolean = true,
    val longPress: Boolean = true,
    val drag: FxDrag = FxDrag.IMMEDIATE,
    val dragRegion: FxRegion? = null,
    val childPriority: FxChildPriority = FxChildPriority.AUTO,
    val touchable: Boolean = true,
    val longPressTimeout: Long = 0L,
) {
    public companion object {
        @JvmField public val Normal: FxGesture = FxGesture()
        @JvmField public val ClickOnly: FxGesture = FxGesture(drag = FxDrag.DISABLED)
        /** 完全透传，内容只展示（#243/#108） */
        @JvmField public val DisplayOnly: FxGesture = FxGesture(click = false, longPress = false, drag = FxDrag.DISABLED, touchable = false)
        /** 长按后才可拖动（#222） */
        @JvmField public val LongPressToDrag: FxGesture = FxGesture(drag = FxDrag.AFTER_LONG_PRESS)
    }
}
```

`gesture/FxGestureDetector.kt`：

```kotlin
package com.petterp.floatingx.core.gesture

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import kotlin.math.abs

/**
 * 点击 / 长按 / 拖动 判定（spec §2.4）。
 * - 全部用 actionMasked；跟踪主指针 id，主指抬起时把控制权转移给剩下的手指
 * - 长按在按下期间由定时器触发，与是否有点击监听无关（#218）
 * - 点击 = 抬起时仍在 slop 内且长按未触发；无额外时间阈值
 * - MOVE 路径零分配
 *
 * 容器的 onInterceptTouchEvent → onIntercept()；onTouchEvent → onTouch()。
 * 收到的坐标均为容器坐标。
 */
internal class FxGestureDetector(
    private val touchSlop: Float,
    private val defaultLongPressTimeout: Long,
    private val callback: Callback,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {

    internal interface Callback {
        fun onClick()
        fun onLongPress()
        fun onDragStart()
        /** 相对上一次事件的增量 */
        fun onDrag(dx: Float, dy: Float)
        fun onDragEnd()
        /** DOWN 落点是否允许起拖（dragRegion） */
        fun canDragFrom(x: Float, y: Float): Boolean
        /** DOWN 落点下是否有可滚动子 view（AUTO 优先级用） */
        fun hasScrollableChildAt(x: Float, y: Float): Boolean
    }

    var config: FxGesture = FxGesture.Normal

    private var pointerId = INVALID
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var moved = false
    private var longPressed = false
    private var dragging = false
    private var canDrag = false
    private var childScrollable = false

    private val longPressRunnable = Runnable {
        if (pointerId == INVALID || moved) return@Runnable
        longPressed = true
        if (config.longPress) callback.onLongPress()
    }

    /** 返回 true 表示从此拦截，子 view 收到 CANCEL */
    fun onIntercept(ev: MotionEvent): Boolean {
        if (!config.touchable) return false
        return when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> { begin(ev); false }
            MotionEvent.ACTION_MOVE -> {
                if (pointerId == INVALID) return false
                if (config.childPriority == FxChildPriority.CHILD) return false
                if (config.childPriority == FxChildPriority.AUTO && childScrollable && config.drag == FxDrag.IMMEDIATE) return false
                val idx = ev.findPointerIndex(pointerId)
                if (idx < 0) return false
                val x = ev.getX(idx)
                val y = ev.getY(idx)
                updateMoved(x, y)
                if (shouldStartDrag()) { startDrag(); true } else false
            }
            // 子 view 消费了整条事件流：不产生点击
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { reset(); false }
            else -> false
        }
    }

    fun onTouch(ev: MotionEvent): Boolean {
        if (!config.touchable) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (pointerId == INVALID) begin(ev)   // 已在 onIntercept 里 begin 过则跳过
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = ev.findPointerIndex(pointerId)
                if (idx < 0) return true
                val x = ev.getX(idx)
                val y = ev.getY(idx)
                if (!dragging) {
                    updateMoved(x, y)
                    if (shouldStartDrag()) startDrag()
                }
                if (dragging) {
                    callback.onDrag(x - lastX, y - lastY)
                    lastX = x
                    lastY = y
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val idx = ev.actionIndex
                if (ev.getPointerId(idx) == pointerId) {
                    val newIdx = if (idx == 0) 1 else 0
                    pointerId = ev.getPointerId(newIdx)
                    lastX = ev.getX(newIdx)
                    lastY = ev.getY(newIdx)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (dragging) callback.onDragEnd()
                else if (!moved && !longPressed && config.click) callback.onClick()
                reset()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (dragging) callback.onDragEnd()
                reset()
                return true
            }
            else -> return true
        }
    }

    /** 容器 detach 或配置切换时调用 */
    fun cancel() {
        if (dragging) callback.onDragEnd()
        reset()
    }

    private fun begin(ev: MotionEvent) {
        reset()
        pointerId = ev.getPointerId(0)
        downX = ev.x
        downY = ev.y
        lastX = downX
        lastY = downY
        canDrag = config.drag != FxDrag.DISABLED && callback.canDragFrom(downX, downY)
        childScrollable = callback.hasScrollableChildAt(downX, downY)
        if (config.longPress || config.drag == FxDrag.AFTER_LONG_PRESS) {
            val timeout = if (config.longPressTimeout > 0) config.longPressTimeout else defaultLongPressTimeout
            handler.postDelayed(longPressRunnable, timeout)
        }
    }

    private fun updateMoved(x: Float, y: Float) {
        if (moved) return
        if (abs(x - downX) > touchSlop || abs(y - downY) > touchSlop) {
            moved = true
            if (!longPressed) handler.removeCallbacks(longPressRunnable)
        }
    }

    private fun shouldStartDrag(): Boolean = canDrag && when (config.drag) {
        FxDrag.IMMEDIATE -> moved
        FxDrag.AFTER_LONG_PRESS -> longPressed
        FxDrag.DISABLED -> false
    }

    /** 第一段增量从 DOWN 点算起，所以 lastX/lastY 回到 down 位置 */
    private fun startDrag() {
        dragging = true
        handler.removeCallbacks(longPressRunnable)
        lastX = downX
        lastY = downY
        callback.onDragStart()
    }

    private fun reset() {
        handler.removeCallbacks(longPressRunnable)
        pointerId = INVALID
        moved = false
        longPressed = false
        dragging = false
        canDrag = false
        childScrollable = false
    }

    private companion object {
        const val INVALID = -1
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxGestureDetectorTest*"`
Expected: 19 tests PASS。若 `primary pointer up transfers` 用例因 `MotionEvent.obtain` 多指构造在 Robolectric 下坐标读取为 0 而失败，改用 `MotionEvent.obtainNoHistory` 无效时，用 `ev.getX(idx)` 前先 `ev.offsetLocation(0f, 0f)` 触发坐标同步；仍失败则把该用例标 `@Ignore("Robolectric 多指坐标")` 并在提交信息里注明，由 Plan 5 的 instrumentation 覆盖。

- [ ] **Step 5: Commit**

```bash
git add floatingx-core/src
git commit -m "feat(core): 可组合手势配置 FxGesture 与 FxGestureDetector"
```

---

### Task 8: Layer 容器 + FxViewHolder + FxContent + FxAnimation（Robolectric）

**Files:**
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/container/FxLayerContainer.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/container/FxViewHolder.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/config/FxContent.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/animation/FxAnimation.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/animation/FxAnimations.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/container/FxLayerContainerTest.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/config/FxContentTest.kt`

**Interfaces:**
- Consumes: Task 6 的 `FxContainer` / `FxContainerTouchHandler`。
- Produces: `FxLayerContainer(context) : FrameLayout, FxContainer` + `var modal: Boolean` + `var onOutsideTouch: (() -> Unit)?`；`FxViewHolder(view)`（链式 setText/setImage…）；`abstract class FxContent { create(context, parent): View }` + `FxContent.Layout/Static/Provider` + 工厂 `layout(id)/view(v)/provider(fn)`；`abstract class FxAnimation { showAnimator(view); hideAnimator(view) }`；`FxAnimations.fade(duration)/scale(duration)`。

- [ ] **Step 1: 写失败测试**

`container/FxLayerContainerTest.kt`：

```kotlin
package com.petterp.floatingx.core.container

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.layout.FxSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxLayerContainerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val container = FxLayerContainer(context)
    private val content = View(context).apply { layoutParams = FrameLayout.LayoutParams(100, 200) }

    private fun layout() {
        container.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY))
        container.layout(0, 0, 1080, 1920)
    }

    private fun event(action: Int, x: Float, y: Float) = MotionEvent.obtain(0L, 0L, action, x, y, 0)

    @Test
    fun `setContent adds child at top start keeping size`() {
        container.setContent(content)
        val lp = content.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.TOP or Gravity.START, lp.gravity)
        assertEquals(100, lp.width)
        assertEquals(content, container.contentView)
        assertEquals(1, container.childCount)
    }

    @Test
    fun `setContent replaces previous content and reparents`() {
        val other = FrameLayout(context).also { it.addView(content) }
        container.setContent(View(context))
        container.setContent(content)
        assertEquals(0, other.childCount)
        assertEquals(1, container.childCount)
        assertEquals(content, container.contentView)
    }

    @Test
    fun `content size reported after layout only when changed`() {
        val sizes = mutableListOf<FxSize>()
        container.onContentSizeChanged = { sizes += it }
        container.setContent(content)
        layout(); layout()
        assertEquals(listOf(FxSize(100f, 200f)), sizes)
        assertEquals(FxSize(100f, 200f), container.contentSize())
    }

    @Test
    fun `position uses translation and hitTest follows it`() {
        container.setContent(content); layout()
        container.setContentPosition(500f, 600f)
        assertEquals(500f, content.translationX, 0f)
        assertEquals(600f, content.translationY, 0f)
        assertTrue(container.hitTest(550f, 700f))
        assertFalse(container.hitTest(10f, 10f))
        assertFalse(container.hitTest(600f, 700f))
    }

    @Test
    fun `hidden content is not hit`() {
        container.setContent(content); layout()
        container.setContentVisible(false)
        assertEquals(View.INVISIBLE, content.visibility)
        assertFalse(container.hitTest(10f, 10f))
    }

    @Test
    fun `down outside content passes through when not modal`() {
        container.setContent(content); layout()
        assertFalse(container.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, 900f, 900f)))
    }

    @Test
    fun `modal consumes outside touch and notifies`() {
        var outside = 0
        container.setContent(content); layout()
        container.modal = true
        container.onOutsideTouch = { outside++ }
        assertTrue(container.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, 900f, 900f)))
        assertTrue(container.dispatchTouchEvent(event(MotionEvent.ACTION_UP, 900f, 900f)))
        assertEquals(1, outside)
    }

    @Test
    fun `down inside content goes to touch handler`() {
        val seen = mutableListOf<String>()
        container.touchHandler = object : FxContainerTouchHandler {
            override fun onIntercept(ev: MotionEvent): Boolean { seen += "intercept:${ev.actionMasked}"; return false }
            override fun onTouch(ev: MotionEvent): Boolean { seen += "touch:${ev.actionMasked}"; return true }
        }
        container.setContent(content); layout()
        assertTrue(container.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN, 10f, 10f)))
        assertEquals(listOf("intercept:${MotionEvent.ACTION_DOWN}", "touch:${MotionEvent.ACTION_DOWN}"), seen)
    }

    @Test
    fun `size change reports bounds changed`() {
        var bounds = 0
        container.onBoundsChanged = { bounds++ }
        layout()
        assertEquals(1, bounds)
    }

    @Test
    fun `screen position adds container location`() {
        container.setContent(content); layout()
        container.setContentPosition(5f, 6f)
        val p = container.contentPositionOnScreen()
        assertEquals(5f, p.x, 0f)
        assertEquals(6f, p.y, 0f)
    }
}
```

`config/FxContentTest.kt`：

```kotlin
package com.petterp.floatingx.core.config

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxContentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val parent = FrameLayout(context)

    @Test
    fun `layout content inflates without attaching`() {
        val view = FxContent.layout(android.R.layout.simple_list_item_1).create(context, parent)
        assertTrue(view is TextView)
        assertNull(view.parent)
        assertEquals(0, parent.childCount)
    }

    @Test
    fun `static content returns the same instance`() {
        val v = View(context)
        assertSame(v, FxContent.view(v).create(context, parent))
    }

    @Test
    fun `provider content is invoked with context`() {
        var seen: Context? = null
        val view = FxContent.provider { ctx -> seen = ctx; View(ctx) }.create(context, parent)
        assertSame(context, seen)
        assertSame(context, view.context)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxLayerContainerTest*" --tests "*FxContentTest*"`
Expected: `Unresolved reference: FxLayerContainer` / `FxContent`。

- [ ] **Step 3: 实现**

`container/FxLayerContainer.kt`：

```kotlin
package com.petterp.floatingx.core.container

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.core.layout.FxPoint
import com.petterp.floatingx.core.layout.FxSize

/**
 * Layer 容器（spec §2.1）：一个 match_parent 的透明覆盖层，内容子 view 在其中用 translation 定位。
 * - 移动只改 translation，父层永不 re-layout（修 #240）
 * - 落在内容之外的 DOWN 直接返回 false 透传给下层（修 #243）；modal 时则消费（#212）
 * - clipChildren=false 允许内容随 FxOverflow 超出边界（#235）
 */
public class FxLayerContainer(context: Context) : FrameLayout(context), FxContainer {

    override val view: ViewGroup get() = this
    override var contentView: View? = null
        private set
    override val isLtr: Boolean get() = layoutDirection != View.LAYOUT_DIRECTION_RTL
    override var touchHandler: FxContainerTouchHandler? = null
    override var onContentSizeChanged: ((FxSize) -> Unit)? = null
    override var onBoundsChanged: (() -> Unit)? = null

    /** true 时拦截内容之外的触摸（ModalScrimFeature 设置） */
    public var modal: Boolean = false
    public var onOutsideTouch: (() -> Unit)? = null

    private val screenLocation = IntArray(2)
    private var lastW = 0
    private var lastH = 0
    private var consumingOutside = false

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
        clipChildren = false
        clipToPadding = false
    }

    override fun setContent(view: View) {
        contentView?.let {
            it.removeOnLayoutChangeListener(contentLayoutListener)
            removeView(it)
        }
        (view.parent as? ViewGroup)?.removeView(view)
        val lp = (view.layoutParams as? LayoutParams)
            ?: LayoutParams(view.layoutParams?.width ?: LayoutParams.WRAP_CONTENT, view.layoutParams?.height ?: LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.TOP or Gravity.START
        lastW = 0
        lastH = 0
        addView(view, lp)
        view.addOnLayoutChangeListener(contentLayoutListener)
        contentView = view
    }

    override fun contentSize(): FxSize =
        contentView?.let { FxSize(it.width.toFloat(), it.height.toFloat()) } ?: FxSize.EMPTY

    override fun setContentPosition(x: Float, y: Float) {
        val c = contentView ?: return
        c.translationX = x
        c.translationY = y
    }

    override fun contentPosition(): FxPoint =
        contentView?.let { FxPoint(it.translationX, it.translationY) } ?: FxPoint.ZERO

    override fun contentPositionOnScreen(): FxPoint {
        getLocationOnScreen(screenLocation)
        val c = contentView ?: return FxPoint(screenLocation[0].toFloat(), screenLocation[1].toFloat())
        return FxPoint(screenLocation[0] + c.translationX, screenLocation[1] + c.translationY)
    }

    override fun setContentVisible(visible: Boolean) {
        contentView?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    override fun hitTest(x: Float, y: Float): Boolean {
        val c = contentView ?: return false
        if (c.visibility != View.VISIBLE) return false
        val left = c.translationX
        val top = c.translationY
        return x >= left && x < left + c.width && y >= top && y < top + c.height
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        if (consumingOutside) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) consumingOutside = false
            return true
        }
        if (action == MotionEvent.ACTION_DOWN && !hitTest(ev.x, ev.y)) {
            if (modal) {
                consumingOutside = true
                onOutsideTouch?.invoke()
                return true
            }
            return false
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = touchHandler?.onIntercept(ev) ?: false

    override fun onTouchEvent(ev: MotionEvent): Boolean = touchHandler?.onTouch(ev) ?: false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) onBoundsChanged?.invoke()
    }
}
```

`container/FxViewHolder.kt`（从 2.x 平移，改为 explicit API）：

```kotlin
package com.petterp.floatingx.core.container

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes

/** 内容 view 的 ViewHolder，findViewById 结果带缓存 */
public class FxViewHolder(public val view: View) {

    private val views = SparseArray<View>()

    public fun <T : View> getView(@IdRes viewId: Int): T =
        checkNotNull(getViewOrNull(viewId)) { "内容里没有 id=$viewId 的 view" }

    @Suppress("UNCHECKED_CAST")
    public fun <T : View> getViewOrNull(@IdRes viewId: Int): T? {
        val cached = views.get(viewId)
        if (cached != null) return cached as? T
        val found = view.findViewById<T>(viewId) ?: return null
        views.put(viewId, found)
        return found
    }

    public fun setOnClickListener(@IdRes viewId: Int, listener: View.OnClickListener): FxViewHolder = apply { getView<View>(viewId).setOnClickListener(listener) }
    public fun setText(@IdRes viewId: Int, value: CharSequence?): FxViewHolder = apply { getView<TextView>(viewId).text = value }
    public fun setText(@IdRes viewId: Int, @StringRes resId: Int): FxViewHolder = apply { getView<TextView>(viewId).setText(resId) }
    public fun setTextSize(@IdRes viewId: Int, size: Float): FxViewHolder = apply { getView<TextView>(viewId).textSize = size }
    public fun setTextSize(@IdRes viewId: Int, unit: Int, size: Float): FxViewHolder = apply { getView<TextView>(viewId).setTextSize(unit, size) }
    public fun setImageResource(@IdRes viewId: Int, @DrawableRes source: Int): FxViewHolder = apply { getView<ImageView>(viewId).setImageResource(source) }
    public fun setImageBitmap(@IdRes viewId: Int, bitmap: Bitmap?): FxViewHolder = apply { getView<ImageView>(viewId).setImageBitmap(bitmap) }
    public fun setImageDrawable(@IdRes viewId: Int, drawable: Drawable?): FxViewHolder = apply { getView<ImageView>(viewId).setImageDrawable(drawable) }
    public fun setBackgroundResource(@IdRes viewId: Int, @DrawableRes source: Int): FxViewHolder = apply { getView<View>(viewId).setBackgroundResource(source) }
    public fun setBackgroundColor(@IdRes viewId: Int, @ColorInt color: Int): FxViewHolder = apply { getView<View>(viewId).setBackgroundColor(color) }
    public fun setGone(@IdRes viewId: Int, gone: Boolean): FxViewHolder = apply { getView<View>(viewId).visibility = if (gone) View.GONE else View.VISIBLE }
    public fun setEnabled(@IdRes viewId: Int, enabled: Boolean): FxViewHolder = apply { getView<View>(viewId).isEnabled = enabled }
}
```

`config/FxContent.kt`：

```kotlin
package com.petterp.floatingx.core.config

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

/**
 * 浮窗内容的来源。开放继承：compose 模块提供 Compose 实现。
 * 内容 view 在 install 时用 host.context 创建一次，之后跨 host 复用（spec §2.2）。
 */
public abstract class FxContent {

    /** 创建内容 view；parent 仅用于解析 LayoutParams，实现不得 addView */
    public abstract fun create(context: Context, parent: ViewGroup): View

    public class Layout(@LayoutRes public val id: Int) : FxContent() {
        override fun create(context: Context, parent: ViewGroup): View =
            LayoutInflater.from(context).inflate(id, parent, false)
    }

    public class Static(public val view: View) : FxContent() {
        override fun create(context: Context, parent: ViewGroup): View = view
    }

    public class Provider(public val provider: (Context) -> View) : FxContent() {
        override fun create(context: Context, parent: ViewGroup): View = provider(context)
    }

    public companion object {
        @JvmStatic public fun layout(@LayoutRes id: Int): FxContent = Layout(id)
        @JvmStatic public fun view(view: View): FxContent = Static(view)
        @JvmStatic public fun provider(provider: (Context) -> View): FxContent = Provider(provider)
    }
}
```

`animation/FxAnimation.kt`：

```kotlin
package com.petterp.floatingx.core.animation

import android.animation.Animator
import android.view.View

/** 显示 / 隐藏动画。作用对象是内容 view，不是容器 */
public abstract class FxAnimation {
    public abstract fun showAnimator(view: View): Animator
    public abstract fun hideAnimator(view: View): Animator
}
```

`animation/FxAnimations.kt`：

```kotlin
package com.petterp.floatingx.core.animation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View

/** 内置动画 */
public object FxAnimations {

    @JvmStatic @JvmOverloads
    public fun fade(duration: Long = 200L): FxAnimation = object : FxAnimation() {
        override fun showAnimator(view: View): Animator = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).setDuration(duration)
        override fun hideAnimator(view: View): Animator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).setDuration(duration)
    }

    @JvmStatic @JvmOverloads
    public fun scale(duration: Long = 200L): FxAnimation = object : FxAnimation() {
        override fun showAnimator(view: View): Animator = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
        ).setDuration(duration)

        override fun hideAnimator(view: View): Animator = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f),
        ).setDuration(duration)
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxLayerContainerTest*" --tests "*FxContentTest*"`
Expected: 13 tests PASS。

- [ ] **Step 5: Commit**

```bash
git add floatingx-core/src
git commit -m "feat(core): Layer 容器、FxViewHolder、FxContent 与内置动画"
```

---

### Task 9: 公开接口层 —— FxControl / FxListener / FxConfig（Builder + DSL）/ FxFeature（JVM）

**Files:**
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/FxControl.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/FxListener.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/config/FxConfig.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/config/FxConfigScope.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/feature/FxFeature.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/feature/FxFeatureScope.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/config/FxConfigTest.kt`

**Interfaces:**
- Consumes: Task 3–8 的全部公开类型。
- Produces：
  - `FxControl`（见代码；Kotlin 扩展 `FxControl.update { }`）
  - `FxListener`（11 个默认空方法）
  - `FxConfig`（不可变）+ `FxConfig.Builder(content)` + `FxConfig.builder(content)`；`toBuilder()`
  - `@FxDsl`、`open class FxConfigScope(base: FxConfig?)`、`class FxInstallScope : FxConfigScope { var host: FxHost? }`、`FxGestureScope`
  - `FxFeature { onAttach(scope); onDetach(); onConfigChanged(old, new); onContentSizeChanged(size); onBoundsChanged(); onShow(); onHide() }`
  - `FxFeatureScope { control; config; container; host; logger; layoutInput(); commitAnchor(anchor); dispatch(block); requestRelayout() }`
- Task 10 实现 `FxControl` / `FxFeatureScope`。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.petterp.floatingx.core.config

import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope
import com.petterp.floatingx.core.gesture.FxDrag
import com.petterp.floatingx.core.gesture.FxGesture
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxMargin
import com.petterp.floatingx.core.layout.FxOverflow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FxConfigTest {

    private val content = FxContent.layout(1)
    private val feature = object : FxFeature {
        override fun onAttach(scope: FxFeatureScope) = Unit
        override fun onDetach() = Unit
    }

    @Test
    fun `builder defaults`() {
        val c = FxConfig.builder(content).build()
        assertSame(content, c.content)
        assertEquals(FxAnchor.DEFAULT, c.anchor)
        assertEquals(FxMargin.NONE, c.margin)
        assertEquals(FxOverflow.NONE, c.overflow)
        assertTrue(c.safeArea)
        assertEquals(FxAdsorb.None, c.adsorb)
        assertEquals(FxGesture.Normal, c.gesture)
        assertNull(c.animation)
        assertNull(c.storage)
        assertTrue(c.features.isEmpty())
        assertNull(c.logger)
    }

    @Test
    fun `builder sets everything and toBuilder copies`() {
        val c = FxConfig.builder(content)
            .anchor(FxGravity.BOTTOM_END, 1f, 2f)
            .margin(1f, 2f, 3f, 4f)
            .overflow(FxOverflow(top = true))
            .safeArea(false)
            .adsorb(FxAdsorb.horizontal())
            .gesture(FxGesture.LongPressToDrag)
            .addFeature(feature)
            .enableLog("demo")
            .build()
        val copy = c.toBuilder().build()
        assertEquals(FxAnchor(FxGravity.BOTTOM_END, 1f, 2f), copy.anchor)
        assertEquals(FxMargin(1f, 2f, 3f, 4f), copy.margin)
        assertEquals(FxOverflow(top = true), copy.overflow)
        assertFalse(copy.safeArea)
        assertEquals(FxAdsorb.horizontal(), copy.adsorb)
        assertEquals(FxGesture.LongPressToDrag, copy.gesture)
        assertEquals(listOf(feature), copy.features)
        assertTrue(copy.logger != null)
    }

    @Test
    fun `dsl builds the same config as builder`() {
        val scope = FxConfigScope(null).apply {
            content(content)
            anchor(FxGravity.CENTER_END, dy = 120f)
            margin(top = 24f)
            overflow(top = true)
            safeArea = false
            adsorb(FxAdsorb.horizontal())
            gesture { drag = FxDrag.AFTER_LONG_PRESS; longPress = false }
        }
        val c = scope.build()
        assertEquals(FxAnchor(FxGravity.CENTER_END, 0f, 120f), c.anchor)
        assertEquals(FxMargin(top = 24f), c.margin)
        assertEquals(FxOverflow(top = true), c.overflow)
        assertFalse(c.safeArea)
        assertEquals(FxGesture(longPress = false, drag = FxDrag.AFTER_LONG_PRESS), c.gesture)
    }

    @Test
    fun `dsl from base keeps unspecified values`() {
        val base = FxConfig.builder(content).anchor(FxGravity.TOP_END).gesture(FxGesture.ClickOnly).build()
        val c = FxConfigScope(base).apply { margin(left = 5f) }.build()
        assertEquals(FxAnchor(FxGravity.TOP_END), c.anchor)
        assertEquals(FxGesture.ClickOnly, c.gesture)
        assertEquals(FxMargin(left = 5f), c.margin)
        assertSame(content, c.content)
    }

    @Test
    fun `dsl without content fails loudly`() {
        assertThrows(IllegalArgumentException::class.java) { FxConfigScope(null).apply { anchor(FxGravity.CENTER) }.build() }
    }

    @Test
    fun `install scope has no host by default`() {
        assertNull(FxInstallScope().host)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxConfigTest*"`
Expected: `Unresolved reference: FxConfig`。

- [ ] **Step 3: 实现**

`FxListener.kt`：

```kotlin
package com.petterp.floatingx.core

import android.view.View
import com.petterp.floatingx.core.layout.FxAnchor

/** 浮窗事件监听；全部有默认空实现，Java 端得益于 jvmDefault=enable 也只需覆写用到的方法 */
public interface FxListener {
    public fun onAttach(control: FxControl) {}
    public fun onDetach(control: FxControl) {}
    public fun onShow(control: FxControl) {}
    public fun onHide(control: FxControl) {}
    public fun onClick(control: FxControl, view: View) {}
    public fun onLongClick(control: FxControl, view: View) {}
    public fun onDragStart(control: FxControl) {}
    /** 每个 MOVE 都回调（#199），x/y 为内容相对容器坐标 */
    public fun onDrag(control: FxControl, x: Float, y: Float) {}
    public fun onDragEnd(control: FxControl, x: Float, y: Float) {}
    /** 锚点提交（拖动/吸附结束、moveTo 完成、update{anchor}）后回调，可用于判断贴在哪边（#148） */
    public fun onPositionChanged(control: FxControl, anchor: FxAnchor) {}
    public fun onCancel(control: FxControl) {}
}
```

`FxControl.kt`：

```kotlin
package com.petterp.floatingx.core

import android.view.View
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxConfigScope
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.container.FxViewHolder
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxPoint

/**
 * 用户面向的控制接口（spec §2.7）。所有方法必须在主线程调用。
 * 未 attach 时 show/hide 记录意图、moveTo/moveBy 排队，host ready 后按序回放。
 */
public interface FxControl {
    public val tag: String
    public val state: FxState
    public val isShowing: Boolean
    /** 内容左上角的屏幕坐标，三种 host 语义一致（#200） */
    public val position: FxPoint
    public val anchor: FxAnchor
    public val config: FxConfig
    public val host: FxHost
    public val contentView: View?
    public val holder: FxViewHolder?

    public fun show()
    public fun hide()
    /** 销毁；之后任何调用抛 IllegalStateException */
    public fun cancel()

    public fun moveTo(x: Float, y: Float)
    public fun moveTo(x: Float, y: Float, animate: Boolean)
    public fun moveBy(dx: Float, dy: Float)
    public fun moveBy(dx: Float, dy: Float, animate: Boolean)

    /** 整体替换配置；Kotlin 用 `update { }` 扩展 */
    public fun update(config: FxConfig)
    /** 内容 view 在 install 时即创建，所以 show 之前也可用（#152/#89） */
    public fun updateContent(block: (FxViewHolder) -> Unit)
    public fun setContent(content: FxContent)

    public fun addListener(listener: FxListener)
    public fun removeListener(listener: FxListener)
    public fun addFeature(feature: FxFeature)
    public fun removeFeature(feature: FxFeature)
}

/** Kotlin DSL：在现有配置基础上局部修改 */
public inline fun FxControl.update(block: FxConfigScope.() -> Unit) {
    update(FxConfigScope(config).apply(block).build())
}
```

> `FxConfigScope.build()` 必须是 `public`（被 inline 公开函数调用），见下。

`config/FxConfig.kt`：

```kotlin
package com.petterp.floatingx.core.config

import com.petterp.floatingx.core.FxLogcatLogger
import com.petterp.floatingx.core.FxLogger
import com.petterp.floatingx.core.animation.FxAnimation
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.gesture.FxGesture
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxMargin
import com.petterp.floatingx.core.layout.FxOverflow
import com.petterp.floatingx.core.storage.FxStorage

/** 不可变配置（spec §2.6）。Java 用 Builder，Kotlin 用 FxConfigScope DSL */
public class FxConfig private constructor(
    public val content: FxContent,
    public val anchor: FxAnchor,
    public val margin: FxMargin,
    public val overflow: FxOverflow,
    public val safeArea: Boolean,
    public val adsorb: FxAdsorb,
    public val gesture: FxGesture,
    public val animation: FxAnimation?,
    public val storage: FxStorage?,
    public val features: List<FxFeature>,
    public val logger: FxLogger?,
) {

    public fun toBuilder(): Builder = Builder(content)
        .anchor(anchor).margin(margin).overflow(overflow).safeArea(safeArea)
        .adsorb(adsorb).gesture(gesture).animation(animation).storage(storage).logger(logger)
        .also { b -> features.forEach(b::addFeature) }

    public class Builder(private var content: FxContent) {
        private var anchor: FxAnchor = FxAnchor.DEFAULT
        private var margin: FxMargin = FxMargin.NONE
        private var overflow: FxOverflow = FxOverflow.NONE
        private var safeArea: Boolean = true
        private var adsorb: FxAdsorb = FxAdsorb.None
        private var gesture: FxGesture = FxGesture.Normal
        private var animation: FxAnimation? = null
        private var storage: FxStorage? = null
        private val features = mutableListOf<FxFeature>()
        private var logger: FxLogger? = null

        public fun content(content: FxContent): Builder = apply { this.content = content }
        public fun anchor(anchor: FxAnchor): Builder = apply { this.anchor = anchor }
        @JvmOverloads
        public fun anchor(gravity: FxGravity, dx: Float = 0f, dy: Float = 0f): Builder = anchor(FxAnchor(gravity, dx, dy))
        public fun margin(margin: FxMargin): Builder = apply { this.margin = margin }
        public fun margin(left: Float, top: Float, right: Float, bottom: Float): Builder = margin(FxMargin(left, top, right, bottom))
        public fun overflow(overflow: FxOverflow): Builder = apply { this.overflow = overflow }
        public fun safeArea(enabled: Boolean): Builder = apply { this.safeArea = enabled }
        public fun adsorb(adsorb: FxAdsorb): Builder = apply { this.adsorb = adsorb }
        public fun gesture(gesture: FxGesture): Builder = apply { this.gesture = gesture }
        public fun animation(animation: FxAnimation?): Builder = apply { this.animation = animation }
        public fun storage(storage: FxStorage?): Builder = apply { this.storage = storage }
        public fun addFeature(feature: FxFeature): Builder = apply { features += feature }
        public fun logger(logger: FxLogger?): Builder = apply { this.logger = logger }
        @JvmOverloads
        public fun enableLog(tag: String = "Fx"): Builder = logger(FxLogcatLogger(tag))

        public fun build(): FxConfig = FxConfig(content, anchor, margin, overflow, safeArea, adsorb, gesture, animation, storage, features.toList(), logger)
    }

    public companion object {
        @JvmStatic public fun builder(content: FxContent): Builder = Builder(content)
    }
}
```

`config/FxConfigScope.kt`：

```kotlin
package com.petterp.floatingx.core.config

import android.content.Context
import android.view.View
import androidx.annotation.LayoutRes
import com.petterp.floatingx.core.animation.FxAnimation
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.gesture.FxChildPriority
import com.petterp.floatingx.core.gesture.FxDrag
import com.petterp.floatingx.core.gesture.FxGesture
import com.petterp.floatingx.core.gesture.FxRegion
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxMargin
import com.petterp.floatingx.core.layout.FxOverflow
import com.petterp.floatingx.core.storage.FxStorage

@DslMarker
public annotation class FxDsl

/**
 * Kotlin DSL。`base` 非空时（control.update {}）未显式设置的项沿用旧值。
 * build() 是 public 因为被 inline 的 FxControl.update 调用。
 */
@FxDsl
public open class FxConfigScope(base: FxConfig?) {
    private var content: FxContent? = base?.content
    private var anchor: FxAnchor = base?.anchor ?: FxAnchor.DEFAULT
    private var margin: FxMargin = base?.margin ?: FxMargin.NONE
    private var overflow: FxOverflow = base?.overflow ?: FxOverflow.NONE
    private var adsorb: FxAdsorb = base?.adsorb ?: FxAdsorb.None
    private var gesture: FxGesture = base?.gesture ?: FxGesture.Normal
    private var animation: FxAnimation? = base?.animation
    private var storage: FxStorage? = base?.storage
    private val features: MutableList<FxFeature> = base?.features?.toMutableList() ?: mutableListOf()
    private var logTag: String? = null
    private val baseLogger = base?.logger

    public var safeArea: Boolean = base?.safeArea ?: true

    public fun layout(@LayoutRes id: Int) { content = FxContent.layout(id) }
    public fun view(view: View) { content = FxContent.view(view) }
    public fun view(provider: (Context) -> View) { content = FxContent.provider(provider) }
    public fun content(content: FxContent) { this.content = content }
    public fun anchor(gravity: FxGravity, dx: Float = 0f, dy: Float = 0f) { anchor = FxAnchor(gravity, dx, dy) }
    public fun margin(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f) { margin = FxMargin(left, top, right, bottom) }
    public fun overflow(top: Boolean = false, bottom: Boolean = false, left: Boolean = false, right: Boolean = false) { overflow = FxOverflow(top, bottom, left, right) }
    public fun adsorb(adsorb: FxAdsorb) { this.adsorb = adsorb }
    public fun gesture(gesture: FxGesture) { this.gesture = gesture }
    public fun gesture(block: FxGestureScope.() -> Unit) { gesture = FxGestureScope(gesture).apply(block).build() }
    public fun animation(animation: FxAnimation?) { this.animation = animation }
    public fun persist(storage: FxStorage?) { this.storage = storage }
    public fun addFeature(feature: FxFeature) { features += feature }
    public fun enableLog(tag: String = "Fx") { logTag = tag }

    public fun build(): FxConfig {
        val c = requireNotNull(content) { "必须通过 layout()/view()/content() 指定浮窗内容" }
        val builder = FxConfig.builder(c)
            .anchor(anchor).margin(margin).overflow(overflow).safeArea(safeArea)
            .adsorb(adsorb).gesture(gesture).animation(animation).storage(storage)
        features.forEach(builder::addFeature)
        val tag = logTag
        if (tag != null) builder.enableLog(tag) else builder.logger(baseLogger)
        return builder.build()
    }
}

/** FloatingX.install / create 用：额外指定 host */
@FxDsl
public class FxInstallScope : FxConfigScope(null) {
    public var host: FxHost? = null
}

@FxDsl
public class FxGestureScope internal constructor(base: FxGesture) {
    public var click: Boolean = base.click
    public var longPress: Boolean = base.longPress
    public var drag: FxDrag = base.drag
    public var dragRegion: FxRegion? = base.dragRegion
    public var childPriority: FxChildPriority = base.childPriority
    public var touchable: Boolean = base.touchable
    public var longPressTimeout: Long = base.longPressTimeout

    internal fun build(): FxGesture = FxGesture(click, longPress, drag, dragRegion, childPriority, touchable, longPressTimeout)
}
```

`feature/FxFeature.kt`：

```kotlin
package com.petterp.floatingx.core.feature

import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.layout.FxSize

/**
 * 容器行为插件（spec §2.5）。core 内置 Location/Gesture/Animation 三个，
 * 用户与其他模块通过 config.addFeature / control.addFeature 注册。
 * feature 之间不可互相引用，需要共享的数据走 FxFeatureScope。
 */
public interface FxFeature {
    /** 容器挂载到 host 之后调用 */
    public fun onAttach(scope: FxFeatureScope)
    /** 容器从 host 卸下之前调用；必须释放持有的 scope */
    public fun onDetach()
    public fun onConfigChanged(old: FxConfig, new: FxConfig) {}
    public fun onContentSizeChanged(size: FxSize) {}
    public fun onBoundsChanged() {}
    public fun onShow() {}
    public fun onHide() {}
}
```

`feature/FxFeatureScope.kt`：

```kotlin
package com.petterp.floatingx.core.feature

import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.FxLogger
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxLayoutInput

/** feature 能看到的全部东西；由 FxControlImpl 实现 */
public interface FxFeatureScope {
    public val control: FxControl
    public val config: FxConfig
    public val container: FxContainer
    public val host: FxHost
    public val logger: FxLogger?

    /** 当前布局输入；内容尺寸尚无效时为 null */
    public fun layoutInput(): FxLayoutInput?

    /** 提交新锚点：更新 control.anchor、持久化、回调 onPositionChanged */
    public fun commitAnchor(anchor: FxAnchor)

    /** 向所有 FxListener 广播 */
    public fun dispatch(block: (FxListener) -> Unit)

    /** 请求按当前锚点重新定位 */
    public fun requestRelayout()
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxConfigTest*"`
Expected: 6 tests PASS。

- [ ] **Step 5: Commit**

```bash
git add floatingx-core/src
git commit -m "feat(core): FxControl/FxListener 接口、FxConfig Builder 与 Kotlin DSL、FxFeature 插件接口"
```

---

### Task 10: FxControlImpl + 内置 Feature（Location/Gesture/Animation/ModalScrim）+ FloatingX 注册表（Robolectric 端到端）

**Files:**
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/feature/LocationFeature.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/feature/GestureFeature.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/feature/AnimationFeature.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/feature/ModalScrimFeature.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/internal/FxControlImpl.kt`
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/FloatingX.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/TestHost.kt`（测试用 host）
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/FloatingXEndToEndTest.kt`

**Interfaces:**
- Consumes: Task 3–9 全部。
- Produces：`FloatingX { DEFAULT_TAG; install(tag, config, host); install(tag = DEFAULT_TAG) { FxInstallScope }; create(config, host); create { }; control(tag); controlOrNull(tag); controls(); isInstalled(tag); uninstall(tag); uninstallAll() }`；`ModalScrimFeature(dismissOnOutsideTouch = false)`（公开）；`FxControlImpl`（internal，实现 `FxControl + FxEngineDelegate + FxFeatureScope`）；`LocationFeature.relayout()/moveTo()/moveBy()/onDragStart()/onDrag()/onDragEnd()`（internal，供 GestureFeature 与 ControlImpl 调用）。
- 关键约定：内容 view 在 `FxControlImpl` 构造时用 `host.context` 创建；`host.bind(engine)` 在构造函数**最后**调用（host 可能同步回调 `onHostReady`）；`LocationFeature` 在内容尺寸无效时收到 `moveTo` 会记为 `pending`，首次有效布局时应用（#195 的"onCreate 里先 move 再 show"）。

- [ ] **Step 1: 写测试用 host 与端到端测试**

`src/test/kotlin/com/petterp/floatingx/core/TestHost.kt`：

```kotlin
package com.petterp.floatingx.core

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxInsets
import com.petterp.floatingx.core.layout.FxRect

/** 把容器挂到给定 FrameLayout 上的最小 host，等价于 Plan 2 的 ViewGroupHost */
class TestHost(
    private val parent: FrameLayout,
    private val readyOnBind: Boolean = true,
    private val insets: FxInsets = FxInsets.NONE,
) : FxHost {
    override val context: Context get() = parent.context
    var session: FxHostSession? = null
    var attachCount = 0
    var detachCount = 0
    var released = false

    override fun bind(session: FxHostSession) {
        this.session = session
        if (readyOnBind) session.onHostReady()
    }
    override fun createContainer(): FxContainer = FxLayerContainer(parent.context)
    override fun attach(container: FxContainer) {
        parent.addView(container.view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        attachCount++
    }
    override fun detach(container: FxContainer) { parent.removeView(container.view); detachCount++ }
    override fun bounds(): FxBounds = FxBounds(FxRect(0f, 0f, parent.width.toFloat(), parent.height.toFloat()), insets)
    override fun release() { released = true }

    fun ready() = session?.onHostReady()
    fun lose() = session?.onHostLost()
}
```

`src/test/kotlin/com/petterp/floatingx/core/FloatingXEndToEndTest.kt`：

```kotlin
package com.petterp.floatingx.core

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.feature.ModalScrimFeature
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxMargin
import com.petterp.floatingx.core.storage.FxStorage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class FloatingXEndToEndTest {

    private class MemoryStorage : FxStorage {
        val map = HashMap<String, FxAnchor>()
        override fun save(key: String, anchor: FxAnchor) { map[key] = anchor }
        override fun load(key: String): FxAnchor? = map[key]
        override fun clear(key: String) { map.remove(key) }
    }

    private class Events : FxListener {
        val list = mutableListOf<String>()
        override fun onAttach(control: FxControl) { list += "attach" }
        override fun onDetach(control: FxControl) { list += "detach" }
        override fun onShow(control: FxControl) { list += "show" }
        override fun onHide(control: FxControl) { list += "hide" }
        override fun onClick(control: FxControl, view: View) { list += "click" }
        override fun onDragStart(control: FxControl) { list += "dragStart" }
        override fun onDragEnd(control: FxControl, x: Float, y: Float) { list += "dragEnd" }
        override fun onPositionChanged(control: FxControl, anchor: FxAnchor) { list += "anchor:${anchor.gravity}" }
        override fun onCancel(control: FxControl) { list += "cancel" }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val parent = FrameLayout(context)
    private val events = Events()
    private val storage = MemoryStorage()

    private fun content(): View = TextView(context).apply { layoutParams = FrameLayout.LayoutParams(100, 200); text = "fx" }

    private fun config(block: FxConfig.Builder.() -> Unit = {}): FxConfig =
        FxConfig.builder(FxContent.view(content())).anchor(FxGravity.BOTTOM_END).margin(FxMargin.all(16f)).storage(storage).apply(block).build()

    private fun layoutParent() {
        parent.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY))
        parent.layout(0, 0, 1080, 1920)
    }

    private fun positionOf(c: FxControl) = c.contentView!!.let { it.translationX to it.translationY }

    private var time = 0L
    private fun touch(action: Int, x: Float, y: Float) {
        val container = parent.getChildAt(0)
        container.dispatchTouchEvent(MotionEvent.obtain(0L, ++time, action, x, y, 0))
    }

    @After
    fun tearDown() = FloatingX.uninstallAll()

    @Test
    fun `install attaches immediately when host is ready and registers by tag`() {
        val host = TestHost(parent)
        val c = FloatingX.install("a", config(), host)
        assertSame(c, FloatingX.control("a"))
        assertTrue(FloatingX.isInstalled("a"))
        assertEquals(FxState.ATTACHED, c.state)
        assertEquals(1, host.attachCount)
        assertEquals(View.INVISIBLE, c.contentView!!.visibility)
        assertNotNull(c.holder)
    }

    @Test
    fun `show makes content visible and positions by anchor after layout`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.addListener(events)
        c.show()
        assertEquals(FxState.SHOWN, c.state)
        assertTrue(c.isShowing)
        assertEquals(View.VISIBLE, c.contentView!!.visibility)
        layoutParent()
        assertEquals(964f to 1704f, positionOf(c))
        assertEquals(listOf("show"), events.list)
    }

    @Test
    fun `moveTo without animation commits anchor persists and notifies`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.addListener(events)
        c.show(); layoutParent()
        c.moveTo(100f, 300f, animate = false)
        assertEquals(100f to 300f, positionOf(c))
        assertEquals(FxAnchor(FxGravity.TOP_START, 84f, 284f), c.anchor)
        assertEquals(c.anchor, storage.map.values.single())
        assertTrue("anchor:TOP_START" in events.list)
    }

    @Test
    fun `moveTo before host ready is replayed after first layout`() {
        val host = TestHost(parent, readyOnBind = false)
        val c = FloatingX.install("a", config(), host)
        c.moveTo(50f, 60f, animate = false)
        c.show()
        assertEquals(FxState.INSTALLED, c.state)
        host.ready()
        assertEquals(FxState.SHOWN, c.state)
        layoutParent()
        assertEquals(50f to 60f, positionOf(c))
    }

    @Test
    fun `updateContent works before show`() {
        val c = FloatingX.install("a", config(), TestHost(parent, readyOnBind = false))
        c.updateContent { holder -> (holder.view as TextView).text = "changed" }
        assertEquals("changed", (c.contentView as TextView).text)
    }

    @Test
    fun `hide then host lost then ready restores hidden state`() {
        val host = TestHost(parent)
        val c = FloatingX.install("a", config(), host)
        c.addListener(events)
        c.show(); c.hide()
        assertEquals(FxState.ATTACHED, c.state)
        assertEquals(View.INVISIBLE, c.contentView!!.visibility)
        host.lose()
        assertEquals(FxState.INSTALLED, c.state)
        assertEquals(0, parent.childCount)
        host.ready()
        assertEquals(FxState.ATTACHED, c.state)
        assertEquals(1, parent.childCount)
        assertEquals(listOf("show", "hide", "detach", "attach"), events.list)
    }

    @Test
    fun `host lost while shown re-shows on ready keeping the same content view`() {
        val host = TestHost(parent)
        val c = FloatingX.install("a", config(), host)
        c.show()
        val view = c.contentView
        host.lose(); host.ready()
        assertEquals(FxState.SHOWN, c.state)
        assertSame(view, c.contentView)
        assertEquals(2, host.attachCount)
    }

    @Test
    fun `size change keeps the anchored corner`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.show(); layoutParent()
        assertEquals(964f to 1704f, positionOf(c))
        c.contentView!!.layoutParams = FrameLayout.LayoutParams(300, 200)
        c.contentView!!.requestLayout()
        layoutParent()
        assertEquals(764f to 1704f, positionOf(c))          // 右边缘不动（#206/#187）
    }

    @Test
    fun `update changes anchor and relayouts`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.show(); layoutParent()
        c.update { anchor(FxGravity.TOP_START); margin() }
        assertEquals(FxAnchor(FxGravity.TOP_START), c.anchor)
        assertEquals(0f to 0f, positionOf(c))
    }

    @Test
    fun `stored anchor wins over config anchor at install`() {
        storage.map["a:${context.resources.configuration.orientation}"] = FxAnchor(FxGravity.TOP_START, 10f, 20f)
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.show(); layoutParent()
        assertEquals(26f to 36f, positionOf(c))
    }

    @Test
    fun `click on content dispatches onClick`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.addListener(events)
        c.show(); layoutParent()
        touch(MotionEvent.ACTION_DOWN, 970f, 1710f)
        touch(MotionEvent.ACTION_UP, 972f, 1712f)
        assertTrue("click" in events.list)
    }

    @Test
    fun `drag moves content and adsorbs to nearest edge on release`() {
        val c = FloatingX.install("a", config { adsorb(FxAdsorb.horizontal()) }, TestHost(parent))
        c.addListener(events)
        c.show(); layoutParent()
        touch(MotionEvent.ACTION_DOWN, 970f, 1710f)
        touch(MotionEvent.ACTION_MOVE, 900f, 1710f)
        touch(MotionEvent.ACTION_MOVE, 400f, 1200f)
        assertEquals(394f to 1194f, positionOf(c))          // 跟手：位移 = 手指位移
        touch(MotionEvent.ACTION_UP, 400f, 1200f)
        ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)   // 吸附动画 200ms
        assertEquals(16f to 1194f, positionOf(c))
        assertEquals(FxGravity.BOTTOM_START, c.anchor.gravity)
        assertEquals(listOf("show", "dragStart", "dragEnd", "anchor:BOTTOM_START"), events.list.filter { it != "click" })
    }

    @Test
    fun `touch outside content passes through unless modal`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        c.show(); layoutParent()
        val outside = MotionEvent.obtain(0L, 1L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        assertFalse(parent.getChildAt(0).dispatchTouchEvent(outside))

        val modal = FloatingX.install("m", config { addFeature(ModalScrimFeature(dismissOnOutsideTouch = true)) }, TestHost(parent))
        modal.show(); layoutParent()
        assertTrue(parent.getChildAt(1).dispatchTouchEvent(MotionEvent.obtain(0L, 2L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)))
        assertEquals(FxState.ATTACHED, modal.state)          // dismissOnOutsideTouch → hide
    }

    @Test
    fun `cancel detaches releases host and unregisters`() {
        val host = TestHost(parent)
        val c = FloatingX.install("a", config(), host)
        c.addListener(events)
        c.show(); c.cancel()
        assertEquals(FxState.CANCELLED, c.state)
        assertEquals(0, parent.childCount)
        assertTrue(host.released)
        assertNull(FloatingX.controlOrNull("a"))
        assertEquals("cancel", events.list.last())
        assertThrows(IllegalStateException::class.java) { c.show() }
    }

    @Test
    fun `installing the same tag cancels the previous control`() {
        val first = FloatingX.install("a", config(), TestHost(parent))
        val second = FloatingX.install("a", config(), TestHost(parent))
        assertEquals(FxState.CANCELLED, first.state)
        assertSame(second, FloatingX.control("a"))
        assertEquals(listOf(second), FloatingX.controls())
    }

    @Test
    fun `create does not register`() {
        val c = FloatingX.create(config(), TestHost(parent))
        assertTrue(FloatingX.controls().isEmpty())
        c.show()
        assertEquals(FxState.SHOWN, c.state)
        c.cancel()
    }

    @Test
    fun `control with unknown tag throws`() {
        assertThrows(IllegalStateException::class.java) { FloatingX.control("nope") }
        assertNull(FloatingX.controlOrNull("nope"))
    }

    @Test
    fun `calls off the main thread throw`() {
        val c = FloatingX.install("a", config(), TestHost(parent))
        var error: Throwable? = null
        val t = Thread { runCatching { c.show() }.onFailure { error = it } }
        t.start(); t.join()
        assertTrue(error is IllegalStateException)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FloatingXEndToEndTest*"`
Expected: `Unresolved reference: FloatingX` / `ModalScrimFeature`。

- [ ] **Step 3: 实现内置 Feature**

`feature/LocationFeature.kt`：

```kotlin
package com.petterp.floatingx.core.feature

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.host.FxLayoutSpec
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxAdsorbResolver
import com.petterp.floatingx.core.layout.FxLayoutInput
import com.petterp.floatingx.core.layout.FxLayoutResolver
import com.petterp.floatingx.core.layout.FxPoint
import com.petterp.floatingx.core.layout.FxSize

/**
 * 定位（spec §2.3）：位置真值是 control.anchor，这里只负责把它投影到容器，
 * 以及在拖动/moveTo 结束后把坐标反算回锚点提交。
 * 内容尺寸无效（0）时什么都不做，等有效布局——这是 2.x 跳变的根源。
 */
internal class LocationFeature : FxFeature {

    private var scope: FxFeatureScope? = null
    private var animator: ValueAnimator? = null
    /** 内容尺寸无效时收到的 moveTo，首次有效布局后应用 */
    private var pending: FxPoint? = null

    override fun onAttach(scope: FxFeatureScope) {
        this.scope = scope
        relayout()
    }

    override fun onDetach() {
        cancelAnimator()
        scope = null
    }

    override fun onContentSizeChanged(size: FxSize) = relayout()

    override fun onBoundsChanged() = relayout()

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        if (old.anchor != new.anchor || old.margin != new.margin || old.overflow != new.overflow || old.safeArea != new.safeArea) relayout()
    }

    fun relayout() {
        val s = scope ?: return
        val input = s.layoutInput() ?: return
        val p = pending
        if (p != null) {
            pending = null
            settle(s, FxLayoutResolver.clamp(p, input), input, animate = false)
            return
        }
        apply(s, FxLayoutResolver.resolve(s.control.anchor, input))
    }

    fun moveTo(x: Float, y: Float, animate: Boolean) {
        val s = scope ?: return
        val input = s.layoutInput()
        if (input == null) {
            pending = FxPoint(x, y)
            return
        }
        settle(s, FxLayoutResolver.clamp(FxPoint(x, y), input), input, animate)
    }

    fun moveBy(dx: Float, dy: Float, animate: Boolean) {
        val s = scope ?: return
        val cur = s.container.contentPosition()
        moveTo(cur.x + dx, cur.y + dy, animate)
    }

    fun onDragStart() = cancelAnimator()

    /** 拖动中：rebound 时允许暂时出界，否则实时 clamp */
    fun onDrag(dx: Float, dy: Float) {
        val s = scope ?: return
        val input = s.layoutInput() ?: return
        val cur = s.container.contentPosition()
        val next = FxPoint(cur.x + dx, cur.y + dy)
        val rebound = (s.config.adsorb as? FxAdsorb.Edges)?.rebound ?: false
        apply(s, if (rebound) next else FxLayoutResolver.clamp(next, input))
    }

    fun onDragEnd() {
        val s = scope ?: return
        val input = s.layoutInput() ?: return
        val target = FxAdsorbResolver.target(s.container.contentPosition(), input, s.config.adsorb)
        settle(s, target, input, animate = true)
    }

    private fun settle(s: FxFeatureScope, target: FxPoint, input: FxLayoutInput, animate: Boolean) {
        cancelAnimator()
        val from = s.container.contentPosition()
        if (!animate || from == target) {
            apply(s, target)
            commit(s, target, input)
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SETTLE_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val f = va.animatedValue as Float
                apply(s, FxPoint(from.x + (target.x - from.x) * f, from.y + (target.y - from.y) * f))
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) {
                    animator = null
                    if (cancelled) return
                    apply(s, target)
                    commit(s, target, input)
                }
            })
            start()
        }
    }

    private fun apply(s: FxFeatureScope, p: FxPoint) {
        s.host.updateLayout(s.container, FxLayoutSpec(p.x, p.y, s.control.anchor.gravity, s.container.isLtr))
    }

    private fun commit(s: FxFeatureScope, p: FxPoint, input: FxLayoutInput) {
        s.commitAnchor(FxLayoutResolver.toAnchor(p, input))
    }

    private fun cancelAnimator() {
        animator?.cancel()
        animator = null
    }

    private companion object {
        const val SETTLE_DURATION = 200L
    }
}
```

`feature/GestureFeature.kt`：

```kotlin
package com.petterp.floatingx.core.feature

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.container.FxContainerTouchHandler
import com.petterp.floatingx.core.gesture.FxGestureDetector

/** 把容器触摸交给 FxGestureDetector，把识别结果转成 LocationFeature 动作与监听器事件 */
internal class GestureFeature(private val location: LocationFeature) : FxFeature, FxContainerTouchHandler, FxGestureDetector.Callback {

    private var scope: FxFeatureScope? = null
    private var detector: FxGestureDetector? = null

    override fun onAttach(scope: FxFeatureScope) {
        this.scope = scope
        val vc = ViewConfiguration.get(scope.container.view.context)
        detector = FxGestureDetector(vc.scaledTouchSlop.toFloat(), ViewConfiguration.getLongPressTimeout().toLong(), this)
            .also { it.config = scope.config.gesture }
        scope.container.touchHandler = this
    }

    override fun onDetach() {
        detector?.cancel()
        scope?.container?.touchHandler = null
        detector = null
        scope = null
    }

    override fun onConfigChanged(old: FxConfig, new: FxConfig) {
        if (old.gesture != new.gesture) {
            detector?.cancel()
            detector?.config = new.gesture
        }
    }

    override fun onIntercept(ev: MotionEvent): Boolean = detector?.onIntercept(ev) ?: false

    override fun onTouch(ev: MotionEvent): Boolean = detector?.onTouch(ev) ?: false

    override fun onClick() {
        val s = scope ?: return
        val v = s.container.contentView ?: return
        s.dispatch { it.onClick(s.control, v) }
    }

    override fun onLongPress() {
        val s = scope ?: return
        val v = s.container.contentView ?: return
        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        s.dispatch { it.onLongClick(s.control, v) }
    }

    override fun onDragStart() {
        val s = scope ?: return
        location.onDragStart()
        s.dispatch { it.onDragStart(s.control) }
    }

    override fun onDrag(dx: Float, dy: Float) {
        val s = scope ?: return
        location.onDrag(dx, dy)
        val p = s.container.contentPosition()
        s.dispatch { it.onDrag(s.control, p.x, p.y) }
    }

    override fun onDragEnd() {
        val s = scope ?: return
        location.onDragEnd()
        val p = s.container.contentPosition()
        s.dispatch { it.onDragEnd(s.control, p.x, p.y) }
    }

    override fun canDragFrom(x: Float, y: Float): Boolean {
        val s = scope ?: return false
        val region = s.config.gesture.dragRegion ?: return true
        val c = s.container.contentView ?: return false
        return region.contains(x - c.translationX, y - c.translationY, c)
    }

    override fun hasScrollableChildAt(x: Float, y: Float): Boolean {
        val c = scope?.container?.contentView ?: return false
        return findScrollable(c, x - c.translationX, y - c.translationY)
    }

    private fun findScrollable(v: View, x: Float, y: Float): Boolean {
        if (x < 0f || y < 0f || x > v.width || y > v.height) return false
        if (v.canScrollVertically(1) || v.canScrollVertically(-1) || v.canScrollHorizontally(1) || v.canScrollHorizontally(-1)) return true
        if (v is ViewGroup) {
            for (i in v.childCount - 1 downTo 0) {
                val child = v.getChildAt(i)
                if (findScrollable(child, x - child.left - child.translationX, y - child.top - child.translationY)) return true
            }
        }
        return false
    }
}
```

`feature/AnimationFeature.kt`：

```kotlin
package com.petterp.floatingx.core.feature

import android.animation.Animator
import android.animation.AnimatorListenerAdapter

/** 播放 config.animation 的显示/隐藏动画；无动画时立即回调 */
internal class AnimationFeature : FxFeature {

    private var scope: FxFeatureScope? = null
    private var running: Animator? = null

    override fun onAttach(scope: FxFeatureScope) { this.scope = scope }

    override fun onDetach() {
        running?.cancel()
        running = null
        scope = null
    }

    override fun onShow() {
        val s = scope ?: return
        val anim = s.config.animation ?: return
        val v = s.container.contentView ?: return
        running?.cancel()
        running = anim.showAnimator(v).also { it.start() }
    }

    fun playHide(onEnd: () -> Unit) {
        val s = scope
        val anim = s?.config?.animation
        val v = s?.container?.contentView
        if (anim == null || v == null) {
            onEnd()
            return
        }
        running?.cancel()
        running = anim.hideAnimator(v).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    running = null
                    onEnd()
                }
            })
            start()
        }
    }
}
```

`feature/ModalScrimFeature.kt`：

```kotlin
package com.petterp.floatingx.core.feature

import com.petterp.floatingx.core.container.FxLayerContainer

/**
 * 拦截内容之外的触摸（#212），可选点击外部自动 hide（#151）。仅对 Layer 容器生效。
 */
public class ModalScrimFeature @JvmOverloads constructor(
    private val dismissOnOutsideTouch: Boolean = false,
) : FxFeature {

    private var container: FxLayerContainer? = null

    override fun onAttach(scope: FxFeatureScope) {
        val c = scope.container as? FxLayerContainer
        if (c == null) {
            scope.logger?.e("ModalScrimFeature 仅支持 Layer 容器（app/scope），当前容器为 ${scope.container::class.java.simpleName}")
            return
        }
        c.modal = true
        c.onOutsideTouch = { if (dismissOnOutsideTouch) scope.control.hide() }
        container = c
    }

    override fun onDetach() {
        container?.let {
            it.modal = false
            it.onOutsideTouch = null
        }
        container = null
    }
}
```

- [ ] **Step 4: 实现 FxControlImpl 与 FloatingX**

`internal/FxControlImpl.kt`：

```kotlin
package com.petterp.floatingx.core.internal

import android.os.Looper
import android.view.View
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.FxLogger
import com.petterp.floatingx.core.FxState
import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxContent
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxViewHolder
import com.petterp.floatingx.core.engine.FxCommand
import com.petterp.floatingx.core.engine.FxEngine
import com.petterp.floatingx.core.engine.FxEngineDelegate
import com.petterp.floatingx.core.feature.AnimationFeature
import com.petterp.floatingx.core.feature.FxFeature
import com.petterp.floatingx.core.feature.FxFeatureScope
import com.petterp.floatingx.core.feature.GestureFeature
import com.petterp.floatingx.core.feature.LocationFeature
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.layout.FxAnchor
import com.petterp.floatingx.core.layout.FxLayoutInput
import com.petterp.floatingx.core.layout.FxPoint
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 把 engine / host / container / features / listeners 装配在一起。
 * 构造顺序很重要：内容 view 先创建，host.bind() 最后调用（host 可能同步 onHostReady）。
 */
internal class FxControlImpl(
    override val tag: String,
    initialConfig: FxConfig,
    initialHost: FxHost,
    private val onCancelled: ((FxControlImpl) -> Unit)? = null,
) : FxControl, FxEngineDelegate, FxFeatureScope {

    override var config: FxConfig = initialConfig
        private set
    override var host: FxHost = initialHost
        private set
    override var container: FxContainer = initialHost.createContainer()
        private set
    override var contentView: View? = null
        private set
    override var holder: FxViewHolder? = null
        private set
    override var anchor: FxAnchor = initialConfig.anchor
        private set
    override val control: FxControl get() = this
    override val logger: FxLogger? get() = config.logger

    private val engine = FxEngine(this)
    private val listeners = CopyOnWriteArrayList<FxListener>()
    private val location = LocationFeature()
    private val gesture = GestureFeature(location)
    private val animation = AnimationFeature()
    private val features = mutableListOf<FxFeature>(location, gesture, animation)
    private var featuresAttached = false
    private var lastOrientation = initialHost.context.resources.configuration.orientation
    private val mainLooper = Looper.getMainLooper()

    init {
        features += initialConfig.features
        createContent()
        loadStoredAnchor()?.let { anchor = it }
        host.bind(engine)
    }

    // ---------- FxControl ----------

    override val state: FxState get() = engine.state
    override val isShowing: Boolean get() = engine.state == FxState.SHOWN
    override val position: FxPoint get() = container.contentPositionOnScreen()

    override fun show() { main(); engine.show() }

    override fun hide() { main(); engine.hide() }

    override fun cancel() {
        main()
        if (engine.state == FxState.CANCELLED) return
        engine.cancel()
        host.release()
        dispatch { it.onCancel(this) }
        listeners.clear()
        onCancelled?.invoke(this)
    }

    override fun moveTo(x: Float, y: Float) = moveTo(x, y, animate = true)
    override fun moveTo(x: Float, y: Float, animate: Boolean) { main(); engine.dispatch(FxCommand.MoveTo(x, y, animate)) }
    override fun moveBy(dx: Float, dy: Float) = moveBy(dx, dy, animate = true)
    override fun moveBy(dx: Float, dy: Float, animate: Boolean) { main(); engine.dispatch(FxCommand.MoveBy(dx, dy, animate)) }

    override fun update(config: FxConfig) {
        main()
        val old = this.config
        this.config = config
        if (old.anchor != config.anchor) anchor = config.anchor
        if (old.content !== config.content) createContent()
        val removed = old.features - config.features.toSet()
        val added = config.features - old.features.toSet()
        removed.forEach { removeFeature(it) }
        added.forEach { addFeature(it) }
        if (featuresAttached) features.forEach { it.onConfigChanged(old, config) }
    }

    override fun updateContent(block: (FxViewHolder) -> Unit) { main(); holder?.let(block) }

    override fun setContent(content: FxContent) {
        main()
        update(config.toBuilder().content(content).build())
    }

    override fun addListener(listener: FxListener) { listeners.addIfAbsent(listener) }
    override fun removeListener(listener: FxListener) { listeners.remove(listener) }

    override fun addFeature(feature: FxFeature) {
        main()
        if (feature in features) return
        features += feature
        if (featuresAttached) feature.onAttach(this)
    }

    override fun removeFeature(feature: FxFeature) {
        main()
        if (features.remove(feature) && featuresAttached) feature.onDetach()
    }

    // ---------- FxEngineDelegate ----------

    override fun performAttach() {
        container.onContentSizeChanged = { size -> if (featuresAttached) features.forEach { it.onContentSizeChanged(size) } }
        container.onBoundsChanged = { engine.onBoundsChanged() }
        host.attach(container)
        container.setContentVisible(false)
        featuresAttached = true
        features.forEach { it.onAttach(this) }
        dispatch { it.onAttach(this) }
    }

    override fun performDetach() {
        features.forEach { it.onDetach() }
        featuresAttached = false
        container.onContentSizeChanged = null
        container.onBoundsChanged = null
        host.detach(container)
        dispatch { it.onDetach(this) }
    }

    override fun performShow() {
        container.setContentVisible(true)
        features.forEach { it.onShow() }
        dispatch { it.onShow(this) }
    }

    override fun performHide() {
        features.forEach { it.onHide() }
        animation.playHide { container.setContentVisible(false) }
        dispatch { it.onHide(this) }
    }

    override fun perform(command: FxCommand) {
        when (command) {
            is FxCommand.MoveTo -> location.moveTo(command.x, command.y, command.animate)
            is FxCommand.MoveBy -> location.moveBy(command.dx, command.dy, command.animate)
        }
    }

    override fun onBoundsChanged() {
        val orientation = host.context.resources.configuration.orientation
        if (orientation != lastOrientation) {
            lastOrientation = orientation
            loadStoredAnchor()?.let { anchor = it }
        }
        features.forEach { it.onBoundsChanged() }
    }

    override fun swapHost(fallback: FxHost) {
        logger?.d { "[$tag] 切换 host: ${host::class.java.simpleName} -> ${fallback::class.java.simpleName}" }
        host.release()
        host = fallback
        container = fallback.createContainer()
        contentView?.let { container.setContent(it) }
        fallback.bind(engine)
    }

    override fun onStateChanged(old: FxState, new: FxState) {
        logger?.d { "[$tag] $old -> $new" }
    }

    // ---------- FxFeatureScope ----------

    override fun layoutInput(): FxLayoutInput? {
        val size = container.contentSize()
        if (!size.isValid) return null
        return FxLayoutInput(host.bounds(), size, container.isLtr, config.margin, config.overflow, config.safeArea)
    }

    override fun commitAnchor(anchor: FxAnchor) {
        this.anchor = anchor
        config.storage?.save(storageKey(), anchor)
        dispatch { it.onPositionChanged(this, anchor) }
    }

    override fun dispatch(block: (FxListener) -> Unit) {
        for (l in listeners) block(l)
    }

    override fun requestRelayout() = location.relayout()

    // ---------- internal ----------

    private fun createContent() {
        val view = config.content.create(host.context, container.view)
        container.setContent(view)
        contentView = view
        holder = FxViewHolder(view)
    }

    private fun storageKey(): String = "$tag:${host.context.resources.configuration.orientation}"

    private fun loadStoredAnchor(): FxAnchor? = config.storage?.load(storageKey())

    private fun main() {
        check(Looper.myLooper() == mainLooper) { "FloatingX[$tag] 的 API 必须在主线程调用" }
    }
}
```

`FloatingX.kt`：

```kotlin
package com.petterp.floatingx.core

import com.petterp.floatingx.core.config.FxConfig
import com.petterp.floatingx.core.config.FxInstallScope
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.internal.FxControlImpl
import java.util.concurrent.ConcurrentHashMap

/**
 * 全局注册表（spec §2.7）。按 tag 管理全局浮窗；局部浮窗用 create() 不注册。
 * 注册表按进程隔离，不做跨进程（#129）。
 */
public object FloatingX {

    public const val DEFAULT_TAG: String = "FloatingX"

    private val controls = ConcurrentHashMap<String, FxControlImpl>()

    /** 安装并注册；同 tag 已存在则先 cancel 旧的 */
    @JvmStatic
    public fun install(tag: String, config: FxConfig, host: FxHost): FxControl {
        controls.remove(tag)?.cancel()
        val control = FxControlImpl(tag, config, host) { c -> controls.remove(c.tag, c) }
        controls[tag] = control
        return control
    }

    @JvmSynthetic
    public fun install(tag: String = DEFAULT_TAG, block: FxInstallScope.() -> Unit): FxControl {
        val scope = FxInstallScope().apply(block)
        val host = requireNotNull(scope.host) { "install 必须指定 host（appHost / systemHost / viewGroupHost）" }
        return install(tag, scope.build(), host)
    }

    /** 创建但不注册，生命周期由调用方管理（局部浮窗） */
    @JvmStatic
    public fun create(config: FxConfig, host: FxHost): FxControl = FxControlImpl("", config, host)

    @JvmSynthetic
    public fun create(block: FxInstallScope.() -> Unit): FxControl {
        val scope = FxInstallScope().apply(block)
        val host = requireNotNull(scope.host) { "create 必须指定 host" }
        return create(scope.build(), host)
    }

    @JvmStatic @JvmOverloads
    public fun control(tag: String = DEFAULT_TAG): FxControl =
        controls[tag] ?: throw IllegalStateException("未安装 tag=$tag 的浮窗，请先 FloatingX.install")

    @JvmStatic @JvmOverloads
    public fun controlOrNull(tag: String = DEFAULT_TAG): FxControl? = controls[tag]

    /** 当前所有全局浮窗的快照（#133） */
    @JvmStatic
    public fun controls(): List<FxControl> = controls.values.toList()

    @JvmStatic @JvmOverloads
    public fun isInstalled(tag: String = DEFAULT_TAG): Boolean = controls.containsKey(tag)

    @JvmStatic @JvmOverloads
    public fun uninstall(tag: String = DEFAULT_TAG) {
        controls.remove(tag)?.cancel()
    }

    @JvmStatic
    public fun uninstallAll() {
        val all = controls.values.toList()
        controls.clear()
        all.forEach { it.cancel() }
    }
}
```

- [ ] **Step 5: 运行端到端测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FloatingXEndToEndTest*"`
Expected: 18 tests PASS。
- 若 `drag moves content and adsorbs` 里吸附后位置仍是 `394f`：Robolectric 没有推进 ValueAnimator。改用 `org.robolectric.shadows.ShadowChoreographer`：在测试类加 `@Before fun setUp() { ShadowChoreographer.setPaused(true); ShadowChoreographer.setFrameDelay(Duration.ofMillis(16)) }`，并把 `idleMainLooper(500)` 保留；仍不行则在该用例里改为断言 `events.list` 含 `dragEnd` 且 `c.anchor.gravity == BOTTOM_START` 在 idle 后成立，并在提交信息注明动画时序由 Plan 5 instrumentation 覆盖。
- 若 `calls off the main thread throw` 在 Robolectric 下 `Looper.myLooper()` 为 null 导致误判，改断言为 `error != null`。

- [ ] **Step 6: 跑全部 core 测试**

Run: `./gradlew :floatingx-core:test`
Expected: 全绿（含 DependencyBoundaryTest——`FxControlImpl` 等新文件不得引入被禁 import）。

- [ ] **Step 7: Commit**

```bash
git add floatingx-core/src
git commit -m "feat(core): FxControlImpl 装配、内置 Location/Gesture/Animation/ModalScrim feature 与 FloatingX 注册表"
```

---

### Task 11: FxActivityTracker + Java 互操作编译测试 + 收尾

**Files:**
- Create: `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/FxActivityTracker.kt`
- Test: `floatingx-core/src/test/kotlin/com/petterp/floatingx/core/FxActivityTrackerTest.kt`
- Test: `floatingx-core/src/test/java/com/petterp/floatingx/core/JavaApiTest.java`
- Modify: `CLAUDE.md`（仓库根目录；增加 3.0 模块段落，保留 2.x 说明直到旧模块删除）

**Interfaces:**
- Produces: `FxActivityTracker { init(app); topActivity; addObserver(o); removeObserver(o) }` + `FxActivityTracker.Observer { onActivityResumed; onActivityPostResumed; onActivityPaused; onActivityDestroyed }`（全部默认空实现）。Plan 2 的 `AppHost` 与 Plan 3 的 `SystemHost` 黑白名单依赖。
- Java 测试证明：`FxConfig.builder`、`FxAdsorb.horizontal(FxHalfHide)`、`FxGesture.LongPressToDrag`、匿名 `FxListener` 只覆写一个方法、`FloatingX.install(tag, config, host)` 都能从 Java 编译并运行。

- [ ] **Step 1: 写失败测试**

`FxActivityTrackerTest.kt`：

```kotlin
package com.petterp.floatingx.core

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FxActivityTrackerTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = FxActivityTracker.init(app)

    @Test
    fun `tracks resumed activity and clears on destroy`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        assertSame(controller.get(), FxActivityTracker.topActivity)
        controller.pause().stop().destroy()
        assertNull(FxActivityTracker.topActivity)
    }

    @Test
    fun `observers receive lifecycle events`() {
        val seen = mutableListOf<String>()
        val observer = object : FxActivityTracker.Observer {
            override fun onActivityResumed(activity: Activity) { seen += "resumed" }
            override fun onActivityPaused(activity: Activity) { seen += "paused" }
            override fun onActivityDestroyed(activity: Activity) { seen += "destroyed" }
        }
        FxActivityTracker.addObserver(observer)
        Robolectric.buildActivity(Activity::class.java).setup().pause().stop().destroy()
        FxActivityTracker.removeObserver(observer)
        assertEquals(listOf("resumed", "paused", "destroyed"), seen)
    }

    @Test
    fun `init is idempotent`() {
        FxActivityTracker.init(app)
        FxActivityTracker.init(app)
        var resumed = 0
        val observer = object : FxActivityTracker.Observer {
            override fun onActivityResumed(activity: Activity) { resumed++ }
        }
        FxActivityTracker.addObserver(observer)
        Robolectric.buildActivity(Activity::class.java).setup()
        FxActivityTracker.removeObserver(observer)
        assertEquals(1, resumed)
    }
}
```

`JavaApiTest.java`：

```java
package com.petterp.floatingx.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import com.petterp.floatingx.core.config.FxConfig;
import com.petterp.floatingx.core.config.FxContent;
import com.petterp.floatingx.core.gesture.FxGesture;
import com.petterp.floatingx.core.layout.FxAdsorb;
import com.petterp.floatingx.core.layout.FxGravity;
import com.petterp.floatingx.core.layout.FxHalfHide;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** 保证公开 API 对 Java 友好（spec §7）；只覆写一个方法的匿名 FxListener 依赖 jvmDefault=enable */
@RunWith(RobolectricTestRunner.class)
public class JavaApiTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @After
    public void tearDown() {
        FloatingX.uninstallAll();
    }

    @Test
    public void buildersAndListenersCompileFromJava() {
        FxConfig config = FxConfig.builder(FxContent.view(new View(context)))
                .anchor(FxGravity.BOTTOM_END, 0f, 16f)
                .gesture(FxGesture.LongPressToDrag)
                .adsorb(FxAdsorb.horizontal(new FxHalfHide(0.3f)))
                .enableLog("java")
                .build();

        final int[] shown = {0};
        FxListener listener = new FxListener() {
            @Override
            public void onShow(FxControl control) {
                shown[0]++;
            }
        };

        FrameLayout parent = new FrameLayout(context);
        FxControl control = FloatingX.install("java", config, new TestHost(parent, true, com.petterp.floatingx.core.layout.FxInsets.NONE));
        control.addListener(listener);
        control.show();
        control.moveTo(10f, 10f);
        control.updateContent(holder -> {
            assertNotNull(holder.getView());
            return kotlin.Unit.INSTANCE;
        });

        assertEquals(FxState.SHOWN, control.getState());
        assertEquals(1, shown[0]);
        assertEquals("java", FloatingX.control("java").getTag());
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxActivityTrackerTest*" --tests "*JavaApiTest*"`
Expected: `Unresolved reference: FxActivityTracker`（Java 测试同时因 tracker 缺失无法编译）。

- [ ] **Step 3: 实现 FxActivityTracker**

```kotlin
package com.petterp.floatingx.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 前台 Activity 跟踪（spec §2.7）。不再用 ContentProvider 自动初始化：
 * 需要它的 host（app/system）在构造时调用 init(application)。
 * onActivityDestroyed 一定清引用，避免 2.x 的 topActivity 指向已销毁 Activity。
 */
public object FxActivityTracker {

    public interface Observer {
        public fun onActivityResumed(activity: Activity) {}
        /** API 29+ 才会回调；低版本 host 自行在 onActivityResumed 后 post */
        public fun onActivityPostResumed(activity: Activity) {}
        public fun onActivityPaused(activity: Activity) {}
        public fun onActivityDestroyed(activity: Activity) {}
    }

    private var app: Application? = null
    private var top: WeakReference<Activity>? = null
    private val observers = CopyOnWriteArrayList<Observer>()

    public val topActivity: Activity?
        get() = top?.get()

    @JvmStatic
    public fun init(application: Application) {
        if (app === application) return
        app?.unregisterActivityLifecycleCallbacks(callbacks)
        app = application
        application.registerActivityLifecycleCallbacks(callbacks)
    }

    @JvmStatic
    public fun addObserver(observer: Observer) {
        observers.addIfAbsent(observer)
    }

    @JvmStatic
    public fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) {
            top = WeakReference(activity)
            observers.forEach { it.onActivityResumed(activity) }
        }
        override fun onActivityPostResumed(activity: Activity) {
            observers.forEach { it.onActivityPostResumed(activity) }
        }
        override fun onActivityPaused(activity: Activity) {
            observers.forEach { it.onActivityPaused(activity) }
        }
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) {
            if (top?.get() === activity) top = null
            observers.forEach { it.onActivityDestroyed(activity) }
        }
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :floatingx-core:testDebugUnitTest --tests "*FxActivityTrackerTest*" --tests "*JavaApiTest*"`
Expected: 4 tests PASS。若 Java 匿名 `FxListener` 报 "is not abstract and does not override"，说明 `jvmDefault` 未生效——回到 Task 2 的 convention plugin，改用 `freeCompilerArgs.add("-jvm-default=enable")`（KGP 2.2 新写法）或 `"-Xjvm-default=all"`。

- [ ] **Step 5: 全量验证**

Run: `./gradlew test publishToMavenLocal -PisPublish=false -PversionName=3.0.0-SNAPSHOT --stacktrace`
Expected: `BUILD SUCCESSFUL`；`floatingx-core` 全部测试通过；`~/.m2/.../floatingx-core/3.0.0-SNAPSHOT/` 有产物。再跑 `./gradlew :app:assembleDebug` 确认旧 demo 仍可编译。

- [ ] **Step 6: 更新仓库 CLAUDE.md**

在 `CLAUDE.md` 的 `## Project` 表格之后追加一段（不改动其余 2.x 说明）：

```markdown
## 3.0 重构进行中

设计与计划见 `docs/superpowers/specs/2026-08-29-floatingx-3-modular-architecture-design.md`
与 `docs/superpowers/plans/`。新模块 `floatingx-core`（包 `com.petterp.floatingx.core`）已落地，
用 `build-logic/` 的 `floatingx.library` convention plugin；旧的 `floatingx` / `floatingx_compose`
模块在 demo 重写前保留。跑 core 测试：`./gradlew :floatingx-core:test`。
```

- [ ] **Step 7: Commit**

```bash
git add floatingx-core/src CLAUDE.md
git commit -m "feat(core): FxActivityTracker 与 Java 互操作测试；更新 CLAUDE.md"
```

---

## 完成标准（Plan 1）

- `./gradlew test` 全绿，`floatingx-core` 共 127 个用例：`FxLayoutResolverTest`(14)、`FxAdsorbResolverTest`(9)、`FxSpStorageTest`(6)、`FxEngineTest`(12)、`FxGestureDetectorTest`(19)、`FxRegionTest`(5)、`FxLayerContainerTest`(13)、`FxContentTest`(3)、`FxConfigTest`(6)、`FloatingXEndToEndTest`(32)、`FxActivityTrackerTest`(4)、`JavaApiTest`(3)、`DependencyBoundaryTest`(1)。
- `publishToMavenLocal` 产出 `io.github.petterpx:floatingx-core:3.0.0-SNAPSHOT`。
- 旧 demo `app:assembleDebug` 仍可编译。
- core 的 `src/main` 没有任何被禁 import（测试守住）。

## 后续计划（各自单独成文）

- Plan 2：`floatingx-scope`（`ViewGroupHost` + Activity/ViewGroup/Fragment 扩展）与 `floatingx-app`（`AppHost`：Activity 跟踪、re-parent、黑白名单、modal）。
- Plan 3：`floatingx-system`（Window 容器、`LayoutParams.gravity` 映射、权限、fallback、keyboard）。
- Plan 4：`floatingx-compose`（`FxContent.Compose`、owner 归 control、Flow 扩展）。
- Plan 5：demo 重写 + instrumentation 测试 + 删除旧模块 + `MIGRATION.md` / README。

---

## 最终评审修复记录（全分支评审后的一次性修复波）

11 个 Task 完成后做了一次全分支评审，裁决出的修复在两个提交里落地（见 `git log feat/3.0-core`）：

- `fix(core):` 代码修复
  - A1 `FxControlImpl.layoutInput()` 增加父容器 0×0 守卫；A2 settle 改成"先提交锚点再投影"；
    A3 `FxLayoutSpec` 带完整 `anchor`（`gravity` 变成派生属性）。
  - A4 `FxContainer.releaseContent()`，`swapHost` / `cancel()` 调用，修旧容器被内容 view 的 layout 监听拖住的泄漏。
  - A5 `LocationFeature.relayout()` 用最新 `layoutInput()` 给在飞的 settle 收尾；
    显式 `update { anchor(..) }` 优先，丢弃在飞 settle（见下方"与 brief 的差异"）。
  - A6 `swapHost` 端到端用例；A7 `FloatingX.create(config, host, tag = "")`，空 tag 不持久化；
    A8 `FxContainer.isLayer`（不做 `FxFeature.onTouchEvent`）；A9 `FxFeature.onCancel()`。
  - B1 `api(libs.androidx.annotation)`（公开 API 用了 `@IdRes/@LayoutRes`）；B2 几何类型 `@JvmOverloads` +
    `FxLogger.e(message)` 单参重载；B3 `findScrollable` 先判可见性；B4 `FxGestureDetector.cancel(notify)`，
    detach 不补发 `onDragEnd`；B5 DOWN 先清 `consumingOutside`；B6 拖动期间复用 `FxLayoutInput`；
    B7 `POM_NAME` / `POM_DESCRIPTION` 更新为 3.0。
  - C1 `FxRegionTest`；C2 storage 解析容错两例；C3 `onActivityPostResumed`；C4 横竖屏锚点各存一份的重载用例。
- `docs:` 本文件与 spec 的同步、CI 增加 `android-actions/setup-android@v3`。

与 brief 的差异：A5 的 brief 写"relayout 开头无条件用新 input 完成 settle"，但它给的用例要求
`update { anchor(TOP_START) }` 之后锚点仍是 `TOP_START`——无条件收尾会用 settle 的反算锚点覆盖用户刚设的锚点。
实现取"用户显式改锚点时丢弃在飞 settle，其余触发（尺寸/可用区变化）用新 input 收尾"，两个场景各一个用例覆盖。

## 遗留项（执行期 ledger 归档，供 Plan 2–5 参考）

- Task 1: minor (deferred): Groovy 旧脚本的 space-assignment 弃用警告（Gradle 9 不兼容提示），属旧模块，Plan 5 删除旧模块时自然消失。
- Task 2: minor (deferred): convention plugin 为无 androidTest 源集的模块也声明了 androidTestImplementation（计划原文，无害）。
- Task 3: minor (deferred): `clampAxis` 内容比可用区大时的兜底恒为物理左/上，RTL 下未按逻辑 start 对齐（计划原文，极端场景）。
- Task 4: minor (deferred): nearestEdge 平局时依赖 Set 迭代顺序（内置工厂均为有序 set，仅外部传 HashSet 时不确定）。
- Task 5: minor (deferred): FxSpStorage.load 遇到损坏数据静默返回 null 不打日志；apply() 异步无 flush 保证；无按 tag 批量 clear。
- Task 5: minor (deferred): FxSpStorage.load 的 bad gravity / non-float 分支无直接测试（计划测试设计）。
- Task 6: minor (deferred): onHostLost 在 CANCELLED 态仍置 hostReady=false（无可观测影响，与 onHostReady 的守卫不对称）。
- Task 7: minor (deferred): 公开 API 用了 `@IdRes`（androidx.annotation）而 core 对 androidx.core 是 implementation——最终评审决定是否改 api 或显式加 androidx.annotation 依赖；FxRegion.child/rect 无直接单测；onIntercept 阶段无多指转移；CHILD 优先级下长按定时器在子 view 消费期间仍运行。
- Task 8: minor (deferred): FxViewHolder.getViewOrNull 泛型擦除 unchecked cast（2.x 沿袭）；setContent 原地改写调用方传入的 LayoutParams.gravity；contentPositionOnScreen 假设内容 left/top=0；KT-73255 注解目标警告。
- Task 9: minor (deferred): DSL 无法清除已设 logger / 移除 feature；DSL scope 的 mutator 对 Java 可见但不可链式（可考虑 @JvmSynthetic）。
- Task 10: minor (deferred): FloatingX.create() 的 tag 为 ""，多个局部浮窗共用同一 FxStorage 时 key 冲突（最终评审判断是否改为 identity 派生 key）；onDrag 每帧分配 FxPoint + FxLayoutSpec（见 pre-flight ruling）。
- Task 10: minor (deferred): addListener/removeListener 无 main() 检查；settle 期间 relayout 不取消动画（旋转/尺寸变化竞争）；canDragFrom/hasScrollableChildAt 只减 translation（依赖内容 left/top=0）；findScrollable 忽略 GONE/INVISIBLE；GestureFeature.onDetach 的 detector.cancel() 会发一次无锚点提交的 onDragEnd；install/create 无主线程检查；requestRelayout 会消费 pending；onDragEnd 报告的是松手点而非停靠点（KDoc 补充）。
- Task 11: minor (deferred): FxActivityTracker 无 release()（Plan 2/3 host 自行 removeObserver）；onActivityPostResumed 无测试覆盖；计划"完成标准"里的用例计数已过期（14 / 23）。
- Final: minor (deferred): commitAndApply 在 onPositionChanged 之后再 apply，监听器内重入 moveTo/cancel 会出现一帧位置/锚点不一致；拖动中 bounds/size 变化会让 onDrag 提前返回直到 UP；FxLayoutSpec.anchor 在拖动/动画中间帧是已提交的旧锚点（Plan 3 Window 容器中间帧只用 x/y）；settle 期间旋转会把 settle 目标写入新方向的 key；install("") 也会禁用持久化；FxRegionTest unknown-id 用例未覆盖 as? ViewGroup 分支。

### 执行期裁决（Rulings）

- 计划 T10 的 GestureFeature.onDrag / LocationFeature.onDrag 每个 MOVE 分配 FxPoint，与 Global Constraints "触摸 MOVE 路径无对象分配" 冲突 — 先按计划实现（可读性优先，data class 逃逸分析大概率栈上分配），作为 deferred minor 交给最终评审判断是否改为复用字段 — 若错，代价是拖动时少量 GC 抖动，不影响正确性。
- 计划 T1 保留 vanniktech 0.34.0（而非最新 0.37.0）— 仓库工具链不传导，且已知与 AGP 8.x 兼容；若 0.34 与 AGP 8.13 不兼容，T1 文本已授权升级到 0.37.0 — 若错，代价是一次构建失败重试。
- 基线 `./gradlew test` 在 3671f31 上本就失败（AGP 8.1.4 无法解析 demo 的 compose 1.9 / lifecycle 2.9 依赖，要求 AGP ≥ 8.6）——不是本次改动引入，且正是 Task 1 的修复对象；继续执行 — 若错，代价为零（Task 1 验证步骤会重新建立绿色基线）。
- Task 1 实现者把 demo 的 simpleMinSdk 提到 23（material 1.14 / compose 1.11 / appcompat 1.8 的 AAR 都要求 minSdk 23，application 模块 manifest merge 硬失败）— 接受：只影响 demo，库模块 minSdk 仍 21 — 若错，代价是 demo 装不上 API 21/22 设备（无关紧要）。
- 库 minSdk 分层 — core/app/system/scope 保持 21（core 只依赖 androidx.core 1.13.1，minSdk 19），compose 模块 minSdk 23（compose 1.11 / lifecycle 2.11 本身要求 23，Compose 用户已在 23+）；spec §1.1 需补一行，Plan 4 落实 — 若错，代价是 compose 模块使用方需要 minSdk 23（本就如此）。旧 floatingx 模块用 appcompat 1.8 只是过渡，Plan 5 删除。
- `./gradlew test` 仍失败是因为 app / floatingx_compose 从未声明 testImplementation junit 却带着 ExampleUnitTest（历史遗留）— Task 2 的 dispatch 里追加"给这两个模块加 `testImplementation libs.junit`"，保证 CI 的 `./gradlew test` 绿 — 若错，代价是两行多余依赖。
- Task 2 实现者额外加了 build-logic/.gitignore 与 floatingx-core/.gitignore（/build、/.kotlin）— 接受：仓库惯例是按模块 gitignore，否则 `git add floatingx-core` 会带入 build 产物 — 若错，代价是两个多余的小文件。后续新模块（Plan 2–4）沿用此惯例。
- Task 3 计划用例 `content wider than area aligns to start` 的 y 期望 80 与其 gravity CENTER_END 的语义矛盾（应为 840）— 接受实现者改测试期望、不改实现 — 若错，代价为零（x=0 的核心断言未变）。
- `toAnchor` 在 overflow 打开时可产生负 dx/dy，且只产出边角 gravity — 这是 spec §2.3 的既定语义（锚点记录相对最近边的真实偏移，resolve 时再 clamp），无需调整；Task 4/10 按此实现 — 若错，代价是持久化的锚点在关闭 overflow 后被 clamp 回边内（可接受）。
- Robolectric 4.16.1 的 SDK 36 沙箱要求 Java 21，仓库工具链 JDK 17 → robolectric.properties 改 sdk=35（Task 5 dispatch 已授权的唯一兜底）；spec §1.1 "Robolectric 4.16.1（sdk=36）" 需改为 sdk=35，随最终 docs 提交一并更新 — 若错，代价是 Robolectric 用例跑在 API 35 而非 36（core 不用任何 36 新 API）。
- Task 6 实现者提醒 `transition` 先执行 action 再改 state、`FxControlImpl` 不能同时实现 FxHostSession —— Task 10 设计本就如此（Impl 实现 Delegate + FeatureScope，performXxx 内不读 engine.state），无需改动；Task 10 dispatch 里注明 — 若错，代价是 Task 10 评审时发现再改。
- Task 9 实现者担心 update{ addFeature(f) } 重复实例被 onAttach 两次 — Task 10 计划中 FxControlImpl.addFeature 已有 `if (feature in features) return` 去重，update 用集合差计算 added/removed，无需改动 — 若错，代价是 Task 10 评审时补一行去重。
- 五项全部修复（spec §2.2/§2.3/§2.5 是权威，计划原文有缺陷）— 修法：(1) hide 动画监听加 cancelled 守卫；(2) cancel() 加 cancelling 闩；(3) features 改 CopyOnWriteArrayList；(4) update 中 anchor 变化改走 commitAnchor（先赋 config 再 commit，仅当 old.anchor != new.anchor）；(5) createContent 末尾 setContentVisible(engine.state == SHOWN)；同时补 5 个回归测试 — 若错，代价是多写几行防御代码。
- Task 11 的 Java 测试暴露 `FxHalfHide` 缺 `@JvmOverloads`，实现者已修；同类缺口（FxAnchor/FxInsets/FxMargin/FxOverflow/FxBounds 带默认参数的构造）交最终评审的修复波统一处理 — 若错，代价是 Java 端多写几个参数。
- FxLayoutSpec 增加 `anchor: FxAnchor` 字段（gravity 改为由 anchor 派生的属性），Window 容器可直接写 LP.gravity + 偏移 — 发布前改代价最低 — 若错，代价是一个多余字段。
- `FloatingX.create(config, host, tag = "")` 加 tag 参数（@JvmOverloads）；tag 为空且配置了 storage 时跳过持久化并 logger?.e 警告 — 若错，代价是局部浮窗需显式 tag 才能持久化（可接受）。
- spec §2.5 的 `FxFeature.onTouchEvent` 与受限 `FxEngineApi/FxHostInfo` **删除**（YAGNI；触摸只有 GestureFeature 一个消费者；feature 信任模型与 listener 相同），改为给 `FxContainer` 加 `isLayer: Boolean` 判别，避免 downcast；spec 同步修订 — 若错，代价是未来加第二个触摸 feature 时再引入分发链。
- 给 `FxFeature` 加 `onCancel()` 默认空方法（cancel 时在 host.release 前调用），Compose owner 的 destroy 走这里；ViewTree owner 在 FxContent.create() 内设置（attach 先于 feature.onAttach 的顺序保持）；spec §6 补注 — 若错，代价是一个多余的钩子。
- androidx.annotation 改为显式 `api` 依赖（公开 API 带 @IdRes/@LayoutRes 等）；androidx.core 保留 implementation（Plan 2 host 需要 WindowInsetsCompat）。
- 触摸 MOVE 路径的 layoutInput() 改为 onDragStart 时缓存、bounds/size 变化时失效；FxPoint/FxLayoutSpec 每帧分配保持 deferred（Plan 5 用 instrumentation 剖析后再定）。
- modal 在 DOWN 而非 UP 触发外部关闭、hitTest 忽略 scale、touchable=false 对可点击子 view 的透传不完整 — 保持 deferred，交 Plan 5 真机验证。
- A5 简报自相矛盾（"relayout 总是完成 settle" vs "update{anchor} 不得被 settle 覆盖"）— 接受实现者取意图：显式 anchor 变更丢弃进行中的 settle，其它 relayout 触发用新 layoutInput 完成 settle；两条路径各有测试 — 若错，代价是显式 update 后少一次动画收尾。
- POM_DESCRIPTION 保持英文（gradle.properties 按 ISO-8859-1 读取，中文变乱码）— 若错，代价为零。
