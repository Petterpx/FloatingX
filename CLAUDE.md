# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

FloatingX — an Android floating-window (悬浮窗) library published to Maven Central as
`io.github.petterpx:floatingx` / `io.github.petterpx:floatingx-compose`. Comments, logs and docs
are written in Chinese; keep that convention when editing library sources.

Three Gradle modules:

| Module | Purpose |
|---|---|
| `floatingx` | The core library. Pure View-based, only depends on `appcompat` + `kotlin-stdlib`. |
| `floatingx_compose` | Optional add-on providing a `ViewTreeLifecycleOwner`/`SavedStateRegistry` so Compose content can live inside a **system** floating window. |
| `app` | Demo app; also the only real test harness (see Validation). |

## 3.0 重构进行中

设计与计划见 `docs/superpowers/specs/2026-08-29-floatingx-3-modular-architecture-design.md`
与 `docs/superpowers/plans/`。新模块 `floatingx-core`（包 `com.petterp.floatingx.core`）已落地，
用 `build-logic/` 的 `floatingx.library` convention plugin；旧的 `floatingx` / `floatingx_compose`
模块在 demo 重写前保留。跑 core 测试：`./gradlew :floatingx-core:test`。

## Build & Commands

Java 17 is required (AGP 8.x). Version catalog: `gradle/libs.versions.toml`.

```bash
./gradlew publishToMavenLocal -PisPublish=false -PversionName=1.0   # what CI runs on every PR
./gradlew app:assembleDebug                                          # build the demo apk
./gradlew app:installDebug                                           # install demo on device
./gradlew lint                                                       # android lint
./gradlew test                                                       # only placeholder ExampleUnitTest exists
```

Publishing (release workflow, on GitHub Release):
`./gradlew publishAndReleaseToMavenCentral --no-configuration-cache -PisPublish=true -PversionName=$TAG`

Gradle properties that change behaviour (`settings.gradle` reads them into `rootProject.ext`):

- `-PversionName` / `-PversionCode` — default to `git describe --tags` and `git rev-list HEAD --count`.
- `-PisPublish` — when `true`, `signAllPublications()` is applied (needs GPG env vars).
- `isDev` in `local.properties` (default `true`) — `true` makes `app` depend on the local
  `:floatingx` / `:floatingx_compose` projects; `false` switches it to the published artifacts.

Notes that contradict `.github/copilot-instructions.md` — trust this file:

- **There is no `detekt` task.** `check/detekt/detekt.yml` and the jar exist, but no Gradle plugin
  applies them and the CI detekt step is commented out in `.github/workflows/android.yml`.
- The library modules have **no tests at all** (`floatingx/src/` contains only `main`); `app` and
  `floatingx_compose` only carry generated `ExampleUnitTest`. Behaviour is verified by hand
  through the demo app.
- Log tags are `Fx-<scope>` (e.g. `Fx-app`, `Fx-system`, `Fx-activity`), not `FloatingX`, so use
  `adb logcat | grep "Fx-"`. Logs only appear when `setEnableLog(true)` was called on the builder.

## Architecture

### Three-layer split: Helper (config) → Control (public API) → Provider (platform)

1. **Helper** — `assist/helper/`. Immutable-ish config built by a DSL/Java builder.
   `FxBasisHelper` holds every shared option as `internal @JvmField`; `FxAppHelper` adds
   global-only concerns (tag, black/whitelist, `FxScopeType`, permission interceptor, keyboard
   adaptation); `FxScopeHelper` adds the `toControl(Activity|Fragment|ViewGroup)` entry points.
   Helpers are the single source of truth — `updateConfig {}` mutates the helper, and the view
   helpers re-read it.

2. **Control** — `imp/FxBasisControlImp` implements the user-facing `IFxControl` (`show`, `hide`,
   `cancel`, `move`, `updateView`, `updateViewContent`, click listeners) and delegates everything
   platform-specific to a provider. Subclasses only override `createPlatformProvider()` and
   optionally `createConfigProvider()` / `createAnimationProvider()`. **`initProvider()` must be
   called right after constructing a control** — the provider fields are `lateinit`.

