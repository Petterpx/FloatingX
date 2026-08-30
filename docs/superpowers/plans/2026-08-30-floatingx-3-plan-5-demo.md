# FloatingX 3.0 Plan 5：demo 重写、旧模块删除、instrumentation 与文档

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除 2.x 的 `floatingx` / `floatingx_compose` 模块与死配置；把 `app` demo 按 3.0 五模块重写成"每个能力一页 + 按 issue 编号的回归页"的完整样例（Kotlin DSL 与 Java Builder 各一份），补 instrumentation 测试与 CI 模拟器任务；重写 README / README_EN，新增 MIGRATION.md，更新 CLAUDE.md 与 copilot-instructions。

**Architecture:** demo 只依赖已发布形态的五个模块（`isDev=true` 时走 project 依赖）。页面统一用一个极小的 `demoPage {}` DSL（section / button / toggle / note）生成 LinearLayout + NestedScrollView，避免每页重复几十行样板；全局浮窗集中在 `DemoWindows`（tag 常量 + 安装函数），页面按钮只调 `FxControl` API。回归页放 `regression/` 包并以 issue 编号命名，每页顶部一段文字说明复现步骤与预期。instrumentation 用 `ActivityScenario` + 真实窗口，系统窗口用例在没有权限时 `Assume` 跳过，CI 用 `appops` 授权。

**Tech Stack:** AGP 8.13.2、Kotlin 2.2.21、compileSdk 36、demo minSdk 23、appcompat 1.8.0、material 1.14.0、compose BOM 2026.06.01（demo 用 material3）、androidx.test 1.7.0 / ext-junit 1.3.0 / espresso 3.7.0、`reactivecircus/android-emulator-runner@v2`。

**Spec:** `docs/superpowers/specs/2026-08-29-floatingx-3-modular-architecture-design.md` §7（API 草案）、§9（issue 矩阵——回归页据此命名）、§10（instrumentation 行）、§11（仓库结构与交付）。Plan 3 计划文件末尾的"真机验证清单"是 instrumentation 与手动验证的输入。

## Global Constraints

- 只删不改：`floatingx/`、`floatingx_compose/`、`check/`、`gradle/dev/` 整目录删除；`settings.gradle` 去掉两个旧 include；版本目录里只有旧模块用的别名（`simpleComposeSdk`、`lifecycleRuntimeKtx`、`lifecycle-runtime-ktx`、`codelocator-core`）删除，其余保留。五个库模块源码本计划**不改**（发现 bug 记入 ledger，交最终评审决定是否开修复波）。
- demo：`app/build.gradle` 保持 Groovy；`applicationId "com.petterp.floatingx.app"`；`namespace "com.petterp.floatingx.app"`；`minSdk 23`、`compileSdk/targetSdk 36`；依赖 `isDev ? project(':floatingx-xxx') : "io.github.petterpx:floatingx-xxx:${rootProject.ext.versionName}"` 五行；`buildFeatures.compose = true`；**manifest 不给任何 Activity 配 `configChanges`**（spec §10：旋转走 recreate）。
- demo 代码：Kotlin 页面全部通过 `demoPage {}` DSL 构建；每个按钮一行调用；页面类名 `XxxActivity`，回归页 `regression/IssueNNNActivity`；Java 样例 `java/JavaDemo.java` 必须编译并能从 MainActivity 触发；注释中文；日志开启 `enableLog("Fx-demo")`。
- instrumentation：`app/src/androidTest`，`AndroidJUnit4`；系统窗口用例在 `Settings.canDrawOverlays == false` 时 `Assume.assumeTrue` 跳过；不依赖 Espresso idling 之外的 sleep（用 `onView(...).check` 与 `InstrumentationRegistry.getInstrumentation().waitForIdleSync()`）。
- 文档：README 与 README_EN 结构一致；所有代码片段必须与实际 API 一致（写完后在 demo 里能找到同样的调用）；MIGRATION.md 以表格列出 2.x 方法 → 3.0 写法。
- 每个 Task 结束 `./gradlew app:assembleDebug test` 全绿（库模块用例数不变：core 133 / scope 22 / app 32 / system 60 / compose 见 Plan 4）；Task 8 之后 `./gradlew app:assembleDebugAndroidTest` 也必须过。提交信息中文、Conventional Commits。

---

## 文件结构

```
settings.gradle                          去掉 ':floatingx'、':floatingx_compose'
gradle/libs.versions.toml                去掉旧别名；加 androidx-test-espresso-contrib？不需要
app/build.gradle                         五模块依赖 + androidTest 依赖
app/proguard-floatingx.pro               保留（内容改为空规则注释）
app/src/main/AndroidManifest.xml         重写：DemoApp、全部页面、DemoService；无 configChanges
app/src/main/java/com/petterp/floatingx/app/
  DemoApp.kt                             Application：enableLog、FloatingX 无自动安装
  DemoWindows.kt                         tag 常量 + installApp()/installSystem()/installCompose()
  ui/DemoPage.kt                         demoPage {} DSL
  ui/DemoContent.kt                      浮窗内容工厂（圆形卡片、可伸缩卡片、带 header 的列表）
  MainActivity.kt                        页面入口 + 快捷操作
  pages/AppHostActivity.kt
  pages/SecondActivity.kt                换页观察 attachedActivity
  pages/BlackActivity.kt                 黑名单页（父类 BaseBlackActivity 演示 #221）
  pages/ImmersedActivity.kt              edge-to-edge / 无状态栏
  pages/SystemHostActivity.kt
  pages/ScopeHostActivity.kt             + ScopeFragment
  pages/GestureActivity.kt
  pages/LayoutActivity.kt
  pages/MultiWindowActivity.kt
  pages/ComposeActivity.kt               + ComposeSecondActivity
  pages/ModalActivity.kt
  service/DemoService.kt                 前台 Service 内安装系统浮窗（#192）
  regression/Issue187Activity.kt         内容尺寸变化不跳动
  regression/Issue221Activity.kt         黑名单父类
  regression/Issue240Activity.kt         App 浮窗拖到底不被裁剪
  regression/Issue244Activity.kt         Fragment 内浮窗
  regression/Issue210Activity.kt         Compose 跨页存活
  java/JavaDemo.java                     Java Builder 样例（app / system / scope）
app/src/main/res/layout/
  fx_card.xml                            圆形卡片（tvTitle）
  fx_card_resizable.xml                  可伸缩卡片（btnGrow / btnShrink）
  fx_list.xml                            header + RecyclerView
  fx_input.xml                           EditText 卡片（键盘）
app/src/androidTest/java/com/petterp/floatingx/app/
  AppHostReparentTest.kt
  RotationTest.kt
  SystemWindowResizeTest.kt
  ComposeOwnerSurvivalTest.kt
  ModalScrimTest.kt
.github/workflows/android.yml            + instrumentation job（emulator-runner）
README.md / README_EN.md                 重写
docs/MIGRATION.md                        新建
CLAUDE.md / .github/copilot-instructions.md   更新
```

---

### Task 1: 删除旧模块、demo 依赖切换、demo 基础设施

**Files:**
- Delete: `floatingx/`、`floatingx_compose/`、`check/`、`gradle/dev/`、`app/src/main/java/com/petterp/floatingx/app/**`（全部旧 demo 源码）、`app/src/main/res/layout/item_floating.xml`、`item_floating_new.xml`、`item_full_screen.xml`
- Modify: `settings.gradle`、`gradle/libs.versions.toml`、`app/build.gradle`、`app/src/main/AndroidManifest.xml`、`app/proguard-floatingx.pro`
- Create: `DemoApp.kt`、`DemoWindows.kt`、`ui/DemoPage.kt`、`ui/DemoContent.kt`、`MainActivity.kt`（本 Task 的 MainActivity 只有快捷操作，页面入口按钮由后续 Task 逐个加）、`res/layout/fx_card.xml`

