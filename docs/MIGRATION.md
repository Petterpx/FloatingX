# 2.x → 3.0 迁移指南

3.0 是一次彻底重写，**不保留 2.x 的 API**。旧的 `io.github.petterpx:floatingx` /
`io.github.petterpx:floatingx-compose` 两个坐标停止发布，包名从 `com.petterp.floatingx.*`
拆成五个模块子包。本文按「2.x 写法 → 3.0 写法」逐条对照。

## 0. 依赖与包名

```groovy
// 2.x
implementation 'io.github.petterpx:floatingx:2.3.7'
implementation 'io.github.petterpx:floatingx-compose:2.3.7'

// 3.0：按需取，core 必选
implementation "io.github.petterpx:floatingx-core:3.0.0"
implementation "io.github.petterpx:floatingx-app:3.0.0"
implementation "io.github.petterpx:floatingx-system:3.0.0"
implementation "io.github.petterpx:floatingx-scope:3.0.0"
implementation "io.github.petterpx:floatingx-compose:3.0.0"
```

| 2.x 包 | 3.0 包 |
|---|---|
| `com.petterp.floatingx.FloatingX` | `com.petterp.floatingx.core.FloatingX` |
| `com.petterp.floatingx.assist.*` | `com.petterp.floatingx.core.config.*` / `core.layout.*` / `core.gesture.*` |
| `com.petterp.floatingx.assist.helper.FxAppHelper` | `com.petterp.floatingx.app.AppHost` + `com.petterp.floatingx.system.SystemHost` |
| `com.petterp.floatingx.assist.helper.FxScopeHelper` | `com.petterp.floatingx.scope.ViewGroupHost` / `FragmentHost` |
| `com.petterp.floatingx.listener.control.IFxControl` | `com.petterp.floatingx.core.FxControl` |

**AndroidManifest 不用再写了。** 2.x 需要接入方自己声明 `SYSTEM_ALERT_WINDOW`；
3.0 的 `floatingx-system` 已在自己的清单里声明权限与透明的权限申请页，
`floatingx-app` 用 `ContentProvider` 自行注册 Activity 跟踪器（`setContext` 也随之消失）。

## 1. 入口

```kotlin
// 2.x：一个 install 打天下，scopeType 决定实现
FloatingX.install {
    setContext(context)
    setLayout(R.layout.item_floating)
    setScopeType(FxScopeType.SYSTEM_AUTO)
}.show()

// 3.0：install 必须指定 host，host 就是「挂在哪」
FloatingX.install("tag") {
    layout(R.layout.item_floating)
    systemHost(app) { fallback(AppHost.builder(app).build()) }   // 等价于 SYSTEM_AUTO
}.show()
```

| 2.x | 3.0 |
|---|---|
| `FxScopeType.APP` | `appHost(app) { … }` |
| `FxScopeType.SYSTEM` | `systemHost(app) { … }`（不配 `fallback`） |
| `FxScopeType.SYSTEM_AUTO` | `systemHost(app) { fallback(AppHost.builder(app).build()) }` |
| `setContext(context)` | 删除。`appHost(app)` / `systemHost(app)` 直接收 `Application` |
| `setTag("x")` | `FloatingX.install("x") { … }`（tag 是 install 的第一个参数） |

## 2. 内容

| 2.x | 3.0 |
|---|---|
| `setLayout(R.layout.x)` | `layout(R.layout.x)` |
| `setLayoutView(view)` | `view(view)`，或 `view { ctx -> … }` 按 context 现场创建 |
| `updateView(resource)` / `updateView(view)` / `updateView(provider)` | `control.setContent(FxContent.layout(id))` / `FxContent.view(v)` / `FxContent.provider {}` |
| `updateViewContent { holder -> … }` | `control.updateContent { holder -> … }`（3.0 里 show 之前也可用） |

## 3. 位置

