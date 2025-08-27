# FloatingX - Android Floating Window Library

FloatingX is an Android library that provides flexible and powerful floating window solutions, supporting system-level, app-level, and local floating windows with JetPack Compose support.

**Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

## Working Effectively

### Prerequisites and Setup
- Install Java 17 (OpenJDK recommended): Required for Android Gradle Plugin 8+
- Ensure Android SDK is available (minimum SDK 21, target/compile SDK 34)  
- **CRITICAL**: Ensure internet access to Google Maven and Gradle Plugin repositories
- **CRITICAL**: Set JVM heap size: `export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"`

### Bootstrap, Build, and Test Repository
**NEVER CANCEL builds or tests - they may take significant time. Use appropriate timeouts.**

```bash
# Grant execute permission (required on fresh clone)
chmod +x gradlew

# Clean and build all modules - NEVER CANCEL: Takes 3-8 minutes. Set timeout to 15+ minutes.
./gradlew clean build publishToMavenLocal -PisPublish=false -PversionName=1.0.0

# Run tests - NEVER CANCEL: Takes 2-5 minutes. Set timeout to 10+ minutes.
./gradlew test

# Run instrumented tests (if device/emulator available) - NEVER CANCEL: Takes 5-15 minutes. Set timeout to 20+ minutes.
./gradlew connectedAndroidTest
```

### Build the Demo Application
```bash
# Build debug APK - NEVER CANCEL: Takes 2-4 minutes. Set timeout to 10+ minutes.
./gradlew app:assembleDebug

# Install and run on connected device/emulator
./gradlew app:installDebug
adb shell am start -n com.petterp.floatingx.app/.MainActivity
```

### Library Publication (Development)
```bash
# Publish to local Maven repository - NEVER CANCEL: Takes 3-6 minutes. Set timeout to 15+ minutes.
./gradlew publishToMavenLocal -PisPublish=false -PversionName=1.0.0-SNAPSHOT

# For release publication (requires signing keys)
./gradlew publishAllPublicationsToMavenCentralRepository -PisPublish=true -PversionName=X.Y.Z
```

## Validation Requirements

### Manual Testing Scenarios
Always perform these validation steps after making changes to core library functionality:

1. **System Floating Window Test**:
   - Build and install demo app: `./gradlew app:installDebug`
   - Launch app and navigate to "System Floating Window" option
   - Grant SYSTEM_ALERT_WINDOW permission when prompted  
   - Use `TestActivity` → "进入多浮窗页面(测试多浮窗功能)" for comprehensive testing
   - Verify floating window appears and is draggable across the screen
   - Test edge absorption, boundary bounce, and multi-touch

2. **App-Level Floating Window Test**:
   - Navigate to app-level floating window demo via `MainActivity`
   - Test local floating windows: "显示局部悬浮窗" button
   - Verify floating window works within app boundaries
   - Test rotation, app switching, and lifecycle scenarios

3. **Local/Scoped Floating Window Test**:
   - Use `TestActivity` → "进入局部悬浮窗页面-(测试api功能)" (`ScopeActivity`)
   - Test Activity, Fragment, and ViewGroup scoped windows
   - Verify floating windows appear only within their designated containers
   - Test view lifecycle and cleanup

4. **Compose Integration Test**:
   - Test Compose floating windows functionality in demo app
   - Verify `enableComposeSupport()` call is working (see `FxComposeSimple.kt`)
   - Test Compose UI rendering within floating windows
   - Check system floating windows with Compose content

5. **Edge Cases and Special Scenarios**:
   - Test immersive mode: "进入无状态栏页面-(测试状态栏影响)" (`ImmersedActivity`)
   - Test RecyclerView interaction: "进入recyclerView测试页面" (`SimpleRvActivity`)
   - Test blacklist functionality: "进入黑名单页面(该页面禁止展示浮窗1)" (`BlackActivity`)

**Expected Outcomes**:
- All floating windows should be draggable and responsive
- No crashes during permission requests or lifecycle changes
- Proper cleanup when Activities/Fragments are destroyed
- Floating windows should respect their scope boundaries