**Interfaces:**
- Produces:
  - `demoPage(title: String? = null, block: DemoPageScope.() -> Unit)`（`Activity` 扩展，`setContentView`）；`DemoPageScope.section(title)`, `button(text, onClick: (View) -> Unit)`, `toggle(text, initial: Boolean, onChange: (Boolean) -> Unit)`, `note(text)`, `custom(view: View)`；`page(text, cls: Class<out Activity>)`（跳转按钮）。
  - `DemoWindows.TAG_APP = "demo-app"`, `TAG_SYSTEM = "demo-system"`, `TAG_COMPOSE = "demo-compose"`；`installApp(app: Application): FxControl`（`appHost`，黑名单 `BlackActivity`、`theme(R.style.Theme_FloatingX)`）、`installSystem(app, strategy = auto, fallback = true): FxControl`、`ensureApp(app)`（未安装则安装）。
  - `DemoContent.card(ctx, text): View`（100dp 圆形卡片，`fx_card.xml`）；`DemoContent.toast(ctx, msg)`。

- [ ] **Step 1: 删除与构建文件**

```bash
git rm -r -q floatingx floatingx_compose check gradle/dev app/src/main/java app/src/main/res/layout
```

`settings.gradle`：删掉 `include ':floatingx'` 与 `include ':floatingx_compose'`。

`gradle/libs.versions.toml`：删除 `simpleComposeSdk`、`lifecycleRuntimeKtx` 两个 version、`lifecycle-runtime-ktx`、`codelocator-core` 两个 library，以及紧邻的"旧模块 / demo 沿用的别名"注释里已不成立的句子（保留 `simpleMinSdk = "23"` 与 demo 相关说明）。新增 library：

```toml
androidx-test-espresso-contrib = { module = "androidx.test.espresso:espresso-contrib", version.ref = "espresso" }
androidx-test-uiautomator = { module = "androidx.test.uiautomator:uiautomator", version = "2.3.0" }
```

`app/build.gradle`（整体替换）：

```groovy
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace 'com.petterp.floatingx.app'
    compileSdk libs.versions.compileSdk.get().toInteger()

    defaultConfig {
        applicationId "com.petterp.floatingx.app"
        minSdk libs.versions.simpleMinSdk.get().toInteger()
        targetSdk libs.versions.targetSdk.get().toInteger()
        versionCode 300
        versionName "3.0.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-floatingx.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    buildFeatures {
        compose true
    }
    packaging {
        resources { excludes += '/META-INF/{AL2.0,LGPL2.1}' }
    }
}

kotlin {
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
}

dependencies {
    implementation isDev ? project(':floatingx-core') : "io.github.petterpx:floatingx-core:${rootProject.ext.versionName}"
    implementation isDev ? project(':floatingx-app') : "io.github.petterpx:floatingx-app:${rootProject.ext.versionName}"
    implementation isDev ? project(':floatingx-scope') : "io.github.petterpx:floatingx-scope:${rootProject.ext.versionName}"
    implementation isDev ? project(':floatingx-system') : "io.github.petterpx:floatingx-system:${rootProject.ext.versionName}"
    implementation isDev ? project(':floatingx-compose') : "io.github.petterpx:floatingx-compose:${rootProject.ext.versionName}"

    implementation libs.appcompat
    implementation libs.material
    implementation libs.fragment
    implementation platform(libs.compose.bom)
    implementation libs.compose.ui
    implementation libs.compose.material3
    debugImplementation libs.compose.ui.tooling
    debugImplementation libs.leakcanary.android

    testImplementation libs.junit
    androidTestImplementation libs.androidx.test.ext.junit
    androidTestImplementation libs.androidx.test.runner
    androidTestImplementation libs.androidx.test.rules
    androidTestImplementation libs.espresso.core
    androidTestImplementation libs.androidx.test.uiautomator
}
```

`isDev` 与 `versionName` 由 `settings.gradle` 的 `gradle.projectsLoaded` 写进 `rootProject.ext`（旧脚本里的 `$version_name` 从未定义，`isDev=false` 时会报错——这是 2.x 的遗留 bug，本次顺手修掉）。

`app/proguard-floatingx.pro` 内容替换为：

```
# FloatingX 3.0 各模块自带 consumer-rules.pro，demo 无需额外规则
```

- [ ] **Step 2: DemoPage DSL**

`ui/DemoPage.kt`：

```kotlin
package com.petterp.floatingx.app.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

/** 示例页 DSL：section / button / toggle / note / page，一行一个按钮，避免每页重复样板 */
class DemoPageScope(private val root: LinearLayout) {
    private val ctx get() = root.context

    private fun Int.dp(): Int = (this * ctx.resources.displayMetrics.density).toInt()

    fun section(title: String) {
        root.addView(TextView(ctx).apply {
            text = title
            textSize = 13f
            setTextColor(Color.GRAY)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16.dp(), 20.dp(), 16.dp(), 6.dp())
        })
    }

    fun note(text: String) {
        root.addView(TextView(ctx).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.DKGRAY)
            setPadding(16.dp(), 4.dp(), 16.dp(), 8.dp())
        })
    }

    fun button(text: String, onClick: (View) -> Unit) {
        root.addView(MaterialButton(ctx).apply {
            this.text = text
            isAllCaps = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setOnClickListener(onClick)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(16.dp(), 2.dp(), 16.dp(), 2.dp())
        })
    }

    fun toggle(text: String, initial: Boolean, onChange: (Boolean) -> Unit) {
        root.addView(MaterialSwitch(ctx).apply {
            this.text = text
            isChecked = initial
            setOnCheckedChangeListener { _, checked -> onChange(checked) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(20.dp(), 2.dp(), 20.dp(), 2.dp())
        })
    }

    fun custom(view: View) {
        root.addView(view)
    }

    fun page(text: String, cls: Class<out Activity>) = button(text) { it.context.startActivity(Intent(it.context, cls)) }
}

/** Activity 直接 setContentView 一个可滚动的示例页 */
fun Activity.demoPage(title: String? = null, block: DemoPageScope.() -> Unit) {
    title?.let { setTitle(it) }
    val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    DemoPageScope(column).block()
    setContentView(NestedScrollView(this).apply { addView(column) })
}

/** 可以在页面顶部放一个真实的 ViewGroup（局部浮窗宿主）再接示例列表 */
fun Activity.demoPageWithHeader(header: View, title: String? = null, block: DemoPageScope.() -> Unit) {
    title?.let { setTitle(it) }
    val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    outer.addView(header)
    val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    DemoPageScope(column).block()
    outer.addView(NestedScrollView(this).apply { addView(column) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
    setContentView(outer)
}
```

- [ ] **Step 3: 内容工厂与布局**

`res/layout/fx_card.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/fxCard"
    android:layout_width="96dp"
    android:layout_height="96dp"
    app:cardBackgroundColor="#E53935"
    app:cardCornerRadius="48dp"
    app:cardElevation="4dp">

    <TextView
        android:id="@+id/tvTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="Fx"
        android:textColor="@android:color/white"
        android:textSize="14sp" />
</com.google.android.material.card.MaterialCardView>
```

`ui/DemoContent.kt`：

```kotlin
package com.petterp.floatingx.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.petterp.floatingx.app.R

/** 各页共用的浮窗内容 */
object DemoContent {

    /** 圆形卡片；用 theme 化的 context 才能解析 Material 属性 */
    fun card(context: Context, text: String): View =
        LayoutInflater.from(context).inflate(R.layout.fx_card, null, false).also {
            it.findViewById<TextView>(R.id.tvTitle).text = text
        }

    fun toast(context: Context, message: String) {
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
```

- [ ] **Step 4: DemoApp / DemoWindows / MainActivity / manifest**

`DemoApp.kt`：

```kotlin
package com.petterp.floatingx.app

import android.app.Application

/** 3.0 不再需要在 Application 里初始化任何东西：floatingx-app 的 ContentProvider 会自动注册 Activity 跟踪 */
class DemoApp : Application()
```

