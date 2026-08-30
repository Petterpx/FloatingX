# FloatingX

![image-20210810161316095](https://tva1.sinaimg.cn/large/008i3skNly1gtbrg85hlhj61040k80ui02.jpg)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a9edd107b5444b7ca31738f5a96b3cb9)](https://app.codacy.com/gh/Petterpx/FloatingX?utm_source=github.com&utm_medium=referral&utm_content=Petterpx/FloatingX&utm_campaign=Badge_Grade_Settings)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petterpx/floatingx-core)](https://central.sonatype.com/artifact/io.github.petterpx/floatingx-core)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)

[简体中文](README.md) · [Wiki](https://github.com/Petterpx/FloatingX/wiki) · [DeepWiki Fast QA](https://deepwiki.com/Petterpx/FloatingX)

**FloatingX** is a flexible and powerful floating-window solution for Android: app-level global
windows (following the foreground Activity), system windows (`WindowManager`), local windows
(ViewGroup / Activity / Fragment) and Jetpack Compose content — all through one API. 3.0 is a full
rewrite split into five modules you can take one by one, and it is **not** API-compatible with 2.x.

## ✨ Features

- **Four hosts, one API**: app-level global window (follows the foreground Activity), system window (`WindowManager`), scoped window (ViewGroup / Activity / Fragment) and Jetpack Compose content.
- **No rebuild on page change**: the same container is silently re-parented between Activities; state, position and animation are all kept.
- **Anchor-based positioning**: stored as "which edge + offset", so content resize and screen rotation never drift the window; margin, bounds, safe area and no-clip overflow are supported.
- **Gestures**: drag, edge adsorption with half-hide, a dedicated drag region (dragRegion), and a conflict policy against scrolling children (childPriority).
- **Animation & persistence**: built-in fade / scale show-hide animations, fully customizable; position persisted per tag + orientation.
- **Painless system windows**: three permission strategies (auto / manual / skip), automatic fallback to the app-level host when denied; customizable `LayoutParams`, keyboard and back-key support.
- **Modal windows**: a scrim intercepts outside touches, optionally dismissing on outside tap.
- **Call at any time**: state machine + command queue — `show` / `moveTo` issued before the host is ready are queued and replayed in order.
- **Page control**: blacklist / whitelist / custom filter decide where the window appears (matched by Class, subclasses included).
- **Compose-native**: every window owns an `FxComposeOwner`; `viewModel()` / `rememberSaveable` survive page changes; `stateFlow()` / `positionFlow()` are observable.
- **Pick what you need, two entry points**: five modules pulled in on demand; Kotlin DSL and Java Builder, with `explicitApi` keeping the public surface clean.

## Dependencies

```groovy
dependencies {
    implementation "io.github.petterpx:floatingx-core:3.0.0"      // pulled in by every other module; declare only when using core alone
    implementation "io.github.petterpx:floatingx-app:3.0.0"       // app-level global window (follows Activity)
    implementation "io.github.petterpx:floatingx-system:3.0.0"    // system window (WindowManager + permission)
    implementation "io.github.petterpx:floatingx-scope:3.0.0"     // local window (Activity / ViewGroup / Fragment)
    implementation "io.github.petterpx:floatingx-compose:3.0.0"   // Jetpack Compose content
}
```

`floatingx-core` comes transitively (`api`) with each of the other four modules, so it rarely needs to be declared; everything else is optional. minSdk 21 (23 for `floatingx-compose`);
the `app` and `system` modules ship their own manifests, so you don't have to configure anything.

## Quick start

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

## Documentation

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

## Demo GIFs

| App host: drag / adsorb / follows pages | System host: stays on the launcher | Rotation: anchor kept |
| --- | --- | --- |
| ![App host: drag / adsorb / follows pages](https://github.com/Petterpx/FloatingX/blob/main/image/fx-app-host.gif?raw=true) | ![System host: stays on the launcher](https://github.com/Petterpx/FloatingX/blob/main/image/fx-system-host.gif?raw=true) | ![Rotation: anchor kept](https://github.com/Petterpx/FloatingX/blob/main/image/fx-rotate.gif?raw=true) |

| Resize: anchor stays put (#187) | Compose: counter survives pages | Scoped: ViewGroup / Fragment |
| --- | --- | --- |
| ![Resize: anchor stays put (#187)](https://github.com/Petterpx/FloatingX/blob/main/image/fx-resize.gif?raw=true) | ![Compose: counter survives pages](https://github.com/Petterpx/FloatingX/blob/main/image/fx-compose.gif?raw=true) | ![Scoped: ViewGroup / Fragment](https://github.com/Petterpx/FloatingX/blob/main/image/fx-scope.gif?raw=true) |

## Demo

The `app` module is a full sample and regression harness — `./gradlew app:installDebug` installs it;
the page list is on [Demo](https://github.com/Petterpx/FloatingX/wiki/Demo).

## Thanks

The initial implementation idea of the basic **floating window View** comes from
[EnFloatingView](https://github.com/leotyndale/EnFloatingView)'s
[FloatingMagnetView](https://github.com/leotyndale/EnFloatingView/blob/master/floatingview/src/main/java/com/imuxuan/floatingview/FloatingMagnetView.java),
which was thoroughly refactored and evolved; the navigation-bar measurement code comes from Wenlu and
has been further adapted to cover more devices.

## About Me

Welcome to follow my public account and look forward to progressing together. If you have any usage
problems, you can also add me on WeChat: **Petterpx**.

![Petterp-wechat](https://user-images.githubusercontent.com/41142188/226162520-93796619-81ca-4e61-bfff-4a5b95e4fa0b.png)
