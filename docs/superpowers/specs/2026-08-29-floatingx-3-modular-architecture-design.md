# FloatingX 3.0 模块化架构设计

- 日期：2026-08-29
- 状态：已评审通过，待实施计划
- 范围：整个框架重写（不保留 2.x API 兼容），发布为 5 个可独立插拔的 artifact

## 0. 背景与目标

FloatingX 2.x 的 20 条 open issue 中有 12 条落在 5 个结构性缺陷上，逐个打补丁无法收敛：

| 编号 | 缺陷 | 现有代码位置 | 典型 issue |
|---|---|---|---|
| B1 | App 级浮窗跟随前台 Activity 反复 remove/addView 到 DecorView，attach 逻辑三个 Provider 各写一份 | `FxAppPlatformProvider.reAttach` | 86/179/201/189/154/212/205 |
| B2 | 显示状态与 `onActivityResumed` 硬耦合，未 attach 时的命令被丢弃 | `FxAppLifecycleImp`、`checkRegisterAppLifecycle` | 195/192/150 |
| B3 | 定位模型是"左上角绝对坐标 + onSizeChanged 兜底"，位置真值散落 5 处 | `FxViewLocationHelper` | 187/172/178/203/206/160/242/204 |
| B4 | 手势仲裁硬编码在容器 View 里，`FxDisplayMode` 枚举是唯一扩展点 | `FxViewTouchHelper` | 218/222/243/165/209 |
| B5 | `WindowManager.LayoutParams` 封装死，每个需求长出一个 `enableXxx` | `FxSystemContainerView.initWLParams` | 194/220/241/235/155/211 |

另有：core ↔ app/system 双向环形依赖（`FloatingX.kt` 直接 import 实现类，`FxSystemControlImp` 降级时反调 `FloatingX.install` 并原地篡改 helper）、`FxBasisHelper` 混杂平台专属字段、零自动化测试、依赖泄漏（#167）。

### 目标

1. **模块可插拔**：全局(app)、系统(system)、局部(scope)、Compose 各自独立 artifact，core 对它们零感知；不引入 system 就不带任何 WindowManager / 权限代码。
2. **性能**：触摸路径零分配、移动不触发父容器 re-layout、位置计算单一来源单次布局、不用 `post{}` 兜底。
3. **API 完备**：覆盖 issues 中明确提出的全部能力（见 §9），Kotlin DSL 与 Java Builder 同等一等。
4. **可验证**：JVM 单测 + Robolectric + instrumentation 三层覆盖，CI 守住依赖边界。

### 非目标

- 不保留 2.x API（只提供 `MIGRATION.md` 对照表）。
- 不做跨进程注册表（#129），文档化为"按进程隔离"。
- 不做锁屏显示、录屏隐藏、毛玻璃（#125/#142/#208），属业务层。

## 1. 模块与依赖

```
floatingx-core     com.petterp.floatingx.core     ← androidx.core 1.13.1, kotlin-stdlib
floatingx-app      com.petterp.floatingx.app      ← core
floatingx-system   com.petterp.floatingx.system   ← core
floatingx-scope    com.petterp.floatingx.scope    ← core, compileOnly androidx.fragment 1.8.9
floatingx-compose  com.petterp.floatingx.compose  ← core, compose-ui 1.11.4, lifecycle-runtime/viewmodel 2.11.0, savedstate 1.5.0, kotlinx-coroutines-android 1.11.0
app (demo)                                        ← 以上全部 + compose-bom 2026.06.01
```

### 1.1 版本矩阵（2026-08-29 定，"主流兼容线"）

库模块的依赖是传导给使用方的**下限**，不是推荐值；仓库自己的 AGP/Gradle 不传导。真正传导的三样：Kotlin metadata 版本、androidx 的 `minCompileSdk`、Compose 版本。

| 项 | 值 | 对使用方的要求 |
|---|---|---|
| 仓库工具链 | AGP 8.13.2、Gradle 8.14.3、Kotlin 2.2.21、JDK 17 | 无（不传导） |
| 所有模块 `compileSdk` / `targetSdk` | 36 / 36 | 无 |
| `minSdk` | core/app/system/scope = 21；compose = 23（compose 1.11 与 lifecycle 2.11 的 AAR 本身要求 23）；demo = 23 | Compose 使用方 minSdk ≥ 23（本就如此） |
| 库模块 Kotlin 不做 language/api 降级锁定 | 产物 metadata 跟随 Kotlin 2.2 | Kotlin ≥ 2.1 |
| core → androidx.core 1.13.1 | aar-metadata `minCompileSdk=34` | compileSdk ≥ 34（与 2.x 持平） |
| compose → compose-ui 1.11.4 | `minCompileSdk=35`, AGP ≥ 8.6 | compileSdk ≥ 35 |
| compose → lifecycle 2.11.0 / savedstate 1.5.0 | `minCompileSdk=34` | compileSdk ≥ 34 |
| 测试 | JUnit 4.13.2、Robolectric 4.16.1（sdk=35——SDK 36 沙箱需要 JDK 21，仓库工具链为 JDK 17）、androidx.test 1.7.0、espresso 3.7.0 | 无 |

明确不用：compose 1.12（强制 compileSdk 37 + AGP 9.1，Compose 用次新的 1.11 线即可）、androidx.core ≥ 1.17（强制 compileSdk 36）。Kotlin 直接用 2.2.21，不做 languageVersion 降级兼容（用户明确要求，避免不一致）。