| 2.x | 3.0 |
|---|---|
| `setGravity(FxGravity.RIGHT_OR_BOTTOM)` | `anchor(FxGravity.BOTTOM_END)` |
| `setX(x)` / `setY(y)` / `setXY(x, y)` | `anchor(gravity, dx, dy)`（相对锚定边的偏移，不再是绝对坐标） |
| `setOffsetXY(x, y)` | `anchor(gravity, dx = x, dy = y)` |
| `setBorderMargin(t, l, b, r)` | `margin(left, top, right, bottom)` |
| `setTopBorderMargin(t)` 等四个 | `margin(top = t)`（Kotlin 具名参数） |
| `setEnableScrollOutsideScreen(true)` | `overflow(top = true, bottom = true, left = true, right = true)`，按边细分 |
| `setEnableSafeArea(false)` | `safeArea = false` |
| `setEnableEdgeAdsorption(true)` | `adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END)))` |
| `setEdgeAdsorbDirection(FxAdsorbDirection.LEFT_OR_RIGHT)` | `adsorb(FxAdsorb.horizontal())`；上下用 `vertical()`，四向用 `all()` |
| `setEdgeOffset(edge)` | `margin(...)`（吸附后的留白就是 margin，不再单独设一个 offset） |
| `setEnableHalfHide(true)` + `setHalfHidePercent(0.3f)` | `adsorb(FxAdsorb.Edges(edges, halfHide = FxHalfHide(0.3f)))`，左右还可给不同比例 `FxHalfHide(start, end)` |
| （越界回弹恒开） | `FxAdsorb.Edges(rebound = true/false)`，可关 |
| `setSaveDirectionImpl(IFxConfigStorage)` | `persist(FxSpStorage(context))`，或自行实现 `FxStorage` |
| `FloatingX.clearConfig()` | `FxStorage.clear(key)`；key 由框架生成为 `"$tag:$orientation"`（横竖屏分别记忆） |

### `FxGravity` 九个值对照

| 2.x | 3.0 |
|---|---|
| `FxGravity.DEFAULT` / `LEFT_OR_TOP` | `FxGravity.TOP_START` |
| `FxGravity.TOP_OR_CENTER` | `FxGravity.TOP_CENTER` |
| `FxGravity.RIGHT_OR_TOP` | `FxGravity.TOP_END` |
| `FxGravity.LEFT_OR_CENTER` | `FxGravity.CENTER_START` |
| `FxGravity.CENTER` | `FxGravity.CENTER` |
| `FxGravity.RIGHT_OR_CENTER` | `FxGravity.CENTER_END` |
| `FxGravity.LEFT_OR_BOTTOM` | `FxGravity.BOTTOM_START` |
| `FxGravity.BOTTOM_OR_CENTER` | `FxGravity.BOTTOM_CENTER` |
| `FxGravity.RIGHT_OR_BOTTOM` | `FxGravity.BOTTOM_END` |

`START` / `END` 是**逻辑方向**，RTL 布局下自动翻转（2.x 的 `LEFT/RIGHT` 是硬编码的物理方向）。

## 4. 手势

2.x 的 `FxDisplayMode` 一个枚举包含了「能否点、能否拖」；3.0 拆成互相独立的开关。

| 2.x | 3.0 |
|---|---|
| `setDisplayMode(FxDisplayMode.Normal)` | 默认，`FxGesture.Normal` |
| `setDisplayMode(FxDisplayMode.ClickOnly)` | `gesture { drag = FxDrag.DISABLED }`，或 `gesture(FxGesture.ClickOnly)` |
| `setDisplayMode(FxDisplayMode.DisplayOnly)` | `gesture(FxGesture.DisplayOnly)`（`touchable = false`，完全透传） |
| `setEnableTouch(false)` | `gesture { touchable = false }` |
| （2.x 无） | `gesture { drag = FxDrag.AFTER_LONG_PRESS }` 长按后才可拖，或 `FxGesture.LongPressToDrag` |
| （2.x 无） | `gesture { dragRegion = FxRegion.child(R.id.header) }` 只有指定子 view 上才能起拖 |
| （2.x 靠 `IFxTouchListener` 自己判） | `gesture { childPriority = FxChildPriority.AUTO / PARENT / CHILD }` 与可滚动子 view 的冲突策略 |
| `setTouchListener(IFxTouchListener)` | 拖动策略移到 `gesture {}`；只想收回调就用 `FxListener.onDragStart/onDrag/onDragEnd` |
| `setScrollListener(...)`（已废弃） | 同上 |
| `setOnClickListener(listener)` / `setOnLongClickListener(listener)` | `control.addListener(object : FxListener { override fun onClick(control, view) {} })` |