3. **Provider** — `imp/{app,system,scope}/`, all implementing `IFxPlatformProvider`. A provider owns
   the container view, the container `ViewGroup`, and attach/detach:
   - `FxAppPlatformProvider` — app-level, no permission. Adds `FxDefaultContainerView` to the
     current Activity's **`DecorView`** (deliberately not `R.id.content`, so dragging is truly
     fullscreen and unaffected by status/navigation bars). Follows the foreground Activity via
     `FxAppLifecycleImp` (an `ActivityLifecycleCallbacks` registered on the `Application`).
   - `FxSystemPlatformProvider` — system-level. Adds `FxSystemContainerView` to `WindowManager`,
     handles the `SYSTEM_ALERT_WINDOW` permission request (via the invisible
     `FxPermissionActivity`/fragment in `util/`), and with `FxScopeType.SYSTEM_AUTO` silently
     falls back to the app-level implementation when permission is denied.
   - `FxScopePlatFromProvider` — local floating window bound to a given `ViewGroup`
     (Activity → `R.id.content`, Fragment → its root view, or any `ViewGroup`).

   `FloatingX.install()` picks `FxSystemControlImp` vs `FxAppControlImp` from
   `FxScopeType.hasPermission` at install time.

### Global registry

`FloatingX` (object) keeps `HashMap<tag, IFxAppControl>`. Every global floating window is addressed
by a tag (`FX_DEFAULT_TAG` when unset); installing over an existing tag cancels the old one. Local
(scope) floating windows are **not** registered here — their lifetime is the caller's.

### The container view and its helpers

`view/FxBasicContainerView` is a `FrameLayout` subclass that hosts the user's layout/view and wraps
it in a `FxViewHolder`. It delegates all behaviour to a fixed list of `FxViewBasicHelper`s, each
receiving `initConfig` / `onInit` / `onSizeChanged` / `onConfigurationChanged` / `onPreCancel`:

- `FxViewTouchHelper` — multi-touch gesture arbitration, click vs. drag, long-press, `FxDisplayMode`.
- `FxViewLocationHelper` — coordinates, gravity, boundaries, edge adsorption, rebound, half-hide,
  rotation/config-change restore, and persistence through `IFxConfigStorage`.
- `FxViewAnimationHelper` — show/hide animation driven by `FxAnimation`.

Subclasses (`FxDefaultContainerView`, `FxSystemContainerView`) implement only the platform-specific
bits: `updateXY`, `parentSize`, and the touch-down/move/cancel hooks (the system variant writes back
to `WindowManager.LayoutParams`; the default variant moves the view directly).

When adding a new behaviour, prefer a new `FxViewBasicHelper` over code in the container view, and a
new flag on `FxBasisHelper` + its `Builder` over a new constructor parameter.

### Java interop

The public API is Kotlin but must stay Java-friendly: builders expose `builder()` +
`@JvmStatic`/`@JvmOverloads`, DSL-only entry points are marked `@JvmSynthetic`, and callback
interfaces meant for Java (`IFxContextProvider`, `IFxHolderProvider`) are written in Java. Keep
`CustomJavaApplication.java` in the demo compiling.

## Validation

There is no automated coverage, so exercise changes through the demo app —
`MainActivity` (local windows) and `TestActivity`, which links to `MultipleFxActivity` (multi-window
by tag), `ScopeActivity` (Activity/Fragment/ViewGroup scopes), `SystemActivity` (system window +
permission flow), `ImmersedActivity` (no status bar), `SimpleRvActivity` (RecyclerView scroll
interaction) and `BlackActivity` (blacklist). Compose usage lives in `kotlin/FxComposeSimple.kt`
and requires `enableComposeSupport()` on the `AppHelper.Builder`.
