# FloatingX

![image-20210810161316095](https://tva1.sinaimg.cn/large/008i3skNly1gtbrg85hlhj61040k80ui02.jpg)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a9edd107b5444b7ca31738f5a96b3cb9)](https://app.codacy.com/gh/Petterpx/FloatingX?utm_source=github.com&utm_medium=referral&utm_content=Petterpx/FloatingX&utm_campaign=Badge_Grade_Settings)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petterpx/floatingx-core)](https://central.sonatype.com/artifact/io.github.petterpx/floatingx-core)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)

[简体中文](README.md) · [Wiki](https://github.com/Petterpx/FloatingX/wiki) · [DeepWiki](https://deepwiki.com/Petterpx/FloatingX)

**FloatingX** is a flexible and powerful floating window solution for Android.

3.0 is a ground-up rewrite split into opt-in modules; the API is not compatible with 2.x, see [Migration from 2.x](https://github.com/Petterpx/FloatingX/wiki/Migration-from-2.x).

## 👏 Features

- Supports **Jetpack Compose**; each window owns a `ViewModelStore` / `SavedState` that survives page changes
- Supports **system windows**, **in-app windows** and **scoped windows** (`ViewGroup` / `Fragment` / `Activity`)
- Supports page changes **without rebuilding**; position, state and animation follow the Activity
- Supports **anchor-based positioning**; rotation and content resizing never drift the window
- Supports **edge snapping**, **half-hide**, **bounds** and **no-clip overflow**
- Supports a **drag region**, **drag after long press**, and **conflict handling** with scrolling lists
- Supports custom **show / hide animations**
- Supports **saving and restoring the last position** (per tag + orientation)
- Supports **blacklist / whitelist** to keep the window off given pages
- Supports **modal windows** that block outside touches
- Supports **system window permission** auto / manual request, with **automatic fallback** to an in-app window when denied
- Supports calling `show` / `moveTo` before the host is ready; they run once it is
- Supports a `kotlin` DSL and a `Java`-friendly Builder
- Full logging: `enableLog(tag)` shows what Fx is doing

## 👨‍💻‍ Dependencies

```groovy
dependencies {
    implementation "io.github.petterpx:floatingx-core:3.0.0"      // pulled in by the other modules
    implementation "io.github.petterpx:floatingx-app:3.0.0"       // app-level global window (follows Activity)
    implementation "io.github.petterpx:floatingx-system:3.0.0"    // system window (WindowManager + permission)
    implementation "io.github.petterpx:floatingx-scope:3.0.0"     // local window (Activity / ViewGroup / Fragment)
    implementation "io.github.petterpx:floatingx-compose:3.0.0"   // Jetpack Compose content
}
```

Only add the modules you use; core comes along with them. minSdk 21 (23 for the compose module). Permissions and the provider are declared in the library manifests, nothing to add on your side.

## 🚀 Quick start

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

For Java, system windows, local windows and Compose windows, see
[Getting Started](https://github.com/Petterpx/FloatingX/wiki/Getting-Started).

## 📖 Documentation

The full documentation lives in the [**GitHub Wiki**](https://github.com/Petterpx/FloatingX/wiki)
(English-titled pages are English, Chinese-titled pages are Chinese):

- [Getting Started](https://github.com/Petterpx/FloatingX/wiki/Getting-Started) — modules, dependencies, minimal samples for the four hosts
- [App Host](https://github.com/Petterpx/FloatingX/wiki/App-Host) — app-level window: black/white list, filter, attach target, theme
- [System Host](https://github.com/Petterpx/FloatingX/wiki/System-Host) — system window: permission strategies, fallback, LayoutParams, keyboard, Service
- [Scope Host](https://github.com/Petterpx/FloatingX/wiki/Scope-Host) — local windows: ViewGroup / Activity / Fragment
- [Compose](https://github.com/Petterpx/FloatingX/wiki/Compose) — `compose {}`, `FxComposeOwner`, `stateFlow` / `positionFlow`
- [Configuration](https://github.com/Petterpx/FloatingX/wiki/Configuration) — capabilities and every config option
- [API Reference](https://github.com/Petterpx/FloatingX/wiki/API-Reference) — `FxControl` / `FxListener` / the `FloatingX` registry
- [FAQ](https://github.com/Petterpx/FloatingX/wiki/FAQ) · [Issue Coverage](https://github.com/Petterpx/FloatingX/wiki/Issue-Coverage) · [Architecture](https://github.com/Petterpx/FloatingX/wiki/Architecture) · [Demo](https://github.com/Petterpx/FloatingX/wiki/Demo)
- Upgrading from 2.x: [Migration from 2.x](https://github.com/Petterpx/FloatingX/wiki/Migration-from-2.x), kept in sync with [`docs/MIGRATION.md`](docs/MIGRATION.md) in the repo

## 🏄‍♀️ Demo GIFs

| App host: drag / adsorb / follows pages | System host: stays on the launcher | Rotation: anchor kept |
| --- | --- | --- |
| ![App host: drag / adsorb / follows pages](https://github.com/Petterpx/FloatingX/blob/main/image/fx-app-host.gif?raw=true) | ![System host: stays on the launcher](https://github.com/Petterpx/FloatingX/blob/main/image/fx-system-host.gif?raw=true) | ![Rotation: anchor kept](https://github.com/Petterpx/FloatingX/blob/main/image/fx-rotate.gif?raw=true) |

| Resize: anchor stays put (#187) | Compose: counter survives pages | Scoped: ViewGroup / Fragment |
| --- | --- | --- |
| ![Resize: anchor stays put (#187)](https://github.com/Petterpx/FloatingX/blob/main/image/fx-resize.gif?raw=true) | ![Compose: counter survives pages](https://github.com/Petterpx/FloatingX/blob/main/image/fx-compose.gif?raw=true) | ![Scoped: ViewGroup / Fragment](https://github.com/Petterpx/FloatingX/blob/main/image/fx-scope.gif?raw=true) |

## 📱 Demo

The `app` module is a full sample and regression harness — `./gradlew app:installDebug` installs it;
the page list is on [Demo](https://github.com/Petterpx/FloatingX/wiki/Demo).

## 👍 Thanks

The initial implementation idea of the basic **floating window View** comes from
[EnFloatingView](https://github.com/leotyndale/EnFloatingView)'s
[FloatingMagnetView](https://github.com/leotyndale/EnFloatingView/blob/master/floatingview/src/main/java/com/imuxuan/floatingview/FloatingMagnetView.java),
which was thoroughly refactored and evolved; the navigation-bar measurement code comes from Wenlu and
has been further adapted to cover more devices.

## About Me

Welcome to follow my public account and look forward to progressing together. If you have any usage
problems, you can also add me on WeChat: **Petterpx**.

![Petterp-wechat](https://user-images.githubusercontent.com/41142188/226162520-93796619-81ca-4e61-bfff-4a5b95e4fa0b.png)