- Maven 坐标 `io.github.petterpx:floatingx-{core,app,system,scope,compose}`。旧的 `floatingx` / `floatingx-compose` 两个 artifact 停止发布，旧模块目录删除。
- 根包 `com.petterp.floatingx` 保留；每个模块独占一个子包，禁止跨模块使用 `internal` 之外的非公开 API。
- 新增 `build-logic/` convention plugin（`floatingx.library`），统一：minSdk 21、compileSdk 36、Java 17、`explicitApi()`、maven-publish 坐标与签名（`-PisPublish=true` 时）、Robolectric 测试配置。五个模块的 `build.gradle.kts` 只声明依赖。
- **依赖边界检查**（CI 必跑）：core 源码不得 import `android.view.WindowManager`、`androidx.fragment`、`androidx.compose`、`androidx.lifecycle`、`androidx.appcompat`。用一个扫描 `src/main` 文件 import 的 JUnit 测试断言（零额外依赖，不引入 Konsist）。
- `settings.gradle.kts` 的 `isDev` 机制保留：demo 在 `isDev=true` 时依赖本地 project，否则依赖已发布产物。
- 版本：`3.0.0`。

## 2. core 模块

### 2.1 `FxHost` —— 谁来承载

```kotlin
public interface FxHost {
    /** engine 绑定时调用；host 通过 session 向 engine 回报事件 */
    public fun bind(session: FxHostSession)
    /** 创建容器。Layer 或 Window 由 host 决定 */
    public fun createContainer(): FxContainer
    public fun attach(container: FxContainer)
    public fun detach(container: FxContainer)
    /** 应用一次布局。Window 容器写 LayoutParams，Layer 容器写子 view 坐标 */
    public fun updateLayout(container: FxContainer, spec: FxLayoutSpec)
    /** 当前可用区域与 insets。用 WindowInsetsCompat，不再用厂商反射 */
    public fun bounds(): FxBounds
    public fun release()
    public fun hostFeatures(): List<FxFeature> = emptyList()   // host 自带 feature（键盘、窗口 flag 映射），换 host 时替换
}

/** 一次布局提交：内容左上角坐标 + 当前锚点 + 布局方向。gravity 只是 anchor.gravity 的快捷读法 */
public data class FxLayoutSpec(val x: Float, val y: Float, val anchor: FxAnchor, val ltr: Boolean)

public interface FxHostSession {
    public fun onHostReady()                 // 已有可挂载的父容器且尺寸有效
    public fun onHostLost()                  // 父容器消失（Activity destroy / ViewGroup detach）
    public fun onBoundsChanged()             // 旋转、insets、分屏
    public fun requestSwap(fallback: FxHost) // 例如 system 权限被拒
}
```

`FxContainer` 是 core 内部的容器抽象，有两种实现：

| 实现 | 用于 | 形态 | 移动方式 |
|---|---|---|---|
| `FxLayerContainer` | app / scope | `match_parent` 透明 `FrameLayout` 覆盖层，内容子 view 在其内定位 | 只改子 view 的 `translationX/Y`，父层不 re-layout（修 #240 裁剪）；scrim、透传都在 layer 上实现 |
| `FxWindowContainer` | system | `wrap_content` 的 WindowManager 窗口 | 写回 `WindowManager.LayoutParams` 并 `updateViewLayout` |

两者共同实现：`contentView`、`onSizeChanged` 回调、`hitTest(x, y)`（判断触点是否落在内容上）、
`isLayer`（只对 Layer 生效的 feature 用它判断，不再各自 `as?` 强转）、`releaseContent()`（摘掉内容 view
与其 layout 监听；换 host 与 `cancel()` 时调用，避免旧容器被内容 view 的监听拖住）。

### 2.2 `FxEngine` —— 状态机 + 命令队列

```
              attach                 show
 Installed ─────────▶ Attached ─────────▶ Shown
     ▲                   │  ▲                │
     │   hostLost        │  │     hide       │
     └───────────────────┘  └────────────────┘
 任意状态 ── cancel ──▶ Cancelled(终态)
```

规则：
- `show / hide / moveTo / moveBy / updateContent / setContent / update` 在状态不满足时**入队**（FIFO），`onHostReady` 后按序回放。队列在 `cancel()` 时清空。
- engine 记录 `desiredVisible: Boolean`。`onHostLost` → 退到 `Installed`，但 `desiredVisible` 不变；下一次 `onHostReady` 自动 attach 并按 `desiredVisible` 恢复（修 #205/#180/#150）。
- `requestSwap(fallback)`：`detach` 当前 host、`release` 它、`bind` fallback、用同一份 `FxConfig` 重新走 attach；`anchor`、监听器、内容 view 全部保留。
- 内容 view 在首次 attach 时用容器的 context 创建一次并由 control 持有；换 host 只 re-parent `FxContainer`，内容 view 不会因切页而销毁重建（修 #201）。
- 所有 engine 方法必须在主线程调用；非主线程调用抛 `IllegalStateException`（不做静默 post，避免时序歧义）。

### 2.3 锚点定位（替换 `FxViewLocationHelper`）