### Build Validation
Always run before committing changes:
```bash
# Lint check - NEVER CANCEL: Takes 1-3 minutes. Set timeout to 5+ minutes.
./gradlew lint

# Code style check with Detekt (configured in check/detekt/detekt.yml)
./gradlew detekt

# Full CI validation - NEVER CANCEL: Takes 5-10 minutes. Set timeout to 20+ minutes.
./gradlew clean build publishToMavenLocal -PisPublish=false -PversionName=1.0
```

## Project Structure and Navigation

### Key Modules
- **`app/`** - Demo application showcasing all FloatingX features
  - Entry point: `com.petterp.floatingx.app.MainActivity`
  - Test activities: `com.petterp.floatingx.app.TestActivity`
  - Example implementations in `app/src/main/java/com/petterp/floatingx/app/kotlin/`

- **`floatingx/`** - Core floating window library
  - Main API: `com.petterp.floatingx.FloatingX`
  - Core classes: `src/main/java/com/petterp/floatingx/`
  - **Important**: Always check this module when modifying core functionality

- **`floatingx_compose/`** - JetPack Compose support extension
  - Compose integration: `src/main/java/com/petterp/floatingx/compose/`
  - **Critical**: Required for system floating windows with Compose

### Frequently Modified Files
- `floatingx/src/main/java/com/petterp/floatingx/FloatingX.kt` - Main API entry point
- `floatingx/src/main/java/com/petterp/floatingx/assist/` - Configuration helpers
- `app/src/main/java/com/petterp/floatingx/app/MainActivity.kt` - Demo app main screen
- `floatingx_compose/src/main/java/com/petterp/floatingx/compose/` - Compose integration

### Configuration Files
- **`gradle/libs.versions.toml`** - Version catalog for all dependencies
- **`gradle.properties`** - Project-wide settings and Maven publication config
- **`app/src/main/AndroidManifest.xml`** - Required permissions and component declarations

## Common Tasks and Expected Build Times

### Build Commands and Timing
**CRITICAL**: All timing estimates include 50% buffer. NEVER CANCEL operations before these timeouts.

| Command | Expected Time | Timeout Setting | Description |
|---------|--------------|----------------|-------------|
| `./gradlew clean` | 30s | 2 minutes | Clean all build artifacts |
| `./gradlew build` | 3-8 minutes | 15 minutes | Full build all modules |
| `./gradlew test` | 2-5 minutes | 10 minutes | Run unit tests |
| `./gradlew connectedAndroidTest` | 5-15 minutes | 20 minutes | Run instrumented tests |
| `./gradlew publishToMavenLocal` | 3-6 minutes | 15 minutes | Publish to local Maven |
| `./gradlew app:assembleDebug` | 2-4 minutes | 10 minutes | Build demo APK |
| `./gradlew detekt` | 1-2 minutes | 5 minutes | Code style checks |
| `./gradlew lint` | 1-3 minutes | 5 minutes | Android lint checks |

### Development Workflow
1. **Before making changes**: Always run `./gradlew clean build` to ensure clean starting state
2. **During development**: Use `./gradlew build` for incremental builds
3. **Before committing**: Run `./gradlew lint detekt` and manual validation scenarios
4. **Testing changes**: Build and install demo app, run through validation scenarios

**Important Notes**:
- **NETWORK DEPENDENCY**: Initial builds require internet access to download Android Gradle Plugin and dependencies
- **BUILD VALIDATION**: Due to network restrictions in some environments, these instructions are based on repository analysis and Android development best practices
- **ACTUAL BUILD TESTING**: In environments with network access, all build commands should be validated before use
- Network access required for initial dependency downloads
- JitPack configuration available (`jitpack.yml`) - requires OpenJDK 11+
- ProGuard configurations in each module ensure proper code obfuscation for release builds
- Detekt configuration in `check/detekt/detekt.yml` defines code style rules

## Demo Application Usage Patterns
The demo app (`app` module) showcases FloatingX capabilities:

**Main Entry Points**:
- `MainActivity.kt` - Primary demo with local floating windows
- `TestActivity.kt` - Comprehensive test suite for all floating window types
- `MultipleFxActivity.kt` - Multiple floating windows demonstration
- `SystemActivity.kt` - System-level floating window examples

