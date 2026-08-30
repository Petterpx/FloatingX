# FloatingX

![image-20210810161316095](https://tva1.sinaimg.cn/large/008i3skNly1gtbrg85hlhj61040k80ui02.jpg)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a9edd107b5444b7ca31738f5a96b3cb9)](https://app.codacy.com/gh/Petterpx/FloatingX?utm_source=github.com&utm_medium=referral&utm_content=Petterpx/FloatingX&utm_campaign=Badge_Grade_Settings)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petterpx/floatingx-core)](https://central.sonatype.com/artifact/io.github.petterpx/floatingx-core)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)

[English](README_EN.md) · [文档 Wiki](https://github.com/Petterpx/FloatingX/wiki) · [AI 快速查文档](https://deepwiki.com/Petterpx/FloatingX)

**FloatingX** 是一个灵活且强大的 Android 悬浮窗解决方案：App 级全局浮窗（跟随前台 Activity）、
系统级浮窗（`WindowManager`）、局部浮窗（ViewGroup / Activity / Fragment）与 Jetpack Compose 内容，
一套 API 全部覆盖。3.0 是一次彻底重写，拆成五个可按需依赖的模块，不兼容 2.x 的 API。

## ✨ 特性

- **四种宿主，一套 API**：App 级全局浮窗（跟随前台 Activity）、系统级浮窗（`WindowManager`）、局部浮窗（ViewGroup / Activity / Fragment）、Jetpack Compose 内容。
- **换页不重建**：同一个容器在 Activity 之间静默 reparent，状态、位置、动画全部保留。
- **锚点定位**：按「贴哪条边 + 偏移」定位，内容尺寸变化、屏幕旋转都不跑偏；支持 margin、边界、safeArea、越界不裁剪。
- **手势**：拖动、边缘吸附与半隐藏、指定拖动区域（dragRegion）、与子 view 滚动的冲突策略（childPriority）。
- **动画与持久化**：内置 fade / scale 显示隐藏动画，可自定义；位置按 tag + 屏幕方向持久化。
- **系统浮窗省心**：权限三策略（自动 / 手动 / 跳过），被拒可自动降级为 App 级浮窗；`LayoutParams` 可定制，支持键盘与返回键。
- **Modal 浮窗**：遮罩拦截外部触摸，可点击外部关闭。
- **任意时机调用**：状态机 + 命令队列，宿主未就绪时 `show` / `moveTo` 自动排队，就绪后按序回放。
- **页面控制**：黑白名单 / 自定义 filter 决定浮窗在哪些页面出现（按 Class 匹配，子类一起命中）。
- **Compose 原生**：每个浮窗自带 `FxComposeOwner`，`viewModel()` / `rememberSaveable` 跨页存活，`stateFlow()` / `positionFlow()` 可观察。
- **按需依赖，双语入口**：五个模块按需引入；Kotlin DSL 与 Java Builder 两套入口，`explicitApi` 保证公开面清晰。

## 依赖

```groovy
dependencies {
    implementation "io.github.petterpx:floatingx-core:3.0.0"      // 其它模块都会自动带上，只用 core 自定义 Host 时才需要单独引
    implementation "io.github.petterpx:floatingx-app:3.0.0"       // App 级全局浮窗（跟随 Activity）
    implementation "io.github.petterpx:floatingx-system:3.0.0"    // 系统级浮窗（WindowManager + 权限）
    implementation "io.github.petterpx:floatingx-scope:3.0.0"     // 局部浮窗（Activity / ViewGroup / Fragment）
    implementation "io.github.petterpx:floatingx-compose:3.0.0"   // Jetpack Compose 内容
}
```

`floatingx-core` 会被其它四个模块以 `api` 传递带上，通常不必单独声明；其余按需取用。minSdk 21（`floatingx-compose` 为 23），
`app` / `system` 模块自带清单，接入方不需要额外配置任何东西。

## 快速开始

```kotlin
val control = FloatingX.install("music") {
    layout(R.layout.fx_card)
    anchor(FxGravity.CENTER_END, dy = 120f)
    margin(top = 24f, bottom = 24f)
    adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END), halfHide = FxHalfHide(0.3f)))
    persist(FxSpStorage(app))
    enableLog("Fx-demo")
    appHost(app) {
        // 传 Class 而非类名字符串：按 isInstance 匹配，子类一起命中
        blacklist(SplashActivity::class.java)
    }
}
control.show()
```

Java 写法、系统级浮窗、局部浮窗、Compose 浮窗见
[快速开始](https://github.com/Petterpx/FloatingX/wiki/快速开始)。

## 文档

完整文档在 [**GitHub Wiki**](https://github.com/Petterpx/FloatingX/wiki)（中文标题为中文页，英文标题为英文页）：

- [快速开始](https://github.com/Petterpx/FloatingX/wiki/快速开始) — 模块与依赖、四种 host 的最小示例
- [App 级全局浮窗](https://github.com/Petterpx/FloatingX/wiki/App-级全局浮窗) — App 级全局浮窗：黑白名单 / filter、挂载点、theme
- [系统级浮窗](https://github.com/Petterpx/FloatingX/wiki/系统级浮窗) — 系统浮窗：权限策略、降级、LayoutParams、键盘、Service
- [局部浮窗](https://github.com/Petterpx/FloatingX/wiki/局部浮窗) — 局部浮窗：ViewGroup / Activity / Fragment
- [Compose 浮窗](https://github.com/Petterpx/FloatingX/wiki/Compose-浮窗) — `compose {}`、`FxComposeOwner`、`stateFlow` / `positionFlow`
- [配置项](https://github.com/Petterpx/FloatingX/wiki/配置项) — 能力一览与全部配置项
- [API 速查](https://github.com/Petterpx/FloatingX/wiki/API-速查) — `FxControl` / `FxListener` / `FloatingX` 注册表
- [常见问题](https://github.com/Petterpx/FloatingX/wiki/常见问题) · [Issue 覆盖矩阵](https://github.com/Petterpx/FloatingX/wiki/Issue-覆盖矩阵) · [架构设计](https://github.com/Petterpx/FloatingX/wiki/架构设计) · [Demo 说明](https://github.com/Petterpx/FloatingX/wiki/Demo-说明)
- 2.x 升级：[从 2.x 迁移](https://github.com/Petterpx/FloatingX/wiki/从-2.x-迁移)，仓库内同步维护一份 [`docs/MIGRATION.md`](docs/MIGRATION.md)

## 效果图

| App 级：拖动 / 吸附 / 换页跟随 | 系统级：回到桌面仍在 | 旋转：锚点保持 |
| --- | --- | --- |
| ![App 级：拖动 / 吸附 / 换页跟随](https://github.com/Petterpx/FloatingX/blob/main/image/fx-app-host.gif?raw=true) | ![系统级：回到桌面仍在](https://github.com/Petterpx/FloatingX/blob/main/image/fx-system-host.gif?raw=true) | ![旋转：锚点保持](https://github.com/Petterpx/FloatingX/blob/main/image/fx-rotate.gif?raw=true) |

| 尺寸变化：锚点不动（#187） | Compose：计数跨页存活 | 局部浮窗：ViewGroup / Fragment |
| --- | --- | --- |
| ![尺寸变化：锚点不动（#187）](https://github.com/Petterpx/FloatingX/blob/main/image/fx-resize.gif?raw=true) | ![Compose：计数跨页存活](https://github.com/Petterpx/FloatingX/blob/main/image/fx-compose.gif?raw=true) | ![局部浮窗：ViewGroup / Fragment](https://github.com/Petterpx/FloatingX/blob/main/image/fx-scope.gif?raw=true) |

## Demo

`app` 模块是完整的示例与回归工程，`./gradlew app:installDebug` 即可安装，页面清单见
[Demo 说明](https://github.com/Petterpx/FloatingX/wiki/Demo-说明)。

## 感谢

基础 **悬浮窗View** 的初版实现思想源自 [EnFloatingView](https://github.com/leotyndale/EnFloatingView) 的
[FloatingMagnetView](https://github.com/leotyndale/EnFloatingView/blob/master/floatingview/src/main/java/com/imuxuan/floatingview/FloatingMagnetView.java)，
并在其之上进行了彻底的重构与演变；导航栏测量部分代码来自 wenlu，并在其之上增加了更多机型适配。

## 关于我

欢迎关注我的公众号，期待一同进步；使用上有问题也可以加我微信 **Petterpx**。

![Petterp-wechat](https://user-images.githubusercontent.com/41142188/226162520-93796619-81ca-4e61-bfff-4a5b95e4fa0b.png)
