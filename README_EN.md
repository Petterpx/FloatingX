# FloatingX

![image-20210810161316095](https://tva1.sinaimg.cn/large/008i3skNly1gtbrg85hlhj61040k80ui02.jpg)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a9edd107b5444b7ca31738f5a96b3cb9)](https://app.codacy.com/gh/Petterpx/FloatingX?utm_source=github.com&utm_medium=referral&utm_content=Petterpx/FloatingX&utm_campaign=Badge_Grade_Settings)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petterpx/floatingx-core)](https://central.sonatype.com/artifact/io.github.petterpx/floatingx-core)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)

[简体中文](README.md) · [Wiki](https://github.com/Petterpx/FloatingX/wiki) · [DeepWiki](https://deepwiki.com/Petterpx/FloatingX)

FloatingX is a floating window library for Android. It covers in-app global windows (following the
current Activity), system windows, and scoped windows attached to a ViewGroup / Fragment; the content can
be a View or Compose.

3.0 is a rewrite split into a few opt-in modules. The API is not compatible with 2.x; see
[Migration from 2.x](https://github.com/Petterpx/FloatingX/wiki/Migration-from-2.x) if you are upgrading.

## Features

- Follows Activity changes without rebuilding; position, state and animation are kept
- Position is stored as an anchor (which edge + offset), so rotation and content resizing never drift it
- Drag, edge snapping, half-hide, a restricted drag area, and no fighting with scrolling lists
- Show / hide animations, optional position persistence
- System windows: permission request and fallback to an in-app window when denied are handled for you
- Can block outside touches (modal); show or hide per page with a black / white list
- Calling show / moveTo before the host is ready is fine — it is queued until then
- Compose content gets its own ViewModelStore and SavedState, nothing is lost across pages
- Both a Kotlin DSL and a Java Builder

## Dependencies

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