```kotlin
// 几何类型全部是 core 自己的纯 Kotlin data class（FxPoint/FxSize/FxRect/FxInsets），
// 不用 android.graphics.*，这样定位逻辑可以在纯 JVM 里测试
public data class FxAnchor(val gravity: FxGravity, val dx: Float = 0f, val dy: Float = 0f)
public data class FxBounds(val rect: FxRect, val insets: FxInsets)      // rect 为父容器区域，insets 为 safe area
public data class FxLayoutInput(
    val bounds: FxBounds, val size: FxSize, val ltr: Boolean,
    val margin: FxMargin, val overflow: FxOverflow, val safeArea: Boolean,
) { val area: FxRect /* bounds 扣掉 insets(若 safeArea) 与 margin 后的可用区 */ }
public object FxLayoutResolver {
    /** 锚点 → 左上角坐标。纯函数 */
    public fun resolve(anchor: FxAnchor, input: FxLayoutInput): FxPoint
    /** 左上角坐标 → 最近边（START/END × TOP/BOTTOM）的锚点。拖动结束后调用 */
    public fun toAnchor(point: FxPoint, input: FxLayoutInput): FxAnchor
    /** 钳制到允许范围（考虑 overflow） */
    public fun clamp(point: FxPoint, input: FxLayoutInput): FxPoint
}
```
`dx/dy` 的语义：相对于 gravity 所指的边向内的偏移（START → `x = area.left + dx`；END → `x = area.right - w - dx`；CENTER → `x = centerX - w/2 + dx`）。

- **位置真值只有 `engine.anchor`**。view 坐标、`LayoutParams.x/y`、持久化存储都是它的投影。
- `FxGravity`：`TOP_START, TOP_CENTER, TOP_END, CENTER_START, CENTER, CENTER_END, BOTTOM_START, BOTTOM_CENTER, BOTTOM_END`。START/END 依 `ltr` 解析（修 #242/#90）。
- 尺寸变化（`onSizeChanged`）→ 用当前 `anchor` 重新 `resolve`，无动画。锚点不动，所以靠右的浮窗收起后仍靠右（修 #206），内容更新不再"先闪回旧位置"（修 #187/#172/#178）。
- 父尺寸无效（0×0）时**不计算、不写坐标**，等下一次有效 `onLayout`；这是 2.x 跳变的直接来源。
- **system 窗口把 `anchor.gravity` 映射到 `LayoutParams.gravity`**（如 `Gravity.END or Gravity.BOTTOM`），`LayoutParams.x/y` 存的是相对该角的偏移。WindowManager 从同一个角定位，宽高变化时系统自己保持那个角不动，宽高与坐标在同一次 `updateViewLayout` 里提交——这是 #203（API 33/34 resize 跳动）的根治方案。
- `FxOverflow(top, bottom, left, right: Boolean)` 允许出界（#235）；`FxMargin` 四边；`safeArea=true` 时 bounds 扣掉 insets（app/system/scope 语义一致，修 #188/#183）。
- **吸附策略**：
  ```kotlin
  public sealed interface FxAdsorb {
      object None
      data class Edges(val directions: Set<FxEdge>, val halfHide: FxHalfHide? = null, val rebound: Boolean = true)
  }
  public data class FxHalfHide(val start: Float, val end: Float = start)   // 各边独立比例（#204）
  ```
  拖动结束 → `clamp` → 若有吸附则算目标边 → 动画移动 → `toAnchor` → 写入 `engine.anchor` → 持久化。四向吸附通过 `directions` 支持（#117/#157）。
- **持久化** `FxStorage { save(key, anchor); load(key): FxAnchor?; clear(key) }`，`key = "$tag:$orientation"`，横竖屏分别记忆（#92）。写入点唯一：拖动/吸附结束、`moveTo` 完成、`update{anchor}`。core 提供 `FxSpStorage` 默认实现。

### 2.4 手势识别（替换 `FxViewTouchHelper` + `FxDisplayMode`）

```kotlin
public data class FxGesture(
    val click: Boolean = true,
    val longPress: Boolean = true,
    val drag: FxDrag = FxDrag.Immediate,             // Immediate | AfterLongPress | Disabled
    val dragRegion: FxRegion? = null,                 // 只允许在内容的子区域起拖（#165）
    val childPriority: FxChildPriority = Auto,        // Auto | Parent | Child：与内部可滚动 view 的冲突策略（#209/#124/#137）
    val touchable: Boolean = true,                    // false = 完全透传（#243/#108）
    val longPressTimeout: Long = ViewConfiguration.getLongPressTimeout(),
) {
    public companion object {
        public val Normal: FxGesture; public val ClickOnly: FxGesture
        public val DisplayOnly: FxGesture; public val LongPressToDrag: FxGesture   // #222
    }
}
```

`FxGestureDetector`（core 内部，可用合成 `MotionEvent` 单测）：
- 全部使用 `actionMasked`；跟踪主指针 id；主指针抬起时若有其它指针则转移主指针，副指针抬起不结束拖拽。
- DOWN：记录起点，若 `longPress` 则 `Handler.postDelayed(longPressTimeout)`；移动超过 `scaledTouchSlop` 取消长按定时器。长按在按下期间触发（修 #218），与是否设置了点击监听无关。
- MOVE 超过 slop：`drag == Immediate` → 进入拖动；`AfterLongPress` 且长按已触发 → 进入拖动；否则不拦截（交给子 view）。
- UP：未拖动、未超 slop、且长按尚未触发 → click（不设额外时间阈值，避免 2.x 的 150–500ms 死区）；已拖动 → dragEnd → 吸附流程。
- `onInterceptTouchEvent` 只在"确定进入拖动"那一刻返回 true（`childPriority` 决定 slop 前是否让子 view 先消费）。
- 拖动中每个 MOVE 直接 `host.updateLayout`，不经动画、不经 `post`；`PointF/RectF` 复用，触摸路径零分配。
- `dragRegion` 支持 `FxRegion.child(viewId)` / `FxRegion.rect(RectF)` / `FxRegion.custom((x, y) -> Boolean)`。
- slop 与拖动增量按屏幕坐标（rawX 偏移）计算，落点判断按容器相对坐标——Window 容器随手指移动时相对坐标不可用。