## 5. 生命周期与回调

| 2.x | 3.0 |
|---|---|
| `setViewLifecycle(IFxViewLifecycle)`（已废弃）/ `addViewLifecycle(...)` | `control.addListener(FxListener)`：`onAttach` / `onDetach` / `onShow` / `onHide` / `onCancel` |
| （2.x 无位置回调） | `FxListener.onPositionChanged(control, anchor)`，可判断吸附到哪条边 |
| 自定义容器行为只能改库 | `addFeature(FxFeature)`，容器行为插件（内置 Location / Gesture / Animation / ModalScrim） |

## 6. 全局浮窗（App 级）

| 2.x | 3.0 |
|---|---|
| `addInstallBlackClass(vararg Class)` | `appHost(app) { blacklist(X::class.java) }`，按 `isInstance` 匹配，**子类一起命中** |
| `addInstallBlackClass(vararg String)` | `appHost(app) { blacklist("com.x.YActivity") }`（类全名精确匹配） |
| `addInstallWhiteClass(...)` | `appHost(app) { whitelist(...) }` |
| `setEnableAllInstall(false)` + 白名单 | 只写 `whitelist(...)` 即可：配了白名单就只在白名单页显示 |
| （2.x 无自定义规则） | `appHost(app) { filter { activity -> !activity.isFinishing } }`，可多次调用，全部通过才显示 |
| （2.x 固定挂 DecorView） | `appHost(app) { attachTo(AppAttachTarget.DECOR / CONTENT) }` |
| （2.x 用 Application context，Material 组件会崩） | `appHost(app) { theme(R.style.Theme_App) }` |

## 7. 系统浮窗

| 2.x | 3.0 |
|---|---|
| `setPermissionInterceptor(IFxPermissionInterceptor)` | `systemHost(app) { permission(FxPermissionStrategy.manual { request -> … }) }`；`request.proceed()` / `useFallback()` / `deny()` |
| （2.x 无「跳过检查」） | `permission(FxPermissionStrategy.skip())` |
| `setManagerParams(FrameLayout.LayoutParams)` | `systemHost(app) { layoutParams { it.type = …; it.flags = … } }`，直接改 `WindowManager.LayoutParams` |
| `setEnableKeyBoardAdapt(true, ...)` | `systemHost(app) { keyboard(R.id.etInput) }`，按 EditText id 登记 |
| `setKeyBackListener(IKeyBackListener)` | `systemHost(app) { onBackPressed { true } }` |
| 权限被拒后无法重试 | `(control.host as? SystemHost)?.retryPermission()` |

## 8. 局部浮窗

```kotlin
// 2.x
ScopeHelper.builder {
    setLayout(R.layout.item_floating)
}.toControl(activity)      // 或 toControl(fragment) / toControl(viewGroup)

private val scopeFx by createFx {
    setLayout(R.layout.item_floating)
    build().toControl(this)
}

// 3.0
val fx = activity.fxScope("tag") { layout(R.layout.item_floating) }   // setContentView 之后调用
val fx = viewGroup.fxScope("tag") { layout(R.layout.item_floating) }
val fx = fragment.fxScope("tag") { layout(R.layout.item_floating) }   // onCreate 里就能写
fx.show()
```

| 2.x | 3.0 |
|---|---|
| `ScopeHelper.builder{}.toControl(activity)` | `activity.fxScope {}` |
| `ScopeHelper.builder{}.toControl(fragment)` | `fragment.fxScope {}` |
| `ScopeHelper.builder{}.toControl(viewGroup)` | `viewGroup.fxScope {}` |
| `createFx { … }` 委托 | 直接 `fxScope {}`（返回 `FxControl`，自己存字段即可） |
| Java 侧 `toControl(...)` | `FloatingX.create(config, ViewGroupHost.of(viewGroup), "tag")` |

局部浮窗仍然**不进注册表**：`FloatingX.controls()` 里看不到，生命周期归调用方。
新增的自动收尾：`Activity.fxScope` 在 API 29+ 页面销毁时自动 `cancel`；
`Fragment.fxScope` 在 fragment destroy 时自动 `cancel`。