`DemoWindows.kt`：

```kotlin
package com.petterp.floatingx.app

import android.app.Application
import com.petterp.floatingx.app.pages.BlackActivity
import com.petterp.floatingx.app.ui.DemoContent
import com.petterp.floatingx.appHost
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.FxListener
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxEdge
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.layout.FxHalfHide
import com.petterp.floatingx.core.storage.FxSpStorage
import com.petterp.floatingx.system.permission.FxPermissionStrategy
import com.petterp.floatingx.system.systemHost
import android.view.View

/** 全局浮窗集中在这里安装；页面按钮只操作 FxControl */
object DemoWindows {
    const val TAG_APP = "demo-app"
    const val TAG_SYSTEM = "demo-system"
    const val TAG_COMPOSE = "demo-compose"

    private val clickToast = object : FxListener {
        override fun onClick(control: FxControl, view: View) = DemoContent.toast(view.context, "点击了 ${control.tag}")
        override fun onLongClick(control: FxControl, view: View) = DemoContent.toast(view.context, "长按了 ${control.tag}")
    }

    /** App 级全局浮窗：黑名单页不显示、贴边 + 半隐、位置持久化 */
    fun installApp(app: Application): FxControl = FloatingX.install(TAG_APP) {
        view { ctx -> DemoContent.card(ctx, "App") }
        anchor(FxGravity.CENTER_END, dy = 120f)
        margin(top = 24f, bottom = 24f)
        adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), halfHide = FxHalfHide(0.3f)))
        persist(FxSpStorage(app))
        enableLog("Fx-demo")
        appHost(app) {
            blacklist(BlackActivity::class.java)
            theme(R.style.Theme_FloatingX)
        }
    }.also { it.addListener(clickToast) }

    fun ensureApp(app: Application): FxControl = FloatingX.controlOrNull(TAG_APP) ?: installApp(app)

    /** 系统浮窗：默认自动申请权限，被拒降级为 App 浮窗 */
    fun installSystem(app: Application, strategy: FxPermissionStrategy = FxPermissionStrategy.auto(), fallback: Boolean = true): FxControl =
        FloatingX.install(TAG_SYSTEM) {
            view { ctx -> DemoContent.card(ctx, "Sys") }
            anchor(FxGravity.TOP_START, dx = 24f, dy = 200f)
            adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END)))
            persist(FxSpStorage(app))
            enableLog("Fx-demo")
            systemHost(app) {
                permission(strategy)
                if (fallback) fallback(com.petterp.floatingx.app.AppHost.builder(app).theme(R.style.Theme_FloatingX).build())
            }
        }.also { it.addListener(clickToast) }

    fun ensureSystem(app: Application): FxControl = FloatingX.controlOrNull(TAG_SYSTEM) ?: installSystem(app)
}
```

注意 `appHost` 的 import 路径以实际为准（`com.petterp.floatingx.app.appHost`，DSL 在 `FxAppExt.kt`），与 demo 自己的包 `com.petterp.floatingx.app` 同名——demo 内引用库的 `AppHost` 时用全限定名 `com.petterp.floatingx.app.AppHost` 避免歧义；实现时若 Kotlin 报解析冲突，把 demo 包改名为 `com.petterp.floatingx.demo`（`namespace`/`applicationId` 不变），并在报告里注明。**建议直接采用 `com.petterp.floatingx.demo` 作为 demo 包名**，本计划后续文件路径按 `app/src/main/java/com/petterp/floatingx/demo/...` 理解。

`MainActivity.kt`：

```kotlin
package com.petterp.floatingx.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.demo.ui.demoPage

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoPage("FloatingX 3.0") {
            section("快捷操作")
            button("显示 App 全局浮窗") { DemoWindows.ensureApp(application).show() }
            button("显示系统浮窗（自动申请权限，拒绝则降级）") { DemoWindows.ensureSystem(application).show() }
            button("隐藏全部全局浮窗") { FloatingX.controls().forEach { it.hide() } }
            button("卸载全部全局浮窗") { FloatingX.uninstallAll() }
            section("能力页")
            // 后续 Task 逐个追加 page(...)
        }
    }
}
```

`AndroidManifest.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".DemoApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="FloatingX"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.FloatingX">

        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <!-- 后续 Task 追加页面；一律不配 configChanges（spec §10：旋转走 recreate） -->
    </application>
</manifest>
```

`res/values/themes.xml` 里已有 `Theme.FloatingX`（检查名字；没有就把现有主题改名为 `Theme.FloatingX`，父主题 `Theme.Material3.DayNight.NoActionBar` 或现有值）。

- [ ] **Step 5: 验证与提交**

Run: `./gradlew app:assembleDebug test`
Expected: BUILD SUCCESSFUL；库模块用例数不变；`app` 无单测（`ExampleUnitTest` 已随旧源码删除，`testImplementation libs.junit` 保留无害）。

```bash
git add -A
git commit -m "refactor: 删除 2.x 模块与死配置；demo 切换到 3.0 五模块并建立示例页 DSL"
```

---

### Task 2: AppHost 页、换页观察页、黑名单页、沉浸页

**Files:**
- Create: `pages/AppHostActivity.kt`、`pages/SecondActivity.kt`、`pages/BlackActivity.kt`（含 `open class BaseBlackActivity`）、`pages/ImmersedActivity.kt`
- Modify: `MainActivity.kt`（追加 `page("App 级全局浮窗", AppHostActivity::class.java)` 等 4 个入口）、`AndroidManifest.xml`（4 个 activity；`ImmersedActivity` 用 `android:theme="@style/Theme.FloatingX.Immersed"`——在 `themes.xml` 加该主题：`windowTranslucentStatus=false`、`statusBarColor=@android:color/transparent`、`windowLayoutInDisplayCutoutMode=shortEdges`）

**页面按钮（每个一行调用，`c` = `DemoWindows.ensureApp(application)`）：**

`AppHostActivity`：
- section("显示/隐藏")：`显示` → `c.show()`；`隐藏` → `c.hide()`；`cancel（卸载）` → `c.cancel()`
- section("移动")：`moveTo(100,300)` → `c.moveTo(100f, 300f)`；`moveBy(-40,0)` → `c.moveBy(-40f, 0f)`；`moveTo 不带动画` → `c.moveTo(0f, 0f, animate = false)`
- section("锚点 / 边界")：9 个 gravity 按钮 → `c.update { anchor(FxGravity.X) }`；`margin 全 48` → `c.update { margin(48f, 48f, 48f, 48f) }`；`允许上下越界` → `c.update { overflow(top = true, bottom = true) }`；toggle("safeArea", true) → `c.update { safeArea = it }`
- section("吸附")：`四向吸附` → `c.update { adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END, FxEdge.TOP, FxEdge.BOTTOM))) }`；`左右吸附 + 半隐 30%` → `…halfHide = FxHalfHide(0.3f)`；`关闭吸附` → `c.update { adsorb(FxAdsorb.None) }`；toggle("越界回弹") → `Edges(..., rebound = it)`
- section("内容")：`换成 layout 内容` → `c.setContent(FxContent.layout(R.layout.fx_card))`；`改标题文字` → `c.updateContent { it.setText(R.id.tvTitle, "Hi") }`
- section("动画")：`slideIn` → `c.update { animation(FxAnimations.slideIn()) }`；`无动画` → `c.update { animation(null) }`
- section("换页 / 过滤")：page("进入第二页（观察 attachedActivity）", SecondActivity)；page("进入黑名单页（浮窗消失）", BlackActivity)；page("进入沉浸页（无状态栏）", ImmersedActivity)；note("当前挂载：" + `c.attachedActivity?.javaClass?.simpleName`，用 `custom(TextView)` 并在 `onResume` 刷新)

`SecondActivity`：note 说明"浮窗应无动画地跟到本页"，button `attachedActivity` → toast `c.attachedActivity?.javaClass?.simpleName`；button `返回` → `finish()`。