### 2.5 `FxFeature` —— 容器行为插件

```kotlin
public interface FxFeature {
    public fun onAttach(scope: FxFeatureScope)
    public fun onDetach()
    /** cancel 时来一次，在最后一次 onDetach 之后、host.release() 之前；跨 attach 周期的资源在这里放 */
    public fun onCancel() {}
    public fun onConfigChanged(old: FxConfig, new: FxConfig) {}
    public fun onContentSizeChanged(size: FxSize) {}
    public fun onBoundsChanged() {}
    public fun onShow() {}
    public fun onHide() {}
}
public interface FxFeatureScope {           // feature 只能看到这些
    public val control: FxControl
    public val config: FxConfig
    public val container: FxContainer       // 含 isLayer，只对 Layer 生效的 feature 用它判断
    public val host: FxHost
    public val logger: FxLogger?
    public fun layoutInput(): FxLayoutInput?          // 内容尺寸/父容器尺寸无效时为 null
    public fun commitAnchor(anchor: FxAnchor)         // 更新真值 + 持久化 + onPositionChanged
    public fun dispatch(block: (FxListener) -> Unit)
    public fun requestRelayout()
}
```

- core 默认三件：`LocationFeature`、`GestureFeature`、`AnimationFeature`，按此顺序分发。
- 可选：`ModalScrimFeature`（core，仅 Layer 容器；拦截外部触摸 / 点击外部 hide，#212/#151）。
- 模块可注册：`KeyboardFeature`（system）。
- 用户：`addFeature(feature)`。
- feature 之间不可互相引用；需要共享的数据走 `FxFeatureScope`。
- 不做 `onTouchEvent` 责任链：触摸只有 `GestureFeature` 一家消费，外部 feature 需要拦截触摸时改容器状态
  （如 `ModalScrimFeature` 设 `FxLayerContainer.modal`），避免第二套事件分发。

### 2.6 配置与内容

```kotlin
public class FxConfig internal constructor(          // immutable，Kotlin 用 DSL、Java 用 Builder
    val content: FxContent,
    val anchor: FxAnchor,
    val margin: FxMargin,
    val overflow: FxOverflow,
    val safeArea: Boolean,
    val adsorb: FxAdsorb,
    val gesture: FxGesture,
    val animation: FxAnimation?,
    val storage: FxStorage?,
    val features: List<FxFeature>,
    val log: FxLogger?,
)
public sealed interface FxContent {
    data class Layout(@LayoutRes val id: Int)
    data class View(val view: android.view.View)
    class Provider(val create: (Context) -> android.view.View)
    // compose 模块追加 FxContent.Compose
}
```

- 删除 2.x 中的 `enableFx / statsBarHeight / navigationBarHeight / reInstall / assistLocation / enableAssistLocation / layoutParams(FrameLayout)` 等字段；`IFxSystemControl` 死接口删除。
- `control.update { }` 生成新 `FxConfig`（copy），engine 广播 `onConfigChanged(old, new)`；`anchor` 变化触发重定位，`gesture` 变化即时生效。

### 2.7 `FxControl` 与注册表

```kotlin
public interface FxControl {
    public val tag: String
    public val state: FxState                 // Installed / Attached / Shown / Cancelled
    public val isShowing: Boolean
    public val position: PointF               // 屏幕坐标，三种 host 一致（#200）
    public val anchor: FxAnchor
    public val config: FxConfig
    public val contentView: View?
    public val holder: FxViewHolder?
    public fun show(); public fun hide(); public fun cancel()
    public fun moveTo(x: Float, y: Float, animate: Boolean = true)
    public fun moveBy(dx: Float, dy: Float, animate: Boolean = true)
    public fun update(block: FxConfigScope.() -> Unit)
    public fun updateContent(block: (FxViewHolder) -> Unit)
    public fun setContent(content: FxContent)
    public fun addListener(l: FxListener); public fun removeListener(l: FxListener)
    public fun addFeature(f: FxFeature); public fun removeFeature(f: FxFeature)
}
public interface FxListener {   // 全部 default 空实现
    fun onShow(c); fun onHide(c); fun onAttach(c, container: View); fun onDetach(c)
    fun onClick(c, v); fun onLongClick(c, v)
    fun onDragStart(c); fun onDrag(c, x, y); fun onDragEnd(c, x, y)   // onDrag 每个 MOVE 都回调（#199）
    fun onPositionChanged(c, anchor: FxAnchor); fun onCancel(c)
}
```

