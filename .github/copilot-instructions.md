# FloatingX - Android Floating Window Library

FloatingX is an Android floating-window (悬浮窗) library, published to Maven Central as five
artifacts. 3.0 is a full rewrite of 2.x; the old `floatingx/` and `floatingx_compose/` modules no
longer exist and the API is not backwards compatible (`docs/MIGRATION.md` has the mapping).

Library sources, comments and docs are written in Chinese — keep that convention.
For the deeper architecture notes, read `CLAUDE.md` in the repository root; this file only covers
build/validation basics.

**Always reference these instructions first and fall back to search or bash commands only when you
encounter something that does not match the info here.**

## Modules

| Module | Package | Purpose |
|---|---|---|
| `floatingx-core` | `com.petterp.floatingx.core` | State machine (`FxEngine`), anchor layout, gestures, features, `FxControl`, the `FloatingX` registry. Pure View, no `WindowManager` / Fragment / Compose / Lifecycle / AppCompat imports (a JUnit test asserts this boundary). |
| `floatingx-app` | `com.petterp.floatingx.app` | `AppHost` — global window that follows the foreground Activity (DecorView by default), black/white list + custom filters. |
| `floatingx-system` | `com.petterp.floatingx.system` | `SystemHost` — `WindowManager` window, overlay permission (auto / manual / skip), fallback to `AppHost`, keyboard & back key. |
| `floatingx-scope` | `com.petterp.floatingx.scope` | `ViewGroupHost` / `FragmentHost` and the `fxScope {}` local windows. `androidx.fragment` is `compileOnly`. |
| `floatingx-compose` | `com.petterp.floatingx.compose` | `compose {}` content, `FxComposeOwner` (owned by the control), `stateFlow()` / `positionFlow()`. minSdk 23. |
| `app` | `com.petterp.floatingx.demo` | Demo app + instrumentation tests; the manual validation harness. |

All library modules share the `floatingx.library` convention plugin in `build-logic/`
(minSdk 21, compileSdk 36, Java 17, `explicitApi()`, `jvmDefault=enable`, maven coordinates,
Robolectric config). Module `build.gradle.kts` files only declare dependencies.

## Working Effectively

### Prerequisites

- **Java 17** (required by AGP 8.13.2). Gradle 8.14.3, Kotlin 2.2.21.
- Android SDK with compileSdk 36 available.
- Network access to Google Maven / Maven Central / Gradle Plugin Portal.
- `export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"` for large builds.
- `chmod +x gradlew` on a fresh clone.

### Build and test

**NEVER CANCEL builds or tests — set generous timeouts.**

```bash
# Unit tests: 279 JVM/Robolectric cases across the five modules. 2-5 min, timeout 10+ min.
./gradlew test

# One module (core 140 / scope 22 / app 32 / system 60 / compose 25 cases)
./gradlew :floatingx-core:test

# What CI runs on every PR. 3-8 min, timeout 15+ min.
./gradlew publishToMavenLocal -PisPublish=false -PversionName=3.0.0-SNAPSHOT

# Demo APK. 2-4 min, timeout 10+ min.
./gradlew app:assembleDebug
./gradlew app:installDebug

# Android lint. 1-3 min, timeout 5+ min.
./gradlew lint
```

**There is no `detekt` task** — no Gradle plugin applies one, and the old `check/detekt/` config has
been deleted. Do not add a `./gradlew detekt` step.

Robolectric is pinned to **`sdk=35`** in each module's `src/test/resources/robolectric.properties`:
the SDK 36 sandbox needs JDK 21 while this repo's toolchain is JDK 17.

### Instrumentation tests

`app/src/androidTest/` holds five suites: `AppHostReparentTest`, `RotationTest`,
`SystemWindowResizeTest`, `ComposeOwnerSurvivalTest`, `ModalScrimTest` (helpers in `TestUtil.kt`).
CI runs them on `reactivecircus/android-emulator-runner`, api-level 34:

```bash
./gradlew app:installDebug app:installDebugAndroidTest
adb shell appops set com.petterp.floatingx.app SYSTEM_ALERT_WINDOW allow   # system-window cases need this
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
./gradlew app:connectedDebugAndroidTest
```

### Publishing

```bash
# Local, for verifying the published coordinates (then set isDev=false in local.properties)
./gradlew publishToMavenLocal -PisPublish=false -PversionName=3.0.0-SNAPSHOT

# Release (needs GPG env vars); run by the release workflow on a GitHub Release
./gradlew publishAndReleaseToMavenCentral --no-configuration-cache -PisPublish=true -PversionName=$TAG
```

Gradle properties read by `settings.gradle` into `rootProject.ext`:

- `-PversionName` / `-PversionCode` — default to `git describe --tags` / `git rev-list HEAD --count`.
- `-PisPublish=true` — applies `signAllPublications()`.
- `isDev` in `local.properties` (default `true`) — `true` makes `app` depend on the local projects,
  `false` switches it to the published artifacts.

## Validation Requirements

Automated coverage is real now, but UI behaviour still needs the demo. After changing core
behaviour, run `./gradlew test` **and** walk the relevant demo pages
(`./gradlew app:installDebug`, entry point `com.petterp.floatingx.demo.MainActivity`):

- **App-level window** — `AppHostActivity`: anchor / margin / overflow / safeArea / adsorption /
  content swap / animation, `attachedActivity`; then `SecondActivity` (page change),
  `BlackActivity` (blacklist), `ImmersedActivity` (edge-to-edge safeArea).