`BlackActivity`：`open class BaseBlackActivity : AppCompatActivity()`，`class BlackActivity : BaseBlackActivity()`；页面 note："本页在黑名单（按父类 BaseBlackActivity 命中，#221），浮窗不显示；返回后恢复"。`DemoWindows.installApp` 的 blacklist 改为 `BaseBlackActivity::class.java`。

`ImmersedActivity`：`WindowCompat.setDecorFitsSystemWindows(window, false)` + 隐藏状态栏（`WindowInsetsControllerCompat.hide(statusBars())`），note："safeArea 打开时浮窗不进刘海/导航栏区域；关闭 safeArea 可贴到屏幕边"。toggle("safeArea") → `c.update { safeArea = it }`。

- [ ] Step 1 写四个页面 + manifest + MainActivity 入口；Step 2 `./gradlew app:assembleDebug`；Step 3 提交 `feat(demo): AppHost 示例页、换页/黑名单/沉浸页`。

`FxAnimations.slideIn()` 以 core `animation/FxAnimations.kt` 的实际工厂名为准（实现者先 `grep -n 'fun ' floatingx-core/src/main/kotlin/com/petterp/floatingx/core/animation/FxAnimations.kt`）。

---

### Task 3: SystemHost 页 + 前台 Service 安装（#192）

**Files:**
- Create: `pages/SystemHostActivity.kt`、`service/DemoService.kt`、`res/layout/fx_input.xml`（MaterialCardView 内一个 `EditText id=etInput` + `TextView`）
- Modify: `DemoWindows.kt`（`installSystem` 加 `keyboard: Boolean = false` 参数：true 时内容用 `fx_input.xml`、`keyboard(R.id.etInput)`、`onBackPressed { toast; true }`）、`MainActivity`、manifest（activity + `<service android:name=".service.DemoService" android:foregroundServiceType="specialUse" android:exported="false">` 带 `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="floating window demo" />`）

**`SystemHostActivity` 按钮（`s` = `DemoWindows.ensureSystem(application)`）：**
- section("权限")：note 显示 `FxPermission.isGranted(this)`（onResume 刷新）；`Auto（默认）安装并显示` → `DemoWindows.installSystem(application).show()`；`Manual：弹对话框再申请` → `installSystem(application, FxPermissionStrategy.manual { req -> AlertDialog… 去开启→req.proceed()；降级→req.useFallback()；取消→req.deny() }).show()`；`Skip（不检查权限，type 由 customizer 决定）` → `installSystem(application, FxPermissionStrategy.skip(), fallback = false).show()`；`retryPermission()` → `(s.host as? SystemHost)?.retryPermission()`；`打开系统设置页` → `startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))`
- section("LayoutParams")：`alpha 0.5` → 重新安装并 `systemHost(app) { layoutParams { it.alpha = 0.5f } }`；`读取当前 LP` → toast `(s.host as SystemHost).windowLayoutParams.let { "type=${it.type} flags=${it.flags} x=${it.x} y=${it.y}" }`
- section("触摸")：toggle("touchable", true) → `s.update { gesture { touchable = it } }`
- section("键盘 / 返回键")：`安装带 EditText 的系统浮窗` → `installSystem(application, keyboard = true).show()`；note："点 EditText 弹键盘；按返回收起键盘，不会触发 onBackPressed"
- section("Service")：`从前台 Service 安装（#192）` → `ContextCompat.startForegroundService(this, Intent(this, DemoService::class.java))`；`停止 Service` → `stopService(...)`
- section("降级")：`拒绝权限时看降级` note；`卸载系统浮窗` → `FloatingX.uninstall(DemoWindows.TAG_SYSTEM)`

`DemoService`：`onStartCommand` → `startForeground(1, notification)`（`NotificationChannel` "fx"，`NotificationCompat`，`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`）→ `DemoWindows.ensureSystem(application).show()`；`onDestroy` 不 cancel（浮窗归注册表）。API 33+ 通知权限未授予时仍可 startForeground（通知不显示，属预期）。

- [ ] Step 1–3 同 Task 2；提交 `feat(demo): SystemHost 示例页（权限策略 / LayoutParams / 键盘 / Service）`。

---

### Task 4: Scope 页（Activity / ViewGroup / Fragment）+ Issue244

**Files:**
- Create: `pages/ScopeHostActivity.kt`（含 `class ScopeFragment : Fragment()`）、`regression/Issue244Activity.kt`
- Modify: `MainActivity`、manifest

`ScopeHostActivity`（`demoPageWithHeader(header = FrameLayout 240dp 高、黄色背景, …)`）：
- `private val boxFx by lazy { header.fxScope("scope-box") { view { DemoContent.card(it, "Box") }; anchor(FxGravity.TOP_START); enableLog("Fx-demo") } }`
- `private val actFx by lazy { fxScope("scope-act") { view { DemoContent.card(it, "Act") }; anchor(FxGravity.BOTTOM_END); persist(FxSpStorage(this)) } }`
- section("ViewGroup 内浮窗")：`显示` → `boxFx.show()`；`隐藏`；`禁止拖动` → `boxFx.update { gesture { drag = FxDrag.DISABLED } }`；`长按后可拖` → `FxDrag.AFTER_LONG_PRESS`；`恢复` → `FxDrag.IMMEDIATE`
- section("Activity 内浮窗")：`显示` / `隐藏` / `cancel`；note："API 29+ 页面销毁自动 cancel"
- section("Fragment")：`添加 Fragment（内含浮窗）` → `supportFragmentManager.beginTransaction().replace(R.id.fragmentSlot, ScopeFragment()).commit()`（header 下再放一个 `FrameLayout id=fragmentSlot` 高 200dp）；`移除 Fragment` → remove；`detach / attach Fragment` → 演示 view 销毁重建后浮窗自动回来
- `ScopeFragment.onCreate` 里 `fxScope("scope-frag") { view { DemoContent.card(it, "Frag") } }.show()`（在 view 创建前调用，正是 #244 场景）；`onCreateView` 返回一个 FrameLayout。
- Java：`JavaDemo.createScope(activity)`（Task 7）

`Issue244Activity`：note 复现步骤 + 一个按钮 add Fragment；与 ScopeFragment 共用。

- [ ] 提交 `feat(demo): Scope 示例页（Activity / ViewGroup / Fragment）与 Issue244 回归页`。

---

### Task 5: 手势页、布局页、多窗页、Modal 页 + Issue187 / 221 / 240

**Files:**
- Create: `pages/GestureActivity.kt`、`pages/LayoutActivity.kt`、`pages/MultiWindowActivity.kt`、`pages/ModalActivity.kt`、`regression/Issue187Activity.kt`、`regression/Issue221Activity.kt`、`regression/Issue240Activity.kt`、`res/layout/fx_card_resizable.xml`（卡片 + `btnGrow`/`btnShrink`）、`res/layout/fx_list.xml`（`tvHeader` + `RecyclerView id=rv` 300dp 高）
- Modify: `DemoContent.kt`（`resizable(ctx)`、`list(ctx, adapter)`）、`MainActivity`、manifest

`GestureActivity`（用局部浮窗 `fxScope("gesture")` 内容 `fx_list.xml`，RecyclerView 20 行）：
- section("拖动模式")：IMMEDIATE / AFTER_LONG_PRESS / DISABLED → `update { gesture { drag = … } }`
- section("拖动区域")：`只允许 header 拖动` → `gesture { dragRegion = FxRegion.child(R.id.tvHeader) }`；`任意位置` → `dragRegion = null`
- section("子 view 优先级")：`AUTO（列表可滚时列表优先）` / `PARENT` / `CHILD` → `gesture { childPriority = FxChildPriority.X }`
- section("透传")：toggle("touchable") 
- section("回调")：`addListener` 打印 onClick/onLongClick/onDragStart/onDragEnd 到页面底部 TextView（custom）
`FxRegion` / `FxChildPriority` 名称以 core `gesture/` 为准。