- `FloatingX`（core object）：`install(tag, config, host): FxControl`、`install(tag) { dsl }`（`@JvmSynthetic`）、`control(tag)`、`controlOrNull(tag)`、`controls(): List<FxControl>` 快照（#133）、`isInstalled(tag)`、`uninstall(tag)`、`uninstallAll()`。内部 `ConcurrentHashMap<String, FxControl>`；同 tag 重复 install 先 `cancel` 旧的。
- 局部浮窗（scope）不进注册表，`FloatingX.create(config, host, tag = "")` 返回未注册的 `FxControl`，生命周期归调用方：ViewGroup 从 window 卸下只是 onHostLost（重新挂上再 ready），只有 Activity.fxScope（API 29+）与 Fragment.fxScope 在宿主 destroy 时自动 cancel。tag 只用于日志与位置持久化的存储键：**留空则不做持久化**（多个局部浮窗会共用同一个键，互相覆盖），此时若配了 `storage` 会记一条 error 日志提示补 tag。
- 监听器归 control 实例持有，`cancel()` 清空；框架内不持有 Activity 强引用（#140/#38）。
- `FxActivityTracker`（core，`internal`）：首次需要时 `registerActivityLifecycleCallbacks`；`onActivityDestroyed` 清引用；提供 `topActivity: Activity?` 与 `addObserver`。core 自身不做任何自动初始化（不带 `ContentProvider`）；进程启动即初始化由 floatingx-app 的 `FxAppInitProvider`（清单里声明的 `ContentProvider`）负责，各 host 在 `bind()` 里再调一次兜底（`init` 幂等）。`install` 必须传入 `Application`（或从 context 取 `applicationContext`）。

### 2.8 日志与错误处理

- `FxLogger` 接口 + 默认 `Logcat` 实现，tag 规则 `Fx-<tag>`；未启用时零开销（`if (logger != null)`）。
- 编程错误（非主线程调用、未知 tag、`cancel` 后再操作）抛异常并在消息中给出 tag；运行时可恢复错误（`addView` 失败、WM `BadTokenException`）记日志并触发 `onHostLost`，由状态机等待下一次 ready。
- 所有 `addView/removeView` 通过 `safeAdd/safeRemove` 工具，先检查 `parent`。

## 3. floatingx-app

```kotlin
public class AppHost private constructor(...) : FxHost {
    public class Builder(app: Application) {
        fun blacklist(vararg cls: Class<out Activity>); fun blacklist(vararg names: String)
        fun whitelist(...)
        fun filter(filter: AppActivityFilter)   // #221；fun interface，Kotlin 可传 lambda
        fun attachTo(target: AppAttachTarget)   // DECOR（默认）| CONTENT
        fun theme(@StyleRes themeRes: Int)      // 内容 view 需要主题属性时包一层 ContextThemeWrapper
        fun build(): AppHost
    }
}
// Kotlin DSL
public fun FxInstallScope.appHost(app: Application, block: AppHost.Builder.() -> Unit = {}): AppHost   // 创建并设置 host
```

行为：
- 通过 `FxActivityTracker` 跟踪前台 Activity。re-parent 时机：API 29+ 用 `onActivityPostResumed`，以下用 `onActivityResumed` 后主线程 `Handler.post`。挂载在新 Activity 首帧布局之内完成，位置由锚点在该次 `onLayout` 内推导，不存在"先 0×0 再修正"。换页时同一容器从旧 DecorView 静默挪到新 DecorView，engine 状态 / feature / 动画不重来，位置由 translation 保留、新父首次布局后按锚点校正。
- 进程启动时 floatingx-app 的 `FxAppInitProvider`（ContentProvider，authority `${applicationId}.floatingx.app.init`）自动 `FxActivityTracker.init(application)`，所以任何时机 install 都能拿到当前前台 Activity；用 `tools:node="remove"` 去掉它或运行在非默认进程的应用须自行在 `Application.onCreate` 里调用 `FxActivityTracker.init(app)`。`host.bind()` 仍会调用 `init`（幂等）。
- 过滤规则不通过的 Activity：`detach`（浮窗不显示），而不是像 2.x 一样留在旧 DecorView。
- Activity destroy：若它是当前父容器 → `onHostLost`；否则忽略。`onHostLost` 不改 `desiredVisible`。
- `attachedActivity: Activity?` 通过 `FxControl` 的扩展属性 `control.attachedActivity`（app 模块提供）。
- `bounds()`：父容器尺寸 + `ViewCompat.getRootWindowInsets` 的 systemBars ∪ displayCutout insets；CONTENT 目标时扣掉父容器已被系统栏挤开的偏移。
- 父容器每次布局时 host 比较尺寸与 insets，只在真变化时回调 `onBoundsChanged`（否则页面任意 `requestLayout` 会打断拖动）。
- `modal`（#212/#151）不在 host 上，而是 `FxConfigScope.modal(enabled, dismissOnOutsideTouch)` / `FxConfig.Builder.modal(...)`，对 app 与 scope 的 Layer 容器通用。

## 4. floatingx-system

```kotlin
public class SystemHost private constructor(...) : FxHost {
    public class Builder(context: Context) {
        fun layoutParams(customizer: SystemLayoutParamsCustomizer)           // #194/#220/#241/#235/#155/#211
        fun permission(strategy: FxPermissionStrategy)                       // auto()（默认）| manual(interceptor) | skip()
        fun fallback(host: FxHost)                                           // 权限被拒时 requestSwap（原 SYSTEM_AUTO）
        fun keyboard(vararg editTextIds: Int)                                // hostFeatures() 提供 KeyboardFeature
        fun onBackPressed(listener: SystemBackListener)
        fun theme(@StyleRes themeRes: Int)                                   // 内容 view 用带主题的 application context 创建
        fun build(): SystemHost
    }
    public val windowLayoutParams: WindowManager.LayoutParams   // 只读快照
    public val isPermissionGranted: Boolean
    public fun retryPermission()                                // 被拒后业务方自行拿到权限时调用
}
public fun interface FxPermissionInterceptor { fun onRequest(request: FxPermissionRequest) }
public interface FxPermissionRequest { fun proceed(); fun deny(); fun useFallback() }
// Kotlin DSL
public fun FxInstallScope.systemHost(context: Context, block: SystemHost.Builder.() -> Unit = {}): SystemHost   // 创建并设置 host
```

