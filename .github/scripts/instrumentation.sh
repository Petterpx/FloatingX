#!/usr/bin/env bash
# CI 模拟器上跑 instrumentation：装包、授权、关动画，失败先留 logcat + 截图再重跑一次。
# emulator-runner 的 script 是逐行 sh -c 执行的，函数 / 变量跨行不保留，所以逻辑放在这个文件里。
set -u
cd "$(dirname "$0")/../.."

./gradlew app:installDebug app:installDebugAndroidTest
adb shell appops set com.petterp.floatingx.app SYSTEM_ALERT_WINDOW allow
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard

run_tests() {
  adb logcat -c
  ./gradlew app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
}
dump() {
  mkdir -p app/build/ci
  adb exec-out screencap -p > "app/build/ci/failure-$1.png" || true
  adb logcat -d > "app/build/ci/logcat-$1.txt" || true
}

if run_tests; then exit 0; fi
dump 1
echo "::warning::connectedDebugAndroidTest 第一次失败（模拟器偶发常见），重跑一次"
if run_tests; then exit 0; fi
dump 2
exit 1