`LayoutActivity`（`fxScope("layout")`，内容 `DemoContent.resizable`）：
- section("锚点")：9 gravity；section("越界")：4 个 toggle → `overflow(...)`；section("吸附")：同 Task 2；section("内容尺寸")：`变大` → 内容 view 宽高 +40dp（`layoutParams` 改后 `requestLayout`）；`变小`；note："尺寸变化时锚点不动（#187）"；section("持久化")：`persist(FxSpStorage)` toggle；`清除记忆` → `FxSpStorage(this).clear("layout:1"); clear("layout:2")`；note："旋转屏幕后位置按方向分别恢复"

`MultiWindowActivity`：`显示 tag1 / tag2` → `DemoWindows.ensureApp` 与新 tag `"demo-app-2"`（`installApp` 加 tag 参数）；`遍历 controls()` → toast 列表；`isInstalled`；`重复 install 同 tag` → 观察旧的被 cancel；`uninstallAll`。

`ModalActivity`：`fxScope("modal") { …; modal(dismissOnOutsideTouch = true) }`；toggle("modal") → `update { modal(it) }`；`Dialog 之上的浮窗` → `AlertDialog` 显示后 `FloatingX.create("dialog") { view {…}; viewGroupHost(dialog.window!!.decorView as ViewGroup) }.show()`，dialog dismiss 时 cancel。

`Issue187Activity`：系统浮窗（若有权限）或 App 浮窗，内容 `resizable`，锚点 `BOTTOM_END`；按钮"变大/变小/切换锚点"；note 预期"右下角不动"。
`Issue221Activity`：note 说明 `blacklist(BaseBlackActivity::class.java)` 命中子类；按钮跳 `BlackActivity`。
`Issue240Activity`：App 浮窗 + `overflow(bottom = true)`；note"拖到导航栏区域不被裁剪"（DecorView 挂载）。

- [ ] 提交 `feat(demo): 手势 / 布局 / 多窗 / Modal 示例页与 Issue187/221/240 回归页`。

---

### Task 6: Compose 页 + Issue210

**Files:**
- Create: `pages/ComposeActivity.kt`、`pages/ComposeSecondActivity.kt`、`regression/Issue210Activity.kt`
- Modify: `DemoWindows.kt`（`installCompose(app)`）、`MainActivity`、manifest

`DemoWindows.installCompose`：

```kotlin
    class CounterViewModel : ViewModel() { var clicks = 0 }

    fun installCompose(app: Application): FxControl = FloatingX.install(TAG_COMPOSE) {
        compose { control ->
            val vm: CounterViewModel = viewModel()          // 归 FxComposeOwner 的 ViewModelStore
            var count by rememberSaveable { mutableIntStateOf(0) }
            val state by control.stateFlow().collectAsState()
            val pos by control.positionFlow().collectAsState()
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(110.dp)) {
                Column(Modifier.clickable { count++; vm.clicks++ }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("count $count", color = Color.White)
                    Text("vm ${vm.clicks}", color = Color.White, fontSize = 11.sp)
                    Text("${pos.x.toInt()},${pos.y.toInt()} $state", color = Color.White, fontSize = 9.sp)
                }
            }
        }
        anchor(FxGravity.CENTER_START, dy = -100f)
        enableLog("Fx-demo")
        appHost(app) { blacklist(BaseBlackActivity::class.java) }
    }
```

`viewModel()` 来自 `androidx.lifecycle:lifecycle-viewmodel-compose`——demo 加依赖 `implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0"`（与 compose 模块的 lifecycle 一致）；`collectAsState` 在 compose runtime。

`ComposeActivity`：`显示 Compose 浮窗` / `隐藏` / `cancel`；page("进入第二页：count 与 vm 应保持", ComposeSecondActivity)；page("进入黑名单页：浮窗消失，返回后 count 保持", BlackActivity)；`旋转提示` note："旋转后 count 保持（rememberSaveable 走 FxComposeOwner）"；`系统窗口版 compose` → `FloatingX.install("demo-compose-sys") { compose {…同上…}; systemHost(app) { fallback(...) } }.show()`。

`ComposeSecondActivity`：note + 返回按钮。`Issue210Activity`：note 复现（Compose 浮窗跨页消失）+ 按钮组合。

- [ ] 提交 `feat(demo): Compose 示例页（ViewModel / rememberSaveable / stateFlow / positionFlow）与 Issue210 回归页`。

---

### Task 7: Java 样例 + MainActivity 收口

**Files:**
- Create: `java/JavaDemo.java`
- Modify: `MainActivity.kt`（section("Java") 三个按钮；section("回归页") 五个入口；section("能力页") 全部入口按顺序）

`JavaDemo.java`：

```java
package com.petterp.floatingx.demo.java;

import android.app.Activity;
import android.app.Application;
import android.view.WindowManager;

import com.petterp.floatingx.app.AppActivityFilter;
import com.petterp.floatingx.app.AppHost;
import com.petterp.floatingx.core.FloatingX;
import com.petterp.floatingx.core.FxControl;
import com.petterp.floatingx.core.FxListener;
import com.petterp.floatingx.core.config.FxConfig;
import com.petterp.floatingx.core.config.FxContent;
import com.petterp.floatingx.core.gesture.FxGesture;
import com.petterp.floatingx.core.layout.FxAdsorb;
import com.petterp.floatingx.core.layout.FxEdge;
import com.petterp.floatingx.core.layout.FxGravity;
import com.petterp.floatingx.core.layout.FxHalfHide;
import com.petterp.floatingx.core.storage.FxSpStorage;
import com.petterp.floatingx.demo.R;
import com.petterp.floatingx.demo.pages.BaseBlackActivity;
import com.petterp.floatingx.scope.ViewGroupHost;
import com.petterp.floatingx.system.SystemHost;
import com.petterp.floatingx.system.permission.FxPermissionStrategy;

import java.util.Collections;
import java.util.EnumSet;

/** Java 侧的 3.0 用法（spec §7）；每个方法对应 MainActivity 的一个按钮 */
public final class JavaDemo {
    public static final String TAG_APP = "java-app";
    public static final String TAG_SYSTEM = "java-system";

    private JavaDemo() {}

    private static FxConfig config(Application app) {
        return FxConfig.builder(FxContent.layout(R.layout.fx_card))
                .anchor(FxGravity.BOTTOM_START, 0f, -120f)
                .margin(16f, 16f, 16f, 16f)
                .adsorb(new FxAdsorb.Edges(EnumSet.of(FxEdge.START, FxEdge.END), new FxHalfHide(0.3f), true))
                .gesture(FxGesture.LongPressToDrag)
                .storage(new FxSpStorage(app))
                .enableLog("Fx-java")
                .build();
    }

    public static FxControl installApp(Application app) {
        AppHost host = AppHost.builder(app)
                .blacklist(BaseBlackActivity.class)
                .filter(activity -> !activity.isFinishing())
                .theme(R.style.Theme_FloatingX)
                .build();
        FxControl control = FloatingX.install(TAG_APP, config(app), host);
        control.addListener(new FxListener() {
            @Override public void onClick(FxControl c, android.view.View view) { /* 日志见 Fx-java */ }
        });
        control.show();
        return control;
    }

    public static FxControl installSystem(Application app) {
        SystemHost host = SystemHost.builder(app)
                .layoutParams(lp -> lp.alpha = 0.9f)
                .permission(FxPermissionStrategy.auto())
                .fallback(AppHost.builder(app).theme(R.style.Theme_FloatingX).build())
                .build();
        FxControl control = FloatingX.install(TAG_SYSTEM, config(app), host);
        control.show();
        return control;
    }

    public static FxControl createScope(Activity activity) {
        android.view.ViewGroup content = activity.findViewById(android.R.id.content);
        FxControl control = FloatingX.create(config(activity.getApplication()), ViewGroupHost.of(content), "java-scope");
        control.show();
        return control;
    }
}
```