- 默认 `LayoutParams`：O+ `TYPE_APPLICATION_OVERLAY` 否则 `TYPE_PHONE`；`FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL or FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS`（半隐 / overflow 需要窗口能放到屏幕外；core 已按 safe area clamp，不会误出界）；`format = TRANSLUCENT`；`gravity` 由 `anchor.gravity` 映射；`width/height = WRAP_CONTENT`。用户 customizer 最后执行，可覆盖任何字段（含 `type`、`softInputMode`）。
- `touchable=false` 映射为 `FLAG_NOT_TOUCHABLE`；`KeyboardFeature` 需要焦点时临时去掉 `FLAG_NOT_FOCUSABLE` 并 `updateViewLayout`，失焦后恢复。
- 权限：`FxPermission.isGranted(context)`；申请通过透明 `FxPermissionActivity`（`excludeFromRecents` + 独立 task（`NEW_TASK` + `taskAffinity=""`）——不能用 `noHistory`，否则被设置页遮住时会被系统 finish，收不到 `onActivityResult`）从**任意 context** 启动（Service 可用，#192），回调通过 `FxPermission` 内部的 `SparseArray<FxPermissionCallback>` 按 requestId 分发。`Manual` 策略把 `FxPermissionRequest` 交给用户拦截器。该 Activity 由本模块清单声明，接入方无需自行配置。Q+ 后台无法启动 Activity：后台申请不会弹页面，业务应在前台时申请或用 `Manual`/`Skip` + `retryPermission()`。
- 被拒且有 `fallback` → `session.requestSwap(fallback)`；无 fallback → 状态停留在 `Installed`，`desiredVisible=true`，用户可稍后 `retryPermission()`。
- `bounds()`：`WindowMetrics`（R+）/ `Display.getRealSize` 由容器读取（`FxWindowContainer.refreshBounds()`）；insets 来自 `WindowMetrics`（R+，屏幕级，与窗口位置无关）；R 以下为 NONE——不能用窗口自身的 `onApplyWindowInsets`（wrap_content 窗口拿到的是与自身 frame 相交的值，拖到状态栏边缘会触发 `onBoundsChanged` 打断拖动）。
- 旋转 / 配置变化时由 `FxWindowContainer` 自己刷新布局方向与屏幕尺寸后再回调 `onBoundsChanged`（同步链路里 host 来不及刷新）。直接加到 `WindowManager` 上的 view 没有父级可继承布局方向，容器必须显式把 `Configuration.layoutDirection` 写进 `View.layoutDirection`，否则 RTL 语言下 START/END 会解析反。
- `Builder` 传入 Activity 时解包为 `applicationContext`（系统窗口活得比页面久，不能持有 Activity）；沿 `ContextWrapper.baseContext` 链逐层检查，被 `ContextThemeWrapper` 包着的 Activity 也会被解包。需要主题属性时用 `theme(themeRes)`（在 application context 外包一层 `ContextThemeWrapper`，与 `AppHost.Builder.theme` 一致）。
- attach 失败（`BadTokenException` / `SecurityException` / `IllegalStateException`）时窗口保持未挂载并记录日志（不崩溃）；`retryPermission()` 检测到容器未挂到 WindowManager 时会 `onHostLost` → `onHostReady` 重新挂载（`Skip` 策略同样适用——它压根不检查权限）。detach 时的 `IllegalArgumentException`（view 已不在 WindowManager 上）同样吞掉并把 `isAttachedToWm` 归位。
- **LP 陷阱**：gravity 映射与 `contentPositionOnScreen()` 依赖 `FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS`，customizer 清掉它们会让坐标基准变成 inset 区域；`TYPE_PHONE`（O 以下）已废弃、部分 ROM 拒绝；`permission(skip())` 配了需要权限的 type 时 addView 失败只在 `Fx-system` logcat 可见。
- **fallback 换 host 后**内容 view 仍持有 SystemHost 的 application context（主题属性 / `view.context` 弹窗行为可能与新 host 下不同）。
- **本模块清单声明 `SYSTEM_ALERT_WINDOW`**，会合并进所有使用方（只用 `skip()` + 非 overlay type 的应用也会带上）。

## 5. floatingx-scope

```kotlin
public class ViewGroupHost(viewGroup: ViewGroup) : FxHost            // Java：ViewGroupHost.of(viewGroup)
public class FragmentHost(fragment: Fragment) : FxHost               // compileOnly androidx.fragment
public fun Activity.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl    // R.id.content；API 29+ destroy 自动 cancel
public fun ViewGroup.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl
public fun Fragment.fxScope(tag: String = "", block: FxConfigScope.() -> Unit): FxControl    // view 创建后挂载，fragment destroy 自动 cancel
public fun FxInstallScope.viewGroupHost(viewGroup: ViewGroup): ViewGroupHost
public fun FxInstallScope.fragmentHost(fragment: Fragment): FragmentHost
```