**Key Demo Features**:
- Global floating window management via `FloatingX.install{}`
- Local scoped floating windows via `createFx{}` delegate
- Compose integration examples in `kotlin/FxComposeSimple.kt`
- Animation implementations in `simple/FxAnimationImpl.kt`

**Testing Different Scenarios**:
```kotlin
// System floating window (requires permission)
FloatingX.install {
    setContext(context)
    setLayout(R.layout.item_floating)
    setScopeType(FxScopeType.SYSTEM_AUTO)
}.show()

// Local floating window (scoped to Activity)
private val activityFx by createFx {
    setLayout(R.layout.item_floating)
    setEnableLog(true, "activityFx")
    build().toControl(this@MainActivity)
}
```

## Dependencies and Permissions

### Required Permissions (in AndroidManifest.xml)
```xml
<!-- For system-level floating windows -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.SYSTEM_OVERLAY_WINDOW" />
```

### Gradle Dependencies
```groovy
// Core library
implementation 'io.github.petterpx:floatingx:2.3.7'

// Compose support (required for system floating windows with Compose)
implementation 'io.github.petterpx:floatingx-compose:2.3.7'
```

### Key Environment Variables
- `GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"` - Increase heap for large builds
- `isDev=true` - Use local project dependencies instead of published versions
- `isPublish=false` - Development mode (no signing required)

## Troubleshooting Common Issues

### Build Failures
- **"Plugin not found"**: Check internet connectivity and ensure `google()` repository is accessible
  - **Common in restricted environments**: If external repositories are blocked, builds will fail
  - **Workaround**: In environments with network restrictions, use pre-downloaded dependencies or mirror repositories
- **OutOfMemoryError**: Increase `GRADLE_OPTS` heap size to `-Xmx4g` or higher
- **Permission denied**: Run `chmod +x gradlew` on fresh clones
- **Gradle version compatibility**: This project requires Gradle 8.14.3+ with Android Gradle Plugin 8.1+

### Runtime Issues
- **System floating window not showing**: Ensure SYSTEM_ALERT_WINDOW permission is granted
- **Compose floating window crashes**: Verify `enableComposeSupport()` is called in AppHelper
- **Local floating window not appearing**: Check container view lifecycle and attachment

### Testing Issues
- **Instrumented tests failing**: Ensure device/emulator is connected and unlocked
- **UI tests timeout**: Floating windows may need extra time to settle, increase test timeouts

## Quick Reference Commands

```bash
# Repository root files overview
ls -la  # Shows: build.gradle, settings.gradle, gradlew, app/, floatingx/, floatingx_compose/

# Check project structure and modules
./gradlew projects

# View available tasks
./gradlew tasks

# Debug build issues with verbose output
./gradlew build --stacktrace --info

# Check dependency tree
./gradlew app:dependencies

# Generate APK with specific version
./gradlew app:assembleDebug -PversionName=1.0.0-SNAPSHOT

# Launch demo app after installation
adb shell am start -n com.petterp.floatingx.app/.MainActivity

# View app logs (useful for debugging floating window issues)
adb logcat -s FloatingX

# Clear app data during testing
adb shell pm clear com.petterp.floatingx.app

# Check if device/emulator has overlay permission
adb shell appops get com.petterp.floatingx.app SYSTEM_ALERT_WINDOW
```

## Debugging Common Floating Window Issues

```bash
# Check floating window permissions
adb shell dumpsys package com.petterp.floatingx.app | grep -i "permission"

# Monitor window manager for floating window activity
adb shell dumpsys window | grep -i float

# Debug touch events on floating windows
adb shell getevent | grep -i touch

# View current running activities (useful when testing scope)
adb shell dumpsys activity activities | grep -i petterp
```

## CI/CD Notes
- GitHub Actions CI runs: `./gradlew publishToMavenLocal -PisPublish=false -PversionName=1.0`
- Build environment: Ubuntu latest, Java 17, Gradle cache enabled
- No UI testing in CI (requires manual validation)
- Publication to Maven Central requires release signing keys