`FxAdsorb.Edges` 的 Java 构造参数顺序、`FxGesture.LongPressToDrag` 常量名、`FxSpStorage` 构造以 core 实际为准（`grep` 后写）。

- [ ] 提交 `feat(demo): Java Builder 样例与 MainActivity 收口`。

---

### Task 8: instrumentation 测试 + CI 模拟器任务

**Files:**
- Create: `app/src/androidTest/java/com/petterp/floatingx/demo/AppHostReparentTest.kt`、`RotationTest.kt`、`SystemWindowResizeTest.kt`、`ComposeOwnerSurvivalTest.kt`、`ModalScrimTest.kt`、`TestUtil.kt`
- Modify: `.github/workflows/android.yml`

**共用 `TestUtil.kt`：**

```kotlin
package com.petterp.floatingx.demo

import android.app.Activity
import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petterp.floatingx.core.FxControl
import org.junit.Assume.assumeTrue

val app: Application get() = ApplicationProvider.getApplicationContext()

fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

fun idle() = InstrumentationRegistry.getInstrumentation().waitForIdleSync()

/** 没有悬浮窗权限时跳过（CI 用 appops 授权） */
fun assumeOverlayPermission() = assumeTrue("需要 SYSTEM_ALERT_WINDOW 权限", Settings.canDrawOverlays(app))

/** 等到浮窗有有效位置（首帧布局完成） */
fun FxControl.awaitPositioned(timeoutMs: Long = 3000): FxControl {
    val end = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < end) {
        var ok = false
        onMain { ok = contentView?.width ?: 0 > 0 }
        if (ok) return this
        Thread.sleep(16)
    }
    error("浮窗在 ${timeoutMs}ms 内未完成布局")
}
```

**`AppHostReparentTest`（spec §10 "A→B→back 挂载顺序与位置"）：**

```kotlin
@RunWith(AndroidJUnit4::class)
class AppHostReparentTest {
    @get:Rule val scenario = ActivityScenarioRule(AppHostActivity::class.java)

    @After fun tearDown() = onMain { FloatingX.uninstallAll() }

    @Test
    fun position_and_state_survive_activity_switch() {
        lateinit var control: FxControl
        onMain { control = DemoWindows.installApp(app).also { it.show() } }
        control.awaitPositioned()
        onMain { control.moveTo(200f, 400f, animate = false) }
        idle()
        val before = control.position
        val second = ActivityScenario.launch(SecondActivity::class.java)
        idle()
        assertEquals(FxState.SHOWN, control.state)
        assertTrue(control.attachedActivity is SecondActivity)
        assertEquals(before.x, control.position.x, 1f)
        assertEquals(before.y, control.position.y, 1f)
        second.close()
        idle()
        assertTrue(control.attachedActivity is AppHostActivity)
    }

    @Test
    fun blacklisted_activity_detaches_and_back_restores() {
        lateinit var control: FxControl
        onMain { control = DemoWindows.installApp(app).also { it.show() } }
        control.awaitPositioned()
        val black = ActivityScenario.launch(BlackActivity::class.java)
        idle()
        assertEquals(FxState.INSTALLED, control.state)
        black.close()
        idle()
        assertEquals(FxState.SHOWN, control.state)
    }
}
```

**`RotationTest`（spec §10 "旋转"）：** `scenario.onActivity { it.requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE }` → `idle()` → 断言 `control.state == SHOWN`、`attachedActivity` 是新实例、`control.anchor.gravity` 不变；再转回竖屏，断言位置回到旋转前（`persist(FxSpStorage)` 按方向存）。用 `UiDevice.getInstance(instrumentation).setOrientationNatural()` 收尾。

**`SystemWindowResizeTest`（spec §10 "WM 窗口 resize 时 LayoutParams 序列无跳变"，#187）：** `assumeOverlayPermission()`；安装系统浮窗，内容 `DemoContent.resizable`，锚点 `BOTTOM_END`；记录 `(host as SystemHost).windowLayoutParams` 的 `gravity/x/y`；主线程把内容宽高 +80px 并 `requestLayout`；`idle()` 后再读——断言 `gravity` 仍 `BOTTOM|RIGHT` 且 `x/y` 与之前相等（锚定边偏移不变，WM 自己保持右下角）；`control.position` 的 x 减少了 80。

**`ComposeOwnerSurvivalTest`（spec §10 "Compose owner 跨 Activity 存活"）：** 安装 `DemoWindows.installCompose(app)`，`idle()`；取 `(control.config.content as FxComposeContent).owner` 与 `ViewModelProvider(owner)[CounterViewModel::class.java]`；启动 `ComposeSecondActivity`；断言 owner 同一实例、`lifecycle.currentState == RESUMED`、同一个 ViewModel；关闭第二页后仍成立；`control.cancel()` 后 `owner.isDestroyed`。

**`ModalScrimTest`（spec §10 "Layer 容器 modal scrim"）：** `ModalActivity` 页面里 `fxScope("modal") { modal(dismissOnOutsideTouch = true) }` 显示后，用 Espresso `onView(withText("modal 下方按钮")).perform(click())`——页面提供一个计数按钮；断言点击计数为 0 且浮窗已 hide（`dismissOnOutsideTouch`）；关闭 modal 后再次点击计数为 1。

**CI（`.github/workflows/android.yml`）新增 job：**

```yaml
  instrumentation:
    runs-on: ubuntu-latest
    needs: build
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - uses: android-actions/setup-android@v3
      - name: Enable KVM
        run: |
          echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' | sudo tee /etc/udev/rules.d/99-kvm4all.rules
          sudo udevadm control --reload-rules && sudo udevadm trigger --name-match=kvm
      - uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          arch: x86_64
          profile: pixel_6
          script: |
            ./gradlew app:installDebug app:installDebugAndroidTest
            adb shell appops set com.petterp.floatingx.app SYSTEM_ALERT_WINDOW allow
            adb shell settings put global window_animation_scale 0
            adb shell settings put global transition_animation_scale 0
            adb shell settings put global animator_duration_scale 0
            ./gradlew app:connectedDebugAndroidTest
      - uses: actions/upload-artifact@v4
        if: always()
        with: { name: androidTest-results, path: app/build/reports/androidTests }
```

`build` job 名以现有 workflow 为准（没有就去掉 `needs`）。

- [ ] Step 1 写测试与 workflow；Step 2 `./gradlew app:assembleDebugAndroidTest`（无设备时只编译）；若本机有模拟器/真机（`adb devices` 有 device），跑 `./gradlew app:connectedDebugAndroidTest` 并把结果写进报告；Step 3 提交 `test(demo): instrumentation 用例（换页 / 旋转 / 系统窗口 resize / Compose owner / modal）与 CI 模拟器任务`。

---

### Task 9: README / README_EN / MIGRATION / CLAUDE / copilot-instructions

**Files:**
- Rewrite: `README.md`、`README_EN.md`
- Create: `docs/MIGRATION.md`
- Rewrite: `CLAUDE.md`（仓库根）、`.github/copilot-instructions.md`

**README.md 结构（README_EN 同结构英文）：**

1. 标题 + 徽章（Maven Central 徽章改指向 `floatingx-core`）+ 一句话简介 + English 链接。
2. **3.0 有什么不同**：五模块可插拔；锚点定位（尺寸变化不跳）；状态机（任何时机 install/show 都生效，跨页/旋转/recreate 不丢）；可组合手势；Compose 状态跨页存活；Java 友好。附 2.x → 3.0 迁移链接（`docs/MIGRATION.md`）。
3. **模块与依赖**：表格（模块 / 用途 / 依赖下限：core→androidx.core 1.13.1（compileSdk≥34）、compose→compose-ui 1.11.4（compileSdk≥35）+ lifecycle 2.10.0 + minSdk 23）；Gradle 片段：

