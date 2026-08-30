# FloatingX

![image-20210810161316095](https://tva1.sinaimg.cn/large/008i3skNly1gtbrg85hlhj61040k80ui02.jpg)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a9edd107b5444b7ca31738f5a96b3cb9)](https://app.codacy.com/gh/Petterpx/FloatingX?utm_source=github.com&utm_medium=referral&utm_content=Petterpx/FloatingX&utm_campaign=Badge_Grade_Settings)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petterpx/floatingx-core)](https://central.sonatype.com/artifact/io.github.petterpx/floatingx-core)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)

**FloatingX** 一个灵活且强大的悬浮窗解决方案。

[English Introduction](https://github.com/Petterpx/FloatingX/blob/main/README_EN.md)

[AI 快速查文档 见这里](https://deepwiki.com/Petterpx/FloatingX)

## ✨ 3.0 有什么不同

3.0 是一次彻底重写，不兼容 2.x 的 API。从 2.x 升级请看 [**2.x → 3.0 迁移指南**](docs/MIGRATION.md)。

- **五个模块，按需依赖**：core 是纯 View 实现（只要 `androidx.core` + `androidx.annotation`），
  app / system / scope / compose 各自独立。不用系统浮窗就不会引入 `WindowManager` 相关代码，
  不用 Compose 就不会引入 Compose 依赖。
- **锚点定位**：存的是「贴哪条边 + 偏移」而不是左上角坐标。内容尺寸变化（文字变长、展开收起）时
  贴着的那条边不动，不再往右下溢出（#187/#172/#178/#203/#206）。
- **状态机 + 命令队列**：`install` / `show` / `moveTo` 在任何时机调用都生效——
  宿主还没就绪就排队，就绪后按序回放。换页、旋转、Activity recreate、被黑名单卸下再回来，
  浮窗都不会丢（#195/#150/#205/#180）。
- **可组合手势**：拖动方式、起拖区域、与可滚动子 view 的冲突策略、触摸透传都是独立开关，
  不再是一个 `FxDisplayMode` 枚举包打天下（#218/#222/#165/#209/#243）。
- **Compose 状态跨页存活**：每个浮窗自带 `FxComposeOwner`（Lifecycle / ViewModelStore /
  SavedStateRegistry 都归浮窗自己），`viewModel()`、`rememberSaveable` 的状态不随宿主 Activity
  销毁而清空（#210/#239）。
- **Java 友好**：DSL 入口标 `@JvmSynthetic`，Java 侧走 `FxConfig.Builder` / `AppHost.Builder` /
  `SystemHost.Builder` / `ViewGroupHost.of`；`FxListener` 全部是默认方法。

## 📦 模块与依赖

| 模块 | 用途 | minSdk | 传导给使用方的下限 |
|---|---|---|---|
| `floatingx-core` | 状态机、锚点定位、手势、feature、注册表、`FxControl` | 21 | `androidx.core 1.13.1` → **compileSdk ≥ 34** |
| `floatingx-app` | `AppHost`：跟随前台 Activity 的全局浮窗 | 21 | 同 core |
| `floatingx-system` | `SystemHost`：`WindowManager` 窗口、悬浮窗权限、键盘适配 | 21 | 同 core |
| `floatingx-scope` | `ViewGroupHost` / `FragmentHost`：局部浮窗（`androidx.fragment` 为 `compileOnly`） | 21 | 同 core |
| `floatingx-compose` | `compose {}` 内容、`FxComposeOwner`、`stateFlow()` / `positionFlow()` | **23** | `compose-ui 1.11.4` → **compileSdk ≥ 35**、`lifecycle 2.10.0` |

所有模块的 Kotlin metadata 跟随 Kotlin 2.2，使用方 **Kotlin ≥ 2.1**。仓库自己的 AGP / Gradle / JDK 不传导。

### Gradle

```groovy
dependencies {
    implementation "io.github.petterpx:floatingx-core:3.0.0"      // 必选
    implementation "io.github.petterpx:floatingx-app:3.0.0"       // App 级全局浮窗（跟随 Activity）
    implementation "io.github.petterpx:floatingx-system:3.0.0"    // 系统级浮窗（WindowManager + 权限）
    implementation "io.github.petterpx:floatingx-scope:3.0.0"     // 局部浮窗（Activity / ViewGroup / Fragment）
    implementation "io.github.petterpx:floatingx-compose:3.0.0"   // Jetpack Compose 内容
}
```

`app` / `system` 模块自带清单：`floatingx-app` 用一个 `ContentProvider` 在进程启动时注册 Activity 跟踪器，
所以 `install` 写在任何时机都能拿到当前前台 Activity；`floatingx-system` 已声明
`SYSTEM_ALERT_WINDOW` 权限与透明的权限申请页，**接入方不需要自己配置任何东西**。

## 🚀 快速开始

### App 级全局浮窗

```kotlin
val control = FloatingX.install("music") {
    layout(R.layout.fx_card)
    anchor(FxGravity.CENTER_END, dy = 120f)
    margin(top = 24f, bottom = 24f)
    adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), halfHide = FxHalfHide(0.3f)))
    persist(FxSpStorage(app))
    enableLog("Fx-demo")
    appHost(app) {
        // 传 Class 而非类名字符串：按 isInstance 匹配，子类一起命中
        blacklist(SplashActivity::class.java)
    }
}
control.show()
```

Java（`app/src/main/java/com/petterp/floatingx/demo/java/JavaDemo.java` 里的同一段）：

```java
FxConfig config = FxConfig.builder(FxContent.layout(R.layout.fx_card))
        .anchor(FxGravity.BOTTOM_START, 24f, 120f)
        .margin(16f, 16f, 16f, 16f)
        .adsorb(new FxAdsorb.Edges(EnumSet.of(FxEdge.START, FxEdge.END), new FxHalfHide(0.3f), true))
        .gesture(FxGesture.LongPressToDrag)
        .storage(new FxSpStorage(app))
        .enableLog("Fx-java")
        .build();
AppHost host = AppHost.builder(app)
        .blacklist(SplashActivity.class)
        .filter(activity -> !activity.isFinishing())
        .build();
FxControl control = FloatingX.install("java-app", config, host);
control.show();
```

### 系统级浮窗

```kotlin
FloatingX.install("sys") {
    layout(R.layout.fx_input)
    anchor(FxGravity.TOP_START, dx = 24f, dy = 200f)
    adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END)))
    systemHost(app) {
        permission(FxPermissionStrategy.auto())          // 默认：自动弹页申请
        // 权限被拒时降级为 App 级浮窗（相当于 2.x 的 SYSTEM_AUTO）
        fallback(AppHost.builder(app).build())
        layoutParams { it.alpha = 0.9f }                 // 在默认 LayoutParams 之后执行，可覆盖任何字段
        keyboard(R.id.etInput)                           // 触到这些 EditText 时窗口临时可聚焦、弹键盘
        onBackPressed { true }                           // 键盘弹起期间才收得到返回键
    }
}.show()
```

权限三策略：

| 策略 | 行为 |
|---|---|
| `FxPermissionStrategy.auto()` | 默认。无权限时自动弹透明页申请，被拒后走 `fallback`（没配就停在 `INSTALLED`） |
| `FxPermissionStrategy.manual { request -> … }` | 交给业务方：`request.proceed()` 去申请 / `useFallback()` 直接降级 / `deny()` 放弃 |
| `FxPermissionStrategy.skip()` | 不检查权限直接挂窗口（已自行申请，或 type 不需要权限） |

被拒后拿到权限，调用 `(control.host as? SystemHost)?.retryPermission()` 即可恢复。
系统浮窗只要 application context，**Service 里也能装**（#192）。

Java 版：

```java
SystemHost host = SystemHost.builder(app)
        .layoutParams(lp -> lp.alpha = 0.9f)
        .permission(FxPermissionStrategy.auto())
        .fallback(AppHost.builder(app).build())
        .build();
FxControl control = FloatingX.install("java-system", config, host);
control.show();
```

### 局部浮窗

局部浮窗**不进注册表**，生命周期归调用方；不再需要时调用 `control.cancel()`。

```kotlin
// Activity：挂在 android.R.id.content 上（必须在 setContentView 之后调用）
val actFx = fxScope("scope-act") {
    layout(R.layout.fx_card)
    anchor(FxGravity.BOTTOM_END)
    persist(FxSpStorage(this@ScopeHostActivity))
}
actFx.show()

// 任意 ViewGroup：浮窗只在这个容器内活动
val boxFx = box.fxScope("scope-box") {
    layout(R.layout.fx_card)
    anchor(FxGravity.TOP_START)
}

// Fragment：在 onCreate 里就能写，view 就绪后自动挂上，fragment destroy 时自动 cancel
class ScopeFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fxScope("scope-frag") {
            layout(R.layout.fx_card)
            anchor(FxGravity.CENTER)
        }.show()
    }
}
```

Java 侧用 `ViewGroupHost.of(...)` + `FloatingX.create(...)`：

```java
ViewGroup content = activity.findViewById(android.R.id.content);
FxControl control = FloatingX.create(config, ViewGroupHost.of(content), "java-scope");
control.show();
```

### Compose

```kotlin
FloatingX.install("compose") {
    compose { control ->
        val vm: CounterViewModel = viewModel()                        // 归浮窗自己的 ViewModelStore
        var count by rememberSaveable { mutableIntStateOf(0) }        // 容器 detach 也不丢
        val state by control.stateFlow().collectAsState()             // FxState
        val pos by control.positionFlow().collectAsState()            // 内容左上角的屏幕坐标
        Surface(shape = CircleShape, modifier = Modifier.size(110.dp)) {
            Column(Modifier.clickable { count++; vm.clicks++ }) {
                Text("count $count")
                Text("${pos.x.toInt()},${pos.y.toInt()} $state")
            }
        }
    }
    anchor(FxGravity.CENTER_START, dy = -100f)
    appHost(app)
}.show()
```

整棵组合跑在浮窗自己的 `FxComposeOwner` 上：`attach → STARTED`、`show → RESUMED`、
`detach → CREATED`，只有 `cancel()` 才 `DESTROYED`。所以换页、旋转、被黑名单卸下再回来，
组合状态都不重来。`compose {}` 对 host 无感知，App / 系统浮窗写法完全一样。

## 🧰 能力一览

| 能力 | API | 说明 |
|---|---|---|
| 锚点定位 | `anchor(FxGravity, dx, dy)` | 9 个 gravity；`dx/dy` 是从所依附的边**向内**的偏移；RTL 下 `START/END` 自动翻转 |
| 边界留白 | `margin(left, top, right, bottom)` | 可用区四边的额外留白 |
| 安全区 | `safeArea = true/false` | 是否避开状态栏 / 导航栏 / 刘海 |
| 越界 | `overflow(top, bottom, left, right)` | 允许内容超出可用区的哪些边 |
| 边缘吸附 | `adsorb(FxAdsorb.Edges(edges, halfHide, rebound))` | 四向可选；`FxAdsorb.horizontal()/vertical()/all()/none()` |
| 半隐 | `FxHalfHide(start, end)` | 贴左 / 贴右可用不同比例 |
| 越界回弹 | `FxAdsorb.Edges(rebound = true)` | 拖动中允许暂时出界，松手回弹 |
| 拖动模式 | `gesture { drag = FxDrag.IMMEDIATE / AFTER_LONG_PRESS / DISABLED }` | 按下即拖 / 长按后拖 / 禁拖 |
| 起拖区域 | `gesture { dragRegion = FxRegion.child(R.id.header) }` | 也可 `FxRegion.rect(...)` 或自定义 |
| 子 view 优先级 | `gesture { childPriority = FxChildPriority.AUTO / PARENT / CHILD }` | 内容里有 RecyclerView 时，纵向手势归谁 |
| 触摸透传 | `gesture { touchable = false }` | 浮窗完全不吃触摸，点它等于点下层 |
| 点击 / 长按 | `gesture { click = …; longPress = …; longPressTimeout = … }` | 预设：`FxGesture.Normal / ClickOnly / DisplayOnly / LongPressToDrag` |
| 动画 | `animation(FxAnimations.fade())` / `scale()` / 自定义 `FxAnimation` | show / hide 动画 |
| 位置持久化 | `persist(FxSpStorage(context))` | key 含 tag 与屏幕方向，横竖屏分别记忆；也可实现 `FxStorage` |
| 黑白名单 | `appHost(app) { blacklist(X::class.java); whitelist(...); filter { … } }` | Class 版按 `isInstance` 匹配，**子类一起命中**；也支持类全名字符串 |
| 挂载点 | `appHost(app) { attachTo(AppAttachTarget.DECOR / CONTENT) }` | 默认 DecorView（拖动范围真正全屏） |
| Modal | `modal(enabled, dismissOnOutsideTouch)` | 浮窗显示中时拦截内容之外的触摸（隐藏后自动放行），可选点外部自动 hide（只对 app / scope 生效） |
| 键盘 | `systemHost(app) { keyboard(R.id.etInput) }` | 系统窗口默认不可聚焦，触到这些 EditText 才临时可聚焦 |
| 返回键 | `systemHost(app) { onBackPressed { true } }` | 仅键盘弹起期间窗口可聚焦，才收得到 |
| 多浮窗 | `FloatingX.install(tag) {}` / `controls()` / `uninstall(tag)` | 全局浮窗按 tag 隔离，同 tag 再 install 会先 cancel 旧的 |
| 日志 | `enableLog("Fx-demo")` | `adb logcat \| grep "Fx-"`；不调用则完全静默 |
| 自定义行为 | `addFeature(FxFeature)` | 容器行为插件，内置 Location / Gesture / Animation / ModalScrim |

## 📖 API 速查

### `FxControl`

```kotlin
control.show(); control.hide(); control.cancel()          // cancel() 幂等；cancel 之后 show/hide/moveTo/moveBy 抛 IllegalStateException
control.moveTo(100f, 200f); control.moveTo(100f, 200f, animate = true)
control.moveBy(-20f, 0f); control.moveBy(-20f, 0f, animate = true)
control.update { anchor(FxGravity.BOTTOM_END); gesture { drag = FxDrag.DISABLED } }   // 局部改配置
control.setContent(FxContent.layout(R.layout.fx_card))    // 整体换内容
control.updateContent { it.setText(R.id.tvTitle, "Hi") }  // 改内容里的 view（show 之前也可用）
control.addListener(listener); control.removeListener(listener)
control.addFeature(feature); control.removeFeature(feature)

control.tag; control.state; control.isShowing
control.position        // 内容左上角的屏幕坐标，三种 host 语义一致
control.anchor; control.config; control.host; control.contentView; control.holder
control.attachedActivity  // floatingx-app 扩展：当前挂在哪个 Activity 上
```

`FxState`：`INSTALLED`（已创建，容器未挂载）→ `ATTACHED`（已挂载但不可见）→ `SHOWN`（可见），
终态 `CANCELLED`。

### `FxListener`（全部有默认实现，只覆写用得上的）

```kotlin
onAttach / onDetach / onShow / onHide / onCancel
onClick(control, view) / onLongClick(control, view)
onDragStart(control) / onDrag(control, x, y) / onDragEnd(control, x, y)   // x/y 相对容器
onPositionChanged(control, anchor)                                        // 锚点提交后，可判断贴在哪条边
```

### `FloatingX` 注册表

```kotlin
FloatingX.install(tag) { … }        // 安装并注册；同 tag 已存在会先 cancel 旧的
FloatingX.create(tag) { … }         // 创建但不注册，生命周期归调用方（局部浮窗）
FloatingX.control(tag)              // 取不到抛异常
FloatingX.controlOrNull(tag)
FloatingX.controls()                // 当前所有全局浮窗的快照
FloatingX.isInstalled(tag)
FloatingX.uninstall(tag) / FloatingX.uninstallAll()
```

`tag` 缺省为 `FloatingX.DEFAULT_TAG`。局部浮窗的 `tag` 只用于日志与位置持久化的存储键（留空则不持久化）。

## 🏄‍♀️ 效果图

| 全屏,activity,fragment,单view                                | 小屏展示                                                     | 非正常比例缩放屏幕                                           |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![效果-展示1](https://github.com/Petterpx/FloatingX/blob/main/image/fx-api-simple.gif?raw=true) | ![演示-小屏](https://github.com/Petterpx/FloatingX/blob/main/image/fx-small-gif.gif?raw=true) | ![非正常比例缩放](https://github.com/Petterpx/FloatingX/blob/main/image/fx-view-deformed-simple.gif?raw=true) |

| 屏幕旋转                                                     | 功能演示                                                     |      |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ---- |
| ![演示-旋转](https://github.com/Petterpx/FloatingX/blob/main/image/fx-rotate-simple.gif?raw=true) | ![演示-局部功能](https://github.com/Petterpx/FloatingX/blob/main/image/fx-api-simple.gif?raw=true) |      |

## ❓ 常见问题

**Q：系统浮窗权限被拒了怎么办？**
配了 `systemHost(app) { fallback(AppHost.builder(app).build()) }` 就会自动降级成 App 级浮窗继续显示，
容器换掉但配置、监听器、当前位置都保留（这就是 2.x 的 `SYSTEM_AUTO`）。没配 `fallback` 则停在
`INSTALLED`，之后拿到权限调用 `SystemHost.retryPermission()` 恢复。

**Q：在后台（或 Service 里）申请权限，什么都没弹出来？**
Android 10（Q）起系统禁止后台启动 Activity，`auto()` 策略的权限申请页可能**悄无声息地起不来**。
需要在后台安装浮窗时，用 `manual {}` / `skip()` 把申请推迟，等应用回到前台再
`SystemHost.retryPermission()`。

**Q：系统浮窗在 Android 11 以下不避让状态栏 / 刘海？**
屏幕级 insets 的公开入口 `WindowManager.getCurrentWindowMetrics()` 是 API 30（R）才有的，
R 以下 `SystemHost` 拿不到 safe area，`safeArea` 对它不起作用（可用区就是整块屏幕）。
需要在低版本避让时，自己用 `margin(top = …)` 留出来。

**Q：`Activity.fxScope {}` 崩了 / 不显示？**
它挂在 `android.R.id.content` 上，必须在 `setContentView()` **之后**调用。

**Q：怎么把浮窗显示在 Dialog 之上？**
Dialog 有自己的 Window，层级在 Activity 之上，挂在 Activity 上的浮窗盖不住它。
把浮窗直接挂到 Dialog 的 decorView 上（`decorView` 必须在 `dialog.show()` 之后取）：

```kotlin
dialog.show()
val decor = dialog.window?.decorView as? ViewGroup ?: return
val dialogFx = FloatingX.create("dialog") {
    layout(R.layout.fx_card)
    anchor(FxGravity.TOP_END)
    viewGroupHost(decor)
}
dialogFx.show()
dialog.setOnDismissListener { if (dialogFx.state != FxState.CANCELLED) dialogFx.cancel() }
```

**Q：`compose {}` 里的 `rememberSaveable` 能跨进程恢复吗？**
不能。它只在**进程内**保存：组合被 dispose（容器 detach）时存进浮窗自己的过桥仓库，
重新组合时取回；进程被杀就没了（浮窗本身也不会自动重建）。要跨进程恢复请自己持久化。

**Q：支持多进程吗？**
不支持。`FloatingX` 注册表按进程隔离，子进程里看到的是另一份空注册表（#129）。

**Q：为什么 App 级浮窗默认挂在 `DecorView` 而不是 `R.id.content`？**
挂 `DecorView` 时拖动范围才是真正的「全屏」，不受状态栏 / 导航栏 / `AppBar` 影响。
需要限制在应用视图范围内时用 `appHost(app) { attachTo(AppAttachTarget.CONTENT) }`。

## ✅ issue 覆盖矩阵

| issue | 3.0 的处理 |
|---|---|
| 187 / 172 / 178 / 203 内容尺寸变化跳动 | 锚点定位 + `LayoutParams.gravity` 映射；0 尺寸不定位 |
| 206 收起后不按 gravity 靠边 | 锚点不随尺寸变化而变 |
| 240 App 级拖动被裁剪 | Layer 容器 `clipChildren=false`，移动只改 `translation` |
| 195 onCreate 中调用无效 / 192 Service | 命令队列；系统浮窗可从任意 context 申请权限 |
| 150 / 205 / 180 跨页、recreate、旋转后消失 | `desiredVisible` + `onHostLost` / `onHostReady` |
| 201 切页子 view 自毁 / 189 already has parent | 内容 view 归 engine 所有；safeAdd |
| 218 长按时机 / 222 长按后拖 / 243 & 108 透传 / 165 区域拖动 / 209 & 124 & 137 滚动冲突 / 207 & 37 禁拖 | 可组合的 `FxGesture` |
| 194 / 220 / 241 / 235 / 155 / 211 LayoutParams | `systemHost { layoutParams {} }` + `FxOverflow` |
| 204 半隐不对称 / 117 & 157 四向吸附 / 148 贴边方向 | `FxAdsorb` / `FxHalfHide(start, end)` / `onPositionChanged(anchor)` |
| 242 / 90 RTL | 布局方向参与锚点解析 |
| 92 横竖屏位置 / 184 storage 不触发 | `FxStorage` 的 key 含 orientation，写入点唯一 |
| 210 / 239 Compose 消失、owner 崩溃 | `FxComposeOwner` 归 control 所有 |
| 212 / 151 屏蔽外部触摸、点外部消失 / 154 & 198 Dialog 之上 | `modal()`；Dialog 之上用 `viewGroupHost(dialog.window.decorView)` |
| 221 黑名单父类 | `blacklist(Class)` 含子类 + `filter(AppActivityFilter)` |
| 244 Fragment 不显示 | `FragmentHost` 等 view 创建后再挂载 |
| 183 降级后偏移不一致 / 188 拖不到底 | safe area 统一；host swap 保留配置 |
| 133 遍历所有浮窗 / 200 系统浮窗坐标 | `FloatingX.controls()` / `FxControl.position` |
| 140 / 38 泄漏 | 监听器归 control，Activity 跟踪器清引用 |
| 167 / 238 依赖泄漏 | 模块依赖边界 + CI 断言 |
| 129 多进程 | 非目标，见上文常见问题 |

## 📱 Demo

`app` 模块是完整的示例与回归工程，`./gradlew app:installDebug` 即可安装。

**能力页**

| 页面 | 内容 |
|---|---|
| `AppHostActivity` | App 级全局浮窗：锚点 / margin / 越界 / safeArea / 吸附 / 内容 / 动画 / `attachedActivity` |
| `SystemHostActivity` | 系统浮窗：三种权限策略、`layoutParams` 定制、`retryPermission`、键盘 / 返回键、前台 Service 安装 |
| `ScopeHostActivity` | 局部浮窗三种宿主：ViewGroup / Activity / Fragment |
| `GestureActivity` | 拖动模式 / 起拖区域 / 子 view 优先级 / 透传 / 回调日志 |
| `LayoutActivity` | 锚点 / 越界 / 吸附 / 内容尺寸 / 位置持久化 |
| `MultiWindowActivity` | 按 tag 管理多浮窗、`controls()`、同 tag 重装 |
| `ModalActivity` | modal 拦截外部触摸、点外部自动 hide、Dialog 之上的浮窗 |
| `ComposeActivity` / `ComposeSecondActivity` | Compose 浮窗：`viewModel()` / `rememberSaveable` / `stateFlow` / `positionFlow` / 跨页 |
| `SecondActivity` | 换页观察：容器被静默挪到新页，状态与位置都不重来 |
| `BlackActivity` | 黑名单页（浮窗消失） |
| `ImmersedActivity` | 沉浸页（edge-to-edge + 无状态栏），验证 safeArea |

**回归页**（按 issue 编号命名）

| 页面 | issue |
|---|---|
| `Issue187Activity` | #187 内容尺寸变化时锚点不动 |
| `Issue210Activity` | #210 Compose 浮窗跨页存活 |
| `Issue221Activity` | #221 黑名单命中子类 |
| `Issue240Activity` | #240 越界不被裁剪 |
| `Issue244Activity` | #244 Fragment 内浮窗 |

主页「Java」分组里的三个按钮对应 `JavaDemo.java`，用 Java Builder 把三种浮窗各写一遍，
验证公开 API 的 Java 友好度。

## 👍 感谢

基础 **悬浮窗View** 的 初版实现思想 源自 [EnFloatingView](https://github.com/leotyndale/EnFloatingView) 的 [FloatingMagnetView](https://github.com/leotyndale/EnFloatingView/blob/master/floatingview/src/main/java/com/imuxuan/floatingview/FloatingMagnetView.java) 实现方式，并在其之上进行了彻底的重构与演变。

对于导航栏的测量部分代码来自，wenlu,并在其之上增加了更多适配，已覆盖市场95%机型，可以说是目前能搜到的唯一可以准确测量的工具。

## 关于我

欢迎关注我的公众号，期待一同进步，如果有使用上的问题，也可以加我微信。

**微信**：**Petterpx**

![Petterp-wechat](https://user-images.githubusercontent.com/41142188/226162520-93796619-81ca-4e61-bfff-4a5b95e4fa0b.png)
