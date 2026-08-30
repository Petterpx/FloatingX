# FloatingX

![image-20210810161316095](https://tva1.sinaimg.cn/large/008i3skNly1gtbrg85hlhj61040k80ui02.jpg)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a9edd107b5444b7ca31738f5a96b3cb9)](https://app.codacy.com/gh/Petterpx/FloatingX?utm_source=github.com&utm_medium=referral&utm_content=Petterpx/FloatingX&utm_campaign=Badge_Grade_Settings)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petterpx/floatingx-core)](https://central.sonatype.com/artifact/io.github.petterpx/floatingx-core)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)

**FloatingX** is a flexible and powerful floating window solution.

[中文文档](https://github.com/Petterpx/FloatingX/blob/main/README.md)

[DeepWiki Fast QA](https://deepwiki.com/Petterpx/FloatingX)

## ✨ What's new in 3.0

3.0 is a full rewrite and is **not** API-compatible with 2.x. Upgrading? See the
[**2.x → 3.0 migration guide**](docs/MIGRATION.md).

- **Five modules, take what you need.** `core` is a pure View implementation (only `androidx.core` +
  `androidx.annotation`); app / system / scope / compose are independent. Skip system windows and no
  `WindowManager` code comes along; skip Compose and no Compose dependency comes along.
- **Anchor-based positioning.** What is stored is "which edge + offset", not a top-left coordinate.
  When the content resizes (longer text, expand/collapse) the anchored edge stays put instead of
  drifting to the bottom-right (#187/#172/#178/#203/#206).
- **State machine + command queue.** `install` / `show` / `moveTo` take effect whenever you call
  them — commands queue up until the host is ready and are then replayed in order. Page changes,
  rotation, Activity recreate, being detached by the blacklist and coming back: the window survives
  all of it (#195/#150/#205/#180).
- **Composable gestures.** Drag mode, drag region, conflict policy with scrollable children and
  touch pass-through are independent switches, no longer one `FxDisplayMode` enum
  (#218/#222/#165/#209/#243).
- **Compose state survives page changes.** Every window owns an `FxComposeOwner` (Lifecycle /
  ViewModelStore / SavedStateRegistry all belong to the window), so `viewModel()` and
  `rememberSaveable` state is not cleared when the host Activity is destroyed (#210/#239).
- **Java friendly.** DSL entry points are `@JvmSynthetic`; Java uses `FxConfig.Builder` /
  `AppHost.Builder` / `SystemHost.Builder` / `ViewGroupHost.of`, and every `FxListener` method has a
  default implementation.

## 📦 Modules & dependencies

| Module | Purpose | minSdk | Floor imposed on consumers |
|---|---|---|---|
| `floatingx-core` | State machine, anchor layout, gestures, features, registry, `FxControl` | 21 | `androidx.core 1.13.1` → **compileSdk ≥ 34** |
| `floatingx-app` | `AppHost`: global window that follows the foreground Activity | 21 | same as core |
| `floatingx-system` | `SystemHost`: `WindowManager` window, overlay permission, keyboard support | 21 | same as core |
| `floatingx-scope` | `ViewGroupHost` / `FragmentHost`: local windows (`androidx.fragment` is `compileOnly`) | 21 | same as core |
| `floatingx-compose` | `compose {}` content, `FxComposeOwner`, `stateFlow()` / `positionFlow()` | **23** | `compose-ui 1.11.4` → **compileSdk ≥ 35**, `lifecycle 2.10.0` |

Kotlin metadata of every module follows Kotlin 2.2, so consumers need **Kotlin ≥ 2.1**. The repo's
own AGP / Gradle / JDK are not propagated.

### Gradle

```groovy
dependencies {
    implementation "io.github.petterpx:floatingx-core:3.0.0"      // required
    implementation "io.github.petterpx:floatingx-app:3.0.0"       // app-level global window (follows Activity)
    implementation "io.github.petterpx:floatingx-system:3.0.0"    // system window (WindowManager + permission)
    implementation "io.github.petterpx:floatingx-scope:3.0.0"     // local window (Activity / ViewGroup / Fragment)
    implementation "io.github.petterpx:floatingx-compose:3.0.0"   // Jetpack Compose content
}
```

The `app` and `system` modules ship their own manifests: `floatingx-app` registers the Activity
tracker from a `ContentProvider` at process start, so `install` picks up the current foreground
Activity no matter when you call it; `floatingx-system` already declares the `SYSTEM_ALERT_WINDOW`
permission and the transparent permission-request Activity — **you don't have to configure
anything**.

## 🚀 Quick start

### App-level global window

```kotlin
val control = FloatingX.install("music") {
    layout(R.layout.fx_card)
    anchor(FxGravity.CENTER_END, dy = 120f)
    margin(top = 24f, bottom = 24f)
    adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), halfHide = FxHalfHide(0.3f)))
    persist(FxSpStorage(app))
    enableLog("Fx-demo")
    appHost(app) {
        // pass a Class, not a class name: matched with isInstance, so subclasses are hit too
        blacklist(SplashActivity::class.java)
    }
}
control.show()
```

Java (the same snippet lives in `app/src/main/java/com/petterp/floatingx/demo/java/JavaDemo.java`):

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

### System window

```kotlin
FloatingX.install("sys") {
    layout(R.layout.fx_input)
    anchor(FxGravity.TOP_START, dx = 24f, dy = 200f)
    adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END)))
    systemHost(app) {
        permission(FxPermissionStrategy.auto())          // default: request automatically
        // fall back to an app-level window when permission is denied (2.x's SYSTEM_AUTO)
        fallback(AppHost.builder(app).build())
        layoutParams { it.alpha = 0.9f }                 // runs after the defaults, may override any field
        keyboard(R.id.etInput)                           // touching these EditTexts makes the window focusable
        onBackPressed { true }                           // only delivered while the keyboard is up
    }
}.show()
```

The three permission strategies:

| Strategy | Behaviour |
|---|---|
| `FxPermissionStrategy.auto()` | Default. Requests via a transparent Activity; on denial falls back to `fallback` (stays `INSTALLED` if none) |
| `FxPermissionStrategy.manual { request -> … }` | You decide: `request.proceed()` to request / `useFallback()` to downgrade / `deny()` to give up |
| `FxPermissionStrategy.skip()` | Attach the window without checking (you requested it yourself, or the type needs no permission) |

Once permission is granted after a denial, call
`(control.host as? SystemHost)?.retryPermission()` to recover. A system window only needs an
application context, so it **can be installed from a Service** (#192).

Java:

```java
SystemHost host = SystemHost.builder(app)
        .layoutParams(lp -> lp.alpha = 0.9f)
        .permission(FxPermissionStrategy.auto())
        .fallback(AppHost.builder(app).build())
        .build();
FxControl control = FloatingX.install("java-system", config, host);
control.show();
```

### Local windows

Local windows are **not** registered; their lifetime belongs to the caller. Call `control.cancel()`
when you no longer need one.

```kotlin
// Activity: attached to android.R.id.content (must be called after setContentView)
val actFx = fxScope("scope-act") {
    layout(R.layout.fx_card)
    anchor(FxGravity.BOTTOM_END)
    persist(FxSpStorage(this@ScopeHostActivity))
}
actFx.show()

// Any ViewGroup: the window is confined to that container
val boxFx = box.fxScope("scope-box") {
    layout(R.layout.fx_card)
    anchor(FxGravity.TOP_START)
}

// Fragment: fine to call in onCreate — it attaches once the view exists and cancels on destroy
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

From Java, use `ViewGroupHost.of(...)` with `FloatingX.create(...)`:

```java
ViewGroup content = activity.findViewById(android.R.id.content);
FxControl control = FloatingX.create(config, ViewGroupHost.of(content), "java-scope");
control.show();
```

### Compose

```kotlin
FloatingX.install("compose") {
    compose { control ->
        val vm: CounterViewModel = viewModel()                        // the window's own ViewModelStore
        var count by rememberSaveable { mutableIntStateOf(0) }        // survives container detach
        val state by control.stateFlow().collectAsState()             // FxState
        val pos by control.positionFlow().collectAsState()            // screen coords of the content's top-left
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

The whole composition runs on the window's own `FxComposeOwner`: `attach → STARTED`,
`show → RESUMED`, `detach → CREATED`, and only `cancel()` reaches `DESTROYED`. Page changes,
rotation and blacklist detach therefore never restart the composition. `compose {}` is agnostic to
the host — app-level and system windows are written exactly the same way.

## 🧰 Capabilities

| Capability | API | Notes |
|---|---|---|
| Anchor positioning | `anchor(FxGravity, dx, dy)` | 9 gravities; `dx/dy` are offsets **inward** from the anchored edge; `START/END` flip under RTL |
| Margin | `margin(left, top, right, bottom)` | Extra inset on the four edges of the usable area |
| Safe area | `safeArea = true/false` | Whether to avoid status bar / navigation bar / display cutout |
| Overflow | `overflow(top, bottom, left, right)` | Which edges the content may exceed |
| Edge adsorption | `adsorb(FxAdsorb.Edges(edges, halfHide, rebound))` | Any of the four edges; `FxAdsorb.horizontal()/vertical()/all()/none()` |
| Half hide | `FxHalfHide(start, end)` | Different ratios for the start and end edges |
| Rebound | `FxAdsorb.Edges(rebound = true)` | May leave the usable area while dragging, springs back on release |
| Drag mode | `gesture { drag = FxDrag.IMMEDIATE / AFTER_LONG_PRESS / DISABLED }` | Drag on touch / after long press / never |
| Drag region | `gesture { dragRegion = FxRegion.child(R.id.header) }` | Also `FxRegion.rect(...)` or your own |
| Child priority | `gesture { childPriority = FxChildPriority.AUTO / PARENT / CHILD }` | Who owns vertical gestures when the content holds a RecyclerView |
| Touch pass-through | `gesture { touchable = false }` | The window eats no touches at all |
| Click / long press | `gesture { click = …; longPress = …; longPressTimeout = … }` | Presets: `FxGesture.Normal / ClickOnly / DisplayOnly / LongPressToDrag` |
| Animation | `animation(FxAnimations.fade())` / `scale()` / a custom `FxAnimation` | Show / hide animations |
| Position persistence | `persist(FxSpStorage(context))` | Key includes the tag and the orientation, so portrait and landscape are remembered separately; or implement `FxStorage` |
| Black / white list | `appHost(app) { blacklist(X::class.java); whitelist(...); filter { … } }` | The Class form matches with `isInstance`, so **subclasses are hit too**; class-name strings also work |
| Attach target | `appHost(app) { attachTo(AppAttachTarget.DECOR / CONTENT) }` | DecorView by default (truly full-screen dragging) |
| Modal | `modal(enabled, dismissOnOutsideTouch)` | Intercepts touches outside the content, optionally hiding on outside touch (app / scope only) |
| Keyboard | `systemHost(app) { keyboard(R.id.etInput) }` | System windows are not focusable by default; touching these EditTexts makes them temporarily focusable |
| Back key | `systemHost(app) { onBackPressed { true } }` | Only delivered while the keyboard is up (that's when the window is focusable) |
| Multiple windows | `FloatingX.install(tag) {}` / `controls()` / `uninstall(tag)` | Global windows are keyed by tag; installing over a tag cancels the old one |
| Logging | `enableLog("Fx-demo")` | `adb logcat \| grep "Fx-"`; completely silent unless enabled |
| Custom behaviour | `addFeature(FxFeature)` | Container behaviour plugin; Location / Gesture / Animation / ModalScrim are built in |

## 📖 API reference

### `FxControl`

```kotlin
control.show(); control.hide(); control.cancel()          // a cancelled control is not reusable; further calls throw IllegalStateException
control.moveTo(100f, 200f); control.moveTo(100f, 200f, animate = true)
control.moveBy(-20f, 0f); control.moveBy(-20f, 0f, animate = true)
control.update { anchor(FxGravity.BOTTOM_END); gesture { drag = FxDrag.DISABLED } }   // patch the config
control.setContent(FxContent.layout(R.layout.fx_card))    // swap the content wholesale
control.updateContent { it.setText(R.id.tvTitle, "Hi") }  // edit views inside the content (works before show)
control.addListener(listener); control.removeListener(listener)
control.addFeature(feature); control.removeFeature(feature)

control.tag; control.state; control.isShowing
control.position        // screen coords of the content's top-left, identical semantics for all hosts
control.anchor; control.config; control.host; control.contentView; control.holder
control.attachedActivity  // floatingx-app extension: which Activity it is currently attached to
```

`FxState`: `INSTALLED` (created, container not attached) → `ATTACHED` (attached but invisible) →
`SHOWN` (visible), with the terminal state `CANCELLED`.

### `FxListener` (all methods have defaults — override only what you need)

```kotlin
onAttach / onDetach / onShow / onHide / onCancel
onClick(control, view) / onLongClick(control, view)
onDragStart(control) / onDrag(control, x, y) / onDragEnd(control, x, y)   // x/y relative to the container
onPositionChanged(control, anchor)                                        // after an anchor commit; tells you which edge it sits on
```

### The `FloatingX` registry

```kotlin
FloatingX.install(tag) { … }        // install and register; installing over a tag cancels the old one
FloatingX.create(tag) { … }         // create without registering, lifetime belongs to the caller (local windows)
FloatingX.control(tag)              // throws when missing
FloatingX.controlOrNull(tag)
FloatingX.controls()                // snapshot of every global window
FloatingX.isInstalled(tag)
FloatingX.uninstall(tag) / FloatingX.uninstallAll()
```

`tag` defaults to `FloatingX.DEFAULT_TAG`. For local windows the `tag` is only used for logging and
as the position-persistence key (leave it empty and nothing is persisted).

## 🏄‍♀️ Demo GIFs

| Full screen, activity, fragment, single view                | Small screen display                                         | Abnormal aspect ratio screen                                 |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![Effect-Display1](https://github.com/Petterpx/FloatingX/blob/main/image/fx-api-simple.gif?raw=true) | ![Demo-Small Screen](https://github.com/Petterpx/FloatingX/blob/main/image/fx-small-gif.gif?raw=true) | ![Abnormal Aspect Ratio](https://github.com/Petterpx/FloatingX/blob/main/image/fx-view-deformed-simple.gif?raw=true) |

| Screen rotation                                             | Feature demo                                                 |      |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ---- |
| ![Demo-Rotation](https://github.com/Petterpx/FloatingX/blob/main/image/fx-rotate-simple.gif?raw=true) | ![Demo-Local Features](https://github.com/Petterpx/FloatingX/blob/main/image/fx-api-simple.gif?raw=true) |      |

## ❓ FAQ

**Q: What happens when the overlay permission is denied?**
With `systemHost(app) { fallback(AppHost.builder(app).build()) }` the window silently downgrades to
an app-level one — the container is swapped but the config, listeners and current position are all
kept (this is 2.x's `SYSTEM_AUTO`). Without a `fallback` it stays `INSTALLED`; call
`SystemHost.retryPermission()` once you have the permission.

**Q: I request the permission from the background (or a Service) and nothing shows up.**
Since Android 10 (Q) the system forbids starting Activities from the background, so the request page
of the `auto()` strategy may **silently fail to appear**. When installing from the background, use
`manual {}` / `skip()` to defer the request and call `SystemHost.retryPermission()` once the app is
in the foreground again.

**Q: System windows don't avoid the status bar / cutout below Android 11.**
`WindowManager.getCurrentWindowMetrics()`, the only public entry point for screen-level insets, was
added in API 30 (R). Below R, `SystemHost` cannot obtain a safe area, so `safeArea` has no effect
(the usable area is the whole screen). Reserve the space yourself with `margin(top = …)` if you need
it on older versions.

**Q: `Activity.fxScope {}` crashes or shows nothing.**
It attaches to `android.R.id.content`, so it must be called **after** `setContentView()`.

**Q: How do I show a floating window on top of a Dialog?**
A Dialog has its own Window that sits above the Activity, so a window attached to the Activity cannot
cover it. Attach it to the Dialog's decorView instead (read `decorView` only after `dialog.show()`):

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

**Q: Does `rememberSaveable` inside `compose {}` survive process death?**
No. It is saved **in-process** only: when the composition is disposed (container detach) the state
goes into the window's own bridging registry and is restored on recomposition. Kill the process and
it is gone (the window itself is not recreated either). Persist it yourself if you need more.

**Q: Is multi-process supported?**
No. The `FloatingX` registry is per-process; a child process sees a separate, empty registry (#129).

**Q: Why is the app-level window attached to `DecorView` instead of `R.id.content`?**
Attaching to `DecorView` is what makes dragging truly full-screen, unaffected by the status bar,
navigation bar or `AppBar`. Use `appHost(app) { attachTo(AppAttachTarget.CONTENT) }` when you want it
confined to the application view area.

## ✅ Issue coverage

| Issue | How 3.0 handles it |
|---|---|
| 187 / 172 / 178 / 203 jumping on content resize | Anchor positioning + `LayoutParams.gravity` mapping; no layout at zero size |
| 206 not snapping by gravity after collapsing | The anchor does not change with the size |
| 240 app-level drag gets clipped | Layer container with `clipChildren=false`; movement only writes `translation` |
| 195 no-op when called in onCreate / 192 Service | Command queue; system windows may request permission from any context |
| 150 / 205 / 180 disappears across pages, recreate, rotation | `desiredVisible` + `onHostLost` / `onHostReady` |
| 201 child views destroyed on page change / 189 already has parent | The content view belongs to the engine; safeAdd |
| 218 long-press timing / 222 drag after long press / 243 & 108 pass-through / 165 drag region / 209 & 124 & 137 scroll conflicts / 207 & 37 disable drag | Composable `FxGesture` |
| 194 / 220 / 241 / 235 / 155 / 211 LayoutParams | `systemHost { layoutParams {} }` + `FxOverflow` |
| 204 asymmetric half hide / 117 & 157 four-way adsorption / 148 which edge | `FxAdsorb` / `FxHalfHide(start, end)` / `onPositionChanged(anchor)` |
| 242 / 90 RTL | Layout direction participates in anchor resolution |
| 92 portrait/landscape position / 184 storage not triggered | `FxStorage` keys include the orientation; a single write point |
| 210 / 239 Compose disappears, owner crash | `FxComposeOwner` belongs to the control |
| 212 / 151 block outside touches, dismiss on outside touch / 154 & 198 above a Dialog | `modal()`; above a Dialog use `viewGroupHost(dialog.window.decorView)` |
| 221 blacklist by superclass | `blacklist(Class)` includes subclasses + `filter(AppActivityFilter)` |
| 244 not shown inside a Fragment | `FragmentHost` waits for the view to be created |
| 183 offset differs after downgrade / 188 cannot drag to the bottom | Unified safe area; host swap keeps the config |
| 133 iterate every window / 200 system window coordinates | `FloatingX.controls()` / `FxControl.position` |
| 140 / 38 leaks | Listeners belong to the control; the Activity tracker clears references |
| 167 / 238 dependency leakage | Module dependency boundary + a CI assertion |
| 129 multi-process | Out of scope, see the FAQ above |

## 📱 Demo

The `app` module is a full sample and regression harness — `./gradlew app:installDebug`.

**Capability pages**

| Page | Content |
|---|---|
| `AppHostActivity` | App-level window: anchor / margin / overflow / safeArea / adsorption / content / animation / `attachedActivity` |
| `SystemHostActivity` | System window: the three permission strategies, `layoutParams` customisation, `retryPermission`, keyboard / back key, install from a foreground Service |
| `ScopeHostActivity` | The three local hosts: ViewGroup / Activity / Fragment |
| `GestureActivity` | Drag mode / drag region / child priority / pass-through / callback log |
| `LayoutActivity` | Anchor / overflow / adsorption / content size / position persistence |
| `MultiWindowActivity` | Multiple windows by tag, `controls()`, reinstalling over a tag |
| `ModalActivity` | Modal touch interception, dismiss on outside touch, a window above a Dialog |
| `ComposeActivity` / `ComposeSecondActivity` | Compose window: `viewModel()` / `rememberSaveable` / `stateFlow` / `positionFlow` / across pages |
| `SecondActivity` | Page change: the container is moved silently, state and position do not restart |
| `BlackActivity` | Blacklisted page (the window disappears) |
| `ImmersedActivity` | Immersive page (edge-to-edge, no status bar) for safeArea |

**Regression pages** (named after the issue)

| Page | Issue |
|---|---|
| `Issue187Activity` | #187 anchor stays put when the content resizes |
| `Issue210Activity` | #210 Compose window survives page changes |
| `Issue221Activity` | #221 blacklist hits subclasses |
| `Issue240Activity` | #240 no clipping when overflowing |
| `Issue244Activity` | #244 window inside a Fragment |

The three buttons in the "Java" group on the home page map to `JavaDemo.java`, which writes all three
kinds of window with Java builders to keep the public API Java-friendly.

## 👍 Thanks

The initial implementation idea of the basic **floating window View** comes from [EnFloatingView](https://github.com/leotyndale/EnFloatingView)'s [FloatingMagnetView](https://github.com/leotyndale/EnFloatingView/blob/master/floatingview/src/main/java/com/imuxuan/floatingview/FloatingMagnetView.java), which was thoroughly refactored and evolved.

The measurement code for the navigation bar comes from Wenlu and has been further adapted to cover 95% of the market models. It can be said to be the only tool that can accurately measure the navigation bar.

## About Me

Welcome to follow my public account and look forward to progressing together. If you have any usage problems, you can also add me on WeChat.

**WeChat**: **Petterpx**

![Petterp-wechat](https://user-images.githubusercontent.com/41142188/226162520-93796619-81ca-4e61-bfff-4a5b95e4fa0b.png)