```groovy
dependencies {
    implementation "io.github.petterpx:floatingx-core:3.0.0"      // 必选
    implementation "io.github.petterpx:floatingx-app:3.0.0"       // App 级全局浮窗（跟随 Activity）
    implementation "io.github.petterpx:floatingx-system:3.0.0"    // 系统级浮窗（WindowManager + 权限）
    implementation "io.github.petterpx:floatingx-scope:3.0.0"     // 局部浮窗（Activity / ViewGroup / Fragment）
    implementation "io.github.petterpx:floatingx-compose:3.0.0"   // Jetpack Compose 内容
}
```

4. **快速开始**：Kotlin `FloatingX.install("music") { layout(...); anchor(FxGravity.CENTER_END, dy = 120f); adsorb(...); appHost(app) { blacklist(SplashActivity::class.java) } }.show()`；Java Builder 版（与 `JavaDemo.installApp` 一致）；系统浮窗（`systemHost(app) { fallback(appHost(app)) }` + 权限说明）；局部浮窗（`activity.fxScope {}` / `viewGroup.fxScope {}` / `fragment.fxScope {}`）；Compose（`compose { ctrl -> … }` + `stateFlow/positionFlow`）。每段代码必须在 demo 里能找到同样调用。
5. **能力一览**：表格（锚点/边界/越界、吸附/半隐/回弹、手势模式/区域/子 view 优先级/透传、动画、位置持久化（按方向）、黑白名单/父类/自定义过滤、modal、键盘、返回键、多浮窗 tag、日志）。
6. **API 速查**：`FxControl`（show/hide/cancel/moveTo/moveBy/update{}/updateContent/setContent/addListener/addFeature）、`FxListener` 回调、`FloatingX` 注册表。
7. **常见问题**：权限被拒的降级；后台申请权限（Q+）；Dialog 之上的浮窗（`viewGroupHost(dialog.window.decorView)`）；多进程不支持；`Activity.fxScope` 在 `setContentView` 之后调用；系统浮窗 R 以下无 safe area。
8. **issue 覆盖矩阵**：复制 spec §9 表（3.0 修复了哪些 issue）。
9. **demo**：页面清单（能力页 + 回归页）。
10. 感谢 / 关于我（沿用旧内容）。

**docs/MIGRATION.md：** 表格 `2.x` → `3.0`：`FloatingX.install { setContext(ctx); setLayout(id); setScopeType(SYSTEM_AUTO) }` → `FloatingX.install("tag") { layout(id); systemHost(app) { fallback(appHost(app)) } }`；`setGravity(FxGravity.RIGHT_OR_BOTTOM)` → `anchor(FxGravity.BOTTOM_END)`（9 个值对照）；`setOffsetXY` → `anchor(dx, dy)`；`setEnableEdgeAdsorption/setEdgeOffset/setEnableHalfHide` → `adsorb(FxAdsorb.Edges(...))` + `margin`；`setBorderMargin` → `margin`；`setEnableScrollOutsideScreen` → `overflow`；`setDisplayMode` → `gesture { drag/click }`；`setTouchListener` → `gesture { dragRegion/childPriority }` + `FxListener.onDrag`；`addInstallBlackClass/WhiteClass` → `appHost { blacklist/whitelist/filter }`；`setPermissionInterceptor` → `permission(FxPermissionStrategy.manual {})`；`setSaveDirectionImpl` → `persist(FxStorage)`；`setAnimationImpl` → `animation(FxAnimation)`；`enableComposeSupport()` + `setLayoutView(ComposeView)` → `compose {}`；`ScopeHelper.builder{}.toControl(x)` → `x.fxScope {}`；`IFxViewLifecycle` → `FxListener` / `FxFeature`；`configControl.xxx` → `control.update {}`；`updateView/updateViewContent` → `setContent/updateContent`；`FxScopeType.APP/SYSTEM/SYSTEM_AUTO` → host 选择。再列"3.0 移除且无对应"的项（`setEnableFixLocation`、`setTagActivityLifecycle`、多进程）。

**CLAUDE.md 重写要点：** 五模块表（包名/职责/依赖）；三层结构改为 Host / Engine / Feature 描述；构建命令（`./gradlew test`、各模块 test、`app:installDebug`、`app:connectedDebugAndroidTest`、`publishToMavenLocal`）；Robolectric sdk=35 说明；demo 页面清单与验证方式；"3.0 重构进行中"段落删除，改为"计划与裁决归档"链接（specs + plans 目录）。`.github/copilot-instructions.md` 同步（去掉 detekt/旧模块描述）。

- [ ] Step 1 写文档；Step 2 检查每个 README 代码片段在 demo 或库测试里有同样的 API 调用（`grep`）；Step 3 `./gradlew test app:assembleDebug`；提交 `docs: README / README_EN / MIGRATION 重写为 3.0；CLAUDE.md 与 copilot-instructions 同步`。

---

## 自查记录

- **Spec 覆盖：** §11 删除项（`floatingx/`、`floatingx_compose/`、`check/detekt/`、`gradle/dev/FxComposeSimple.kt`）→ T1；demo "每模块一个演示页 + 按 issue 编号命名的回归页" → T2–T6；Java 样例保持编译 → T7；§10 instrumentation 四行 + CI emulator-runner → T8；`docs/MIGRATION.md`、README 重写、copilot/CLAUDE 更新 → T9；Plan 3 真机清单 → T8 用例 + T9 FAQ。
- **占位扫描：** 页面按钮以"标签 → 调用"列表给出（每项都是具体 API 调用），页面骨架由 T1 的 DSL 提供；无 TBD。
- **类型一致性：** `DemoWindows.installApp/installSystem/installCompose/ensureApp/ensureSystem`（T1/T3/T6）；`DemoContent.card/resizable/list/toast`（T1/T5）；`demoPage/demoPageWithHeader`（T1）；`BaseBlackActivity`（T2，T6/T7 引用）；`JavaDemo.installApp/installSystem/createScope`（T7，T9 README 引用）；包名统一 `com.petterp.floatingx.demo`（T1 裁决）。

## 执行记录（SDD ledger 归档）

分支 feat/3.0-demo，9 个功能提交 + 5 个修复波提交（77edd4d..dc8415b）；最终 ./gradlew test：core 142 / scope 22 / app 32 / system 62 / compose 25 = 283，0 失败；app:assembleDebug + assembleDebugAndroidTest 通过；instrumentation 用例仅编译验证（无设备），首次真实执行在 CI instrumentation job。

### Pre-flight scan

| Pair / Task | Produces vs consumes | Finding |
|---|---|---|
| T1 ↔ T2–T7 | T1 提供 demoPage/demoPageWithHeader DSL、DemoWindows.installApp/installSystem/ensureApp/ensureSystem、DemoContent.card/toast、包名 com.petterp.floatingx.demo；各页按此调用 | 一致 |
| T1 ↔ T2 | T1 的 DemoWindows.installApp 引用 BlackActivity（T2 创建）→ T1 无法编译 | **冲突**：Ruling 见下 |
| T2 ↔ T7 | BaseBlackActivity（T2）被 JavaDemo（T7）与 T6 引用 | 一致 |
| T3 ↔ T1 | T3 给 installSystem 加 keyboard 参数并改内容；T1 版本先无该参数 | 一致（T3 修改 T1 文件） |
| T5 ↔ T1 | T5 给 DemoContent 加 resizable/list；MultiWindow 给 installApp 加 tag 参数 | 一致 |
| T6 ↔ T1 | installCompose 加进 DemoWindows；需要 lifecycle-viewmodel-compose（目录别名已在 Plan 4 加入：lifecycle-viewmodel-compose） | 一致 |
| T8 ↔ T2–T6 | 测试引用 AppHostActivity/SecondActivity/BlackActivity/ComposeSecondActivity/ModalActivity/DemoWindows.installCompose/CounterViewModel | 一致 |
| T9 ↔ 全部 | README 片段须与 demo/JavaDemo 一致 | 一致（T9 有 grep 检查步骤） |
| T1 自身 | 删除 floatingx/、floatingx_compose/ 后 settings.gradle 与 catalog 同步；旧 app 源码全部删除 | 一致 |
| T8 自身 | 系统窗口用例 assume 权限；CI 用 appops 在 installDebug 之后授权 | 一致 |