- **System window** — `SystemHostActivity`: the three permission strategies, `layoutParams`
  customisation, `retryPermission()`, keyboard / back key, install from a foreground Service.
- **Local windows** — `ScopeHostActivity`: ViewGroup / Activity / Fragment hosts.
- **Gestures** — `GestureActivity`: drag mode, drag region, child priority, pass-through, callbacks.
- **Layout** — `LayoutActivity`: anchor, overflow, adsorption, content size, persistence.
- **Multiple windows** — `MultiWindowActivity`: tags, `controls()`, reinstall over a tag.
- **Modal / Dialog** — `ModalActivity`.
- **Compose** — `ComposeActivity` / `ComposeSecondActivity`.
- **Regressions** — `Issue187Activity`, `Issue210Activity`, `Issue221Activity`, `Issue240Activity`,
  `Issue244Activity` (each page's KDoc states the expected behaviour).
- **Java interop** — the "Java" group on the home page (`demo/java/JavaDemo.java`). Any public API
  change must keep this file compiling.

Expected outcomes: windows draggable and responsive; no crash during permission requests or
lifecycle changes; state and position preserved across page changes, rotation and recreate; local
windows confined to their container and cleaned up on destroy.

Demo Activities deliberately declare **no `configChanges`** — rotation goes through recreate.

## Project Structure

```
build-logic/            floatingx.library convention plugin
floatingx-core/         state machine, layout, gestures, features, registry
floatingx-app/          AppHost
floatingx-system/       SystemHost
floatingx-scope/        ViewGroupHost / FragmentHost
floatingx-compose/      compose {} + FxComposeOwner
app/                    demo + androidTest
docs/MIGRATION.md       2.x → 3.0 API mapping
docs/superpowers/       spec + implementation plans
gradle/libs.versions.toml   version catalog
```

Frequently touched entry points:

- `floatingx-core/src/main/kotlin/com/petterp/floatingx/core/FloatingX.kt` — registry.
- `.../core/FxControl.kt`, `.../core/config/FxConfig.kt`, `.../core/config/FxConfigScope.kt` —
  public API surface. Adding a config option means touching **both** the DSL scope and the Java
  builder.
- `.../core/engine/FxEngine.kt` — state machine and command queue.
- `.../core/feature/` — prefer a new `FxFeature` over new code inside the container.
- `app/src/main/java/com/petterp/floatingx/demo/DemoWindows.kt` — every global demo window.

## Dependencies and Permissions

Consumers do **not** need to declare anything in their manifest: `floatingx-system` declares
`SYSTEM_ALERT_WINDOW` plus the transparent `FxPermissionActivity`, and `floatingx-app` registers its
Activity tracker from `FxAppInitProvider` (a `ContentProvider`).

```groovy
dependencies {
    implementation "io.github.petterpx:floatingx-core:3.0.0"      // required
    implementation "io.github.petterpx:floatingx-app:3.0.0"
    implementation "io.github.petterpx:floatingx-system:3.0.0"
    implementation "io.github.petterpx:floatingx-scope:3.0.0"
    implementation "io.github.petterpx:floatingx-compose:3.0.0"
}
```

Floors imposed on consumers: compileSdk ≥ 34 (core, via `androidx.core 1.13.1`), compileSdk ≥ 35 for
`floatingx-compose` (`compose-ui 1.11.4`), Kotlin ≥ 2.1, minSdk 21 (23 for compose).

## Troubleshooting

- **"Plugin not found"** — check connectivity to `google()` / `mavenCentral()` / the plugin portal.
- **OutOfMemoryError** — raise `GRADLE_OPTS` heap.
- **Permission denied on `./gradlew`** — `chmod +x gradlew`.
- **Robolectric "sandbox requires JDK 21"** — something changed `sdk=` away from 35 in a
  `robolectric.properties`.
- **System window not showing** — grant `SYSTEM_ALERT_WINDOW`, or configure a
  `fallback(AppHost.builder(app).build())` so it downgrades. Below Android 11 there is no
  screen-level safe area for system windows.
- **Local window not appearing** — `Activity.fxScope {}` must be called after `setContentView()`;
  `Fragment.fxScope {}` waits for the view to be created.
- **Instrumentation flakes** — disable the three animation scales and grant the overlay appop as
  shown above; assert with `TestUtil.await`, never a bare sleep.

## Quick Reference

```bash
./gradlew projects                 # module list
./gradlew tasks                    # available tasks
./gradlew app:dependencies         # dependency tree
./gradlew build --stacktrace       # verbose build

adb shell am start -n com.petterp.floatingx.app/com.petterp.floatingx.demo.MainActivity
adb logcat | grep "Fx-"            # log tags are Fx-<scope>; only emitted after enableLog(tag)
adb shell pm clear com.petterp.floatingx.app
adb shell appops get com.petterp.floatingx.app SYSTEM_ALERT_WINDOW
adb shell dumpsys window | grep -i float
```

## CI/CD

`.github/workflows/android.yml` has two jobs:

1. `build` — `./gradlew test`, then
   `./gradlew publishToMavenLocal -PisPublish=false -PversionName=3.0.0-SNAPSHOT`.
2. `instrumentation` — emulator-runner (api 34, x86_64, pixel_6) running
   `app:connectedDebugAndroidTest` with the overlay appop granted; test reports are uploaded as an
   artifact.

Publication to Maven Central happens on a GitHub Release and requires the signing keys.