- `ViewGroupHost` 用 Layer 容器，bind 即 ready；`viewGroup` 从 window 卸下 → `onHostLost`，重新挂上 → `onHostReady`。`FragmentHost` 观察 `viewLifecycleOwnerLiveData`：view 创建后 ready、view 销毁即 lost（修 #244）。tag 为空则不做位置持久化。
- 不进 `FloatingX` 注册表。
- Java：`ViewGroupHost.of(viewGroup)` + `FloatingX.create(config, host)`。

## 6. floatingx-compose

```kotlin
public fun FxConfigScope.compose(content: @Composable (FxControl) -> Unit)    // FxContent.Compose
public val LocalFxControl: ProvidableCompositionLocal<FxControl>
public fun FxControl.stateFlow(): StateFlow<FxState>
public fun FxControl.positionFlow(): StateFlow<PointF>
```

- `FxComposeOwner`（`LifecycleOwner + ViewModelStoreOwner + SavedStateRegistryOwner`）由 **control**（engine）持有；每次容器 attach 时 `setViewTreeLifecycleOwner/ViewModelStoreOwner/SavedStateRegistryOwner`，容器 detach 只 `onPause/onStop`，**只在 `cancel()` 时 `onDestroy`**（修 #239/#210）。
- 若容器所在 view tree 已有 owner（AppHost 挂在 Activity 下），则不覆盖，直接复用 Activity 的。
- `ComposeView` 首次测量为 0 的问题由锚点模型自然消化：0 尺寸时不定位，等有效 `onSizeChanged`（修 #184）。
- ViewTree owner 在 `FxContent.create()` 内部设置到内容 view 上（core 只认 `FxContent`，不感知 owner）；
  owner 的 `onDestroy` 挂在 `FxFeature.onCancel()` 上——core 保证它只在 `cancel()` 时来一次，普通 detach 不触发。
- 仅此模块依赖 coroutines；core 无 Flow。

## 7. 公开 API 草案

```kotlin
val control = FloatingX.install("music") {
    layout(R.layout.fx_player)                       // view { ctx -> ... } / compose { ... }
    anchor(FxGravity.CENTER_END, dy = 120f)
    margin(top = 24f)
    overflow(top = true)
    adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), halfHide = FxHalfHide(0.3f)))
    gesture { drag = FxDrag.AfterLongPress; dragRegion = FxRegion.child(R.id.header) }
    animation(FxAnimations.slideIn())
    persist(FxSpStorage(app))
    enableLog()
    systemHost(app) {
        layoutParams { it.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY }
        fallback(appHost(app) { blacklist(SplashActivity::class.java) })
    }
}
control.show(); control.hide(); control.moveTo(100f, 200f); control.moveBy(-20f, 0f)
control.update { anchor(FxGravity.BOTTOM_END); gesture { drag = FxDrag.Disabled } }
control.updateContent { holder -> holder.setText(R.id.title, "…") }
control.addListener(object : FxListener { override fun onDragEnd(c: FxControl, x: Float, y: Float) {} })
control.cancel()

val local = activity.fxScope { layout(R.layout.fx_local); anchor(FxGravity.BOTTOM_START) }
```

```java
FxConfig config = FxConfig.builder()
        .layout(R.layout.fx_player)
        .anchor(FxGravity.CENTER_END, 0f, 120f)
        .gesture(FxGesture.LongPressToDrag)
        .build();
FxHost host = SystemHost.builder(app)
        .layoutParams(lp -> lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        .fallback(AppHost.builder(app).blacklist(SplashActivity.class).build())
        .build();
FxControl control = FloatingX.install("music", config, host);
control.show();
```

Java 互操作约束：所有 DSL-only 入口标 `@JvmSynthetic`；`FxConfig.Builder`、`AppHost.Builder`、`SystemHost.Builder`、`ViewGroupHost.of` 为 Java 入口；`FxListener` 为全默认方法接口；demo 的 `CustomJavaApplication.java` 用新 API 重写并保持编译。

## 8. 性能约束（实施时必须满足）

1. 触摸 MOVE 路径：无对象分配（复用 `PointF/RectF`，无 lambda 捕获），无日志字符串拼接（`logger?.d { }` 惰性）。
2. 移动只写 `translationX/Y`（Layer）或 `LayoutParams.x/y`（Window）；不调用 `requestLayout`。
3. 定位在 `onLayout/onSizeChanged` 内同步完成，禁止 `post/postDelayed` 兜底；唯一定时器是长按。
4. `bounds()` 由 host 按需计算；触摸 MOVE 路径由 core 在拖动开始时缓存 `layoutInput`，host 的父布局监听只在尺寸或 insets 真变化时才派发 `onBoundsChanged`，且无变化路径不构造几何对象。
5. 未启用日志时 `FxLogger` 为 `null`，不走任何格式化。
6. 注册表读路径无锁（`ConcurrentHashMap.get`）。

## 9. issue 覆盖矩阵