## 9. Compose

```kotlin
// 2.x：要在 AppHelper 上开开关，再自己塞一个 ComposeView
FloatingX.install {
    setContext(context)
    enableComposeSupport()
    setLayoutView(ComposeView(context).apply { setContent { … } })
}

// 3.0：内容本身就是一段组合
FloatingX.install("compose") {
    compose { control ->
        val state by control.stateFlow().collectAsState()
        …
    }
    appHost(app)
}.show()
```

| 2.x | 3.0 |
|---|---|
| `enableComposeSupport()` + `setLayoutView(ComposeView)` | `compose { control -> … }` |
| owner 挂在宿主 Activity 上（换页就丢） | 每个浮窗自带 `FxComposeOwner`，`viewModel()` / `rememberSaveable` 跨页存活 |
| （2.x 无） | `control.stateFlow()` / `control.positionFlow()` |

## 10. 控制与配置更新

| 2.x | 3.0 |
|---|---|
| `control.updateConfig { … }`（`IFxConfigControl`） | `control.update { … }`（同一套 `FxConfigScope` DSL，未显式设置的项沿用旧值） |
| `control.isShow()` | `control.isShowing` |
| `control.getX()` / `getY()` | `control.position`（`FxPoint`，三种 host 语义一致：内容左上角的屏幕坐标） |
| `control.getView()` / `getViewHolder()` | `control.contentView` / `control.holder` |
| `control.getManagerView()` | 无对应：容器归 core 所有，不再暴露 |
| `control.move(x, y)` / `move(x, y, useAnimation)` | `control.moveTo(x, y)` / `moveTo(x, y, animate)` |
| `control.moveByVector(x, y)` | `control.moveBy(dx, dy)` / `moveBy(dx, dy, animate)` |
| `setEnableAnimation(true)` + `setAnimationImpl(FxAnimation)` | `animation(FxAnimations.fade())` / `animation(FxAnimations.scale())` / `animation(自定义 FxAnimation)`（传 `null` 即关闭） |
| `setEnableLog(true, "tag")` | `enableLog("tag")`（不调用就完全静默） |

`FloatingX` 注册表：`install(tag)` / `create(tag)` / `control(tag)` / `controlOrNull(tag)` /
`controls()` / `isInstalled(tag)` / `uninstall(tag)` / `uninstallAll()`。
`tag` 缺省为 `FloatingX.DEFAULT_TAG`。

## 11. 3.0 移除且无对应的能力

| 2.x | 说明 |
|---|---|
| 位置「强行修复」/ 辅助定位（`enableAssistLocation`） | 3.0 存的是锚点而不是绝对坐标，尺寸/可用区变化时按锚点重算，不需要机型修复开关 |
| `setTagActivityLifecycle(IFxProxyTagActivityLifecycle)` | 代理宿主 Activity 生命周期的口子取消；需要感知挂载变化请用 `FxListener.onAttach/onDetach` 与 `control.attachedActivity` |
| 多进程 | 非目标。`FloatingX` 注册表按进程隔离，子进程里是另一份空注册表（#129） |
| `FxViewHolder` 之外的容器 API（`getManagerView` 等） | 容器归 core 所有，不再作为公开 API |

## 12. 迁移速查清单

1. 换依赖坐标与 import；删掉 `AndroidManifest` 里为浮窗加的权限声明。
2. `install {}` 里补上 host（`appHost` / `systemHost` / `viewGroupHost`），删掉 `setContext` / `setScopeType` / `setTag`。
3. `setGravity` + `setOffsetXY` → 一次 `anchor(gravity, dx, dy)`；`setX/setY` 改成相对锚定边的偏移。
4. `setDisplayMode` / `setEnableTouch` / `setTouchListener` → `gesture {}`。
5. `setOnClickListener` / `addViewLifecycle` → `control.addListener(FxListener)`。
6. `ScopeHelper.builder{}.toControl(x)` → `x.fxScope {}`。
7. `enableComposeSupport()` + `ComposeView` → `compose {}`。
8. `updateConfig {}` → `update {}`；`updateView` → `setContent`；`updateViewContent` → `updateContent`。