- Ruling: T1 ↔ T2 循环引用 — T1 里 DemoWindows.installApp 先用 `blacklist("com.petterp.floatingx.demo.pages.BlackActivity")`（按名，字符串）保持可编译，T2 创建 BaseBlackActivity 后改回 `blacklist(BaseBlackActivity::class.java)` — 若错，代价为零。
- Ruling: demo 包名统一为 `com.petterp.floatingx.demo`（避免与库的 `com.petterp.floatingx.app.AppHost` 同包歧义；applicationId/namespace 保持 `com.petterp.floatingx.app`）。
- Ruling: 就地分支 feat/3.0-demo；实现者 opus；评审按复杂度；完成后本地 ff 合回 main、不 push（沿用）。
- Ruling: 库模块源码本计划不改；发现的库 bug 记 ledger，最终评审决定是否开修复波。

### Tasks
- Task 1: Ruling: demo namespace 与 floatingx-app 同为 com.petterp.floatingx.app 会生成两个同名 R — demo namespace 改为 com.petterp.floatingx.demo（applicationId 保持 com.petterp.floatingx.app；manifest 内类名已是全限定），并入 Task 2 — 若错，代价是一次 namespace 改名。
- Task 1: implementer deviations（交评审）：删 values-night/themes.xml（MaterialComponents 父主题在暗色下会让 MaterialSwitch 崩溃）与死的 proguard-rules.pro；新建缺失的 app/proguard-floatingx.pro；systemHost 块加 theme()；uiautomator 用 version.ref。
- Task 1: complete (commits 77edd4d..377321f, review clean)
- Task 2: minor (deferred): values-v28 主题重复两条属性缺注释。
- Task 2: complete (commits 377321f..f635ffb, review clean)
- Task 3: 库缺陷（记录，最终修复波处理）：floatingx-system `FxPermissionRequest.deny()` KDoc 说停在 INSTALLED，但 SystemHost.denied() 有 fallback 时无条件 requestSwap，deny 与 useFallback 等价 — Ruling：deny() 改为只记日志停在 INSTALLED（用户可 retryPermission），useFallback() 有 fallback 则 swap、无则同 deny；Auto 策略被拒仍走 fallback；补测试。
- Task 3: implementer deviations（交评审）：manifest 用全限定 service 名；`host as? SystemHost`；Manual 拦截器用 WeakReference 持 Activity。
- Task 3: Important（入最终修复波）：SystemHostActivity Manual 拦截器 `ref.get()` 未检查 isFinishing/isDestroyed，retryPermission 时可能在已销毁 Activity 上弹 AlertDialog（BadTokenException）。
- Task 3: minor (deferred): 权限状态 note 用 custom(TextView) 实现（可刷新）；installWithAlpha 重装不带 permission/fallback（note 已说明）。
- Task 3: complete (commits f635ffb..453f4be, review approved; 1 Important 入修复波)
- Task 4: implementer deviations（交评审）：FxSpStorage(this@Activity)；控件 CANCELLED 后重建而非 lazy；fragmentSlot 放滚动列；固定 R.id.fragmentSlot；onDestroy cancel 局部浮窗。
- Task 4: minor (deferred): hostBox/fragmentSlot 作为 internal 顶层函数放在 pages 而非 ui。
- Task 4: complete (commits 453f4be..9a811b5, review clean)
- Task 5: implementer deviations（交评审）：modal toggle 保留 dismissOnOutsideTouch；Issue187 用独立 tag；resizable/list 显式设 root.layoutParams；各页加显示/隐藏按钮。
- Task 5: minor (deferred): 共享 demo-app 控件的 toggle 初值可能与真实状态不一致；Dialog 覆盖浮窗重复 tag "dialog"。
- Task 5: complete (commits 9a811b5..9026536, review clean)
- Task 6: implementer deviations（交评审）：installCompose(app, system=false) 单一 composable + ensureCompose；系统变体 dy=60；compose host 不设 theme。
- Task 6: minor (deferred): ComposeActivity 与 Issue210Activity 演示面有重叠；系统变体首次点击会弹权限页未提示。
- Task 6: complete (commits 9026536..a0f042b, review clean)
- Task 7: implementer deviations（交评审）：anchor(BOTTOM_START, 24f, 120f)（计划的 -120 会推出区域外）；onClick toast；SystemHost 加 theme()；scope 控件生命周期归 MainActivity；保留 Second/Black/Immersed 入口。
- Task 7: minor (deferred): 报告里 Modal/Compose 顺序的"计划顺序"说法与计划文件列表相反（无功能影响）；java-app/java-system 共用 config 初始位置重叠。
- Task 7: complete (commits a0f042b..4797aa7, review clean)
- Task 8: 库缺陷（最终修复波）：core ModalScrimFeature 在 hide() 后 container.modal 仍为 true，而 hitTest 对 INVISIBLE 内容返回 false → 隐藏的 modal 浮窗吞掉全屏触摸 — Ruling：FxLayerContainer.dispatchTouchEvent 仅在内容可见时拦截外部触摸（或 ModalScrimFeature 在 onHide/onShow 切换 modal），补 core 测试。
- Task 8: 说明：五个 instrumentation 用例仅编译验证（无设备），首次真实执行在 CI instrumentation job；SystemWindowResizeTest 无权限时 assume 跳过。
- Task 8: minor (deferred): 报告称 getter 有主线程 check（实际只有 mutator 有）；SystemWindowResizeTest 多余 idle()；ModalScrimTest 依赖按钮与卡片的布局距离。
- Task 8: complete (commits 4797aa7..3a51194, review clean; 用例仅编译验证)
- Task 9: implementer notes：README 用 fallback(AppHost.builder(app).build())（demo 无 DSL 嵌套形式）；MIGRATION 移除项按真实 2.x API；README 硬编码 3.0.0；去掉 2.x 日志截图。
- Task 9: complete (commits 3a51194..2ef5083, review clean; README/README_EN 19/19 章节 24/24 代码块对齐，API 全部核对)
- Final review (opus, 77edd4d..2ef5083): 2 Critical（= ledger 两个库缺陷）/ 5 Important / 7 minor；裁决全部被确认。
- Ruling: C1 在 FxLayerContainer.dispatchTouchEvent 修（内容不可见时不拦截外部触摸），非 ModalScrimFeature；补 core 测试。C2 SystemHost.deny() 停 INSTALLED、useFallback() 才 swap；补 system 测试；SystemHostActivity 的 note 同步。I1 在 showPermissionDialog 里守卫并 deny()。I2 build job 加 assembleDebug/assembleDebugAndroidTest。I3 emulator target=google_apis + setup-gradle。I4 TestUtil 注释改正。I5 ModalScrimTest 加隐藏后不吞触摸断言。测试数 279→281 同步 CLAUDE.md/copilot。
- Final review minor（本波顺手）：M2 README/demo 注释"cancel 后再调用抛 ISE"不实（实为 no-op；库 KDoc 不改）；M3 删未用别名 espresso-contrib；M4 .gitignore 死条目；M5 未用颜色/drawable；M6 recyclerview 显式依赖；M7 v28 主题注释；M1 demoPage 标题 TextView。
- Final fix wave: commits 2ef5083..dc8415b（C1/C2/I1–I5/M1–M8）；测试 283（core 142 / system 62）; scoped re-review 派出
- Final re-review: 16/16 ADDRESSED；nit：DemoPage 标题 Color.BLACK 暗色模式对比度；TestUtil 注释 addListener 无 check；recyclerview 内联版本；force-avd-creation 无 cache 时是空操作。