| issue | 类型 | 设计点 |
|---|---|---|
| 187 / 172 / 178 / 203 内容尺寸变化跳动 | open/closed | §2.3 锚点 + `LayoutParams.gravity` 映射 + 0 尺寸不定位 |
| 206 收起后不按 gravity 靠边 | open | §2.3 锚点不变 |
| 240 App 级拖动被裁剪 | open | §2.1 Layer 容器只改 translation |
| 195 onCreate 中调用无效 / 192 Service | open/closed | §2.2 命令队列；§4 任意 context 申请权限 |
| 150 / 205 / 180 跨页、recreate、旋转后消失 | closed(反复) | §2.2 `desiredVisible` + `onHostLost/Ready` |
| 201 切页子 view 自毁 / 189 already has parent | closed | §2.2 内容 view 归 engine；safeAdd |
| 218 长按时机 / 222 长按后拖 / 243 & 108 透传 / 165 区域拖动 / 209 & 124 & 137 滚动冲突 / 207 & 37 禁拖 | open/closed | §2.4 `FxGesture` |
| 194 / 220 / 241 / 235 / 155 / 211 LayoutParams | open/closed | §4 `layoutParams {}` + §2.3 `FxOverflow` |
| 204 半隐不对称 / 117 & 157 四向吸附 / 148 贴边方向 | open/closed | §2.3 `FxAdsorb` / `FxHalfHide(start,end)` / `onPositionChanged(anchor)` |
| 242 / 90 RTL | open/closed | §2.3 `ltr` |
| 92 横竖屏位置 / 184 storage 不触发 | closed | §2.3 `FxStorage` key 含 orientation，写入点唯一 |
| 210 / 239 Compose 消失、owner 崩溃 | open | §6 owner 归 control |
| 212 / 151 屏蔽外部触摸、点外部消失 / 154 & 198 Dialog 之上 | open/closed | §2.5 `ModalScrimFeature`；Dialog 之上文档化为用 `ViewGroupHost(dialog.window.decorView)` |
| 221 黑名单父类 | open | §3 `blacklist(Class)` 含子类 + `filter(AppActivityFilter)` |
| 244 Fragment 不显示 | open | §5 等 view 创建 |
| 183 降级后偏移不一致 / 188 拖不到底 | closed | §2.3 `safeArea` 统一 + §2.2 swap 保留配置 |
| 133 遍历所有浮窗 / 200 系统浮窗坐标 | closed | §2.7 `controls()` / `position` |
| 140 / 38 泄漏 | closed | §2.7 监听器归 control，tracker 清引用 |
| 167 / 238 依赖泄漏 | closed | §1 依赖边界 + CI 检查 |
| 129 多进程 | open | 非目标，文档化 |

## 10. 测试策略

| 层 | 对象 | 用例 |
|---|---|---|
| JVM (JUnit) | `FxLayoutResolver` | 9 个 gravity × LTR/RTL × overflow × margin 表驱动；`toAnchor(resolve(a)) == a` 往返；clamp 边界 |
| JVM | `FxAdsorb` | 各方向目标边计算、半隐偏移、四向 |
| JVM | `FxEngine` | 状态迁移表；未 ready 时命令入队并按序回放；`hostLost` 保留 `desiredVisible`；swap 保留 anchor/listener；cancel 清队列 |
| Robolectric | `FxGestureDetector` | 合成 `MotionEvent` 序列：点击 / 长按（按下期间触发）/ 拖动 / 多指（副指抬起不结束）/ AfterLongPress / dragRegion 外不拖 / touchable=false |
| Robolectric | `AppHost` | A→B→back 挂载顺序与位置；B 先 destroy；黑名单页 detach；旋转（demo manifest 去掉 `configChanges`）；父类过滤 |
| Robolectric | `ViewGroupHost` / Fragment | attach 时机、window 卸下 → lost / 重挂 → ready；Fragment view 销毁 → lost、fragment destroy → cancel |
| Robolectric | `SystemHost` | `ShadowSettings.canDrawOverlays`；LP 默认值 + customizer 覆盖；fallback swap |
| Instrumentation (`app/androidTest`) | 真机/模拟器 | WM 窗口 resize 时 `LayoutParams` 序列无跳变；权限流程（CI 用 `adb shell appops set <pkg> SYSTEM_ALERT_WINDOW allow`）；Compose owner 跨 Activity 存活；Layer 容器 modal scrim |
| CI | JUnit 文件扫描 | core 依赖边界；`explicitApi` |

CI（`.github/workflows/android.yml`）：`./gradlew test publishToMavenLocal` + emulator-runner 跑 `connectedCheck`。

## 11. 仓库结构与交付

```
build-logic/                      convention plugin
floatingx-core/
floatingx-app/
floatingx-system/
floatingx-scope/
floatingx-compose/
app/                              demo：每模块一个演示页 + 按 issue 编号命名的回归页（Issue187Activity…）
docs/MIGRATION.md                 2.x → 3.0 API 对照
README.md / README_EN.md          重写
```

- 删除：`floatingx/`、`floatingx_compose/`、`check/detekt/`（无插件应用的死配置）、`gradle/dev/FxComposeSimple.kt`。
- `.github/copilot-instructions.md` 与 `CLAUDE.md` 随新结构更新。
- 发布 `3.0.0`；Release note 附 issue 覆盖矩阵。

## 12. 实施顺序（供 writing-plans 参考）

1. build-logic + 5 个空模块 + CI 依赖边界检查（先让骨架能发布）。
2. core：`FxLayoutResolver` / `FxAdsorb`（纯函数 + JVM 测试）→ `FxEngine`（状态机 + 测试）→ `FxGestureDetector`（Robolectric 测试）→ 容器 + Feature + `FxControl` + 注册表。
3. scope（最简单的 host，用来打通 core 端到端）。
4. app（Activity 跟踪、re-parent、过滤、modal）。
5. system（LP、权限、fallback、keyboard）。
6. compose。
7. demo 重写（含 Java 样例、issue 回归页）+ instrumentation 测试。
8. 文档、MIGRATION、README、发布配置。
