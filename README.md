# FloatingX

![image-20210810161316095](https://tva1.sinaimg.cn/large/008i3skNly1gtbrg85hlhj61040k80ui02.jpg)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a9edd107b5444b7ca31738f5a96b3cb9)](https://app.codacy.com/gh/Petterpx/FloatingX?utm_source=github.com&utm_medium=referral&utm_content=Petterpx/FloatingX&utm_campaign=Badge_Grade_Settings)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petterpx/floatingx-core)](https://central.sonatype.com/artifact/io.github.petterpx/floatingx-core)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
[![AtomGit Star](https://atomgit.com/petterp/Floatingx/star/badge.svg)](https://atomgit.com/petterp/Floatingx)

[English](README_EN.md) · [文档 Wiki](https://github.com/Petterpx/FloatingX/wiki) · [DeepWiki](https://deepwiki.com/Petterpx/FloatingX)

**FloatingX** 一个灵活且强大的悬浮窗解决方案。

3.0 为全新重写版本，按模块拆分、按需依赖；API 与 2.x 不兼容，升级请看 [从 2.x 迁移](https://github.com/Petterpx/FloatingX/wiki/从-2.x-迁移)。

## 👏 特性

- 支持 **JetPack Compose**，浮窗自带 `ViewModelStore` / `SavedState`，换页不丢
- 支持 **系统浮窗**、**应用内浮窗**、**局部浮窗**（`ViewGroup` / `Fragment` / `Activity`）
- 支持 换页 **不重建**，浮窗位置、状态、动画跟着 Activity 走
- 支持 **锚点定位**，旋转、内容尺寸变化不跑偏
- 支持 **边缘吸附**、**半隐藏**、**边界设置**、**越界不裁剪**
- 支持 **拖动区域**、**长按拖动**，与列表滚动 **冲突处理**
- 支持 自定义 **显示 / 隐藏动画**
- 支持 **保存历史位置** 及还原（按 tag + 屏幕方向）
- 支持 **黑名单与白名单**，指定页面禁止显示浮窗
- 支持 **Modal 浮窗**，拦截外部点击
- 支持 **系统浮窗权限** 自动 / 手动申请，被拒 **自动降级** 为应用内浮窗
- 支持 宿主未就绪时调用 `show` / `moveTo`，就绪后自动执行
- 支持 `kotlin` DSL 构建，及对 `Java` 的友好兼容（Builder）
- 完善的日志系统，`enableLog(tag)` 即可看到 Fx 运行过程

## 👨‍💻‍ 依赖方式

```groovy
dependencies {
    implementation "io.github.petterpx:floatingx-core:3.0.0"      // 其它模块会自动带上
    implementation "io.github.petterpx:floatingx-app:3.0.0"       // App 级全局浮窗（跟随 Activity）
    implementation "io.github.petterpx:floatingx-system:3.0.0"    // 系统级浮窗（WindowManager + 权限）
    implementation "io.github.petterpx:floatingx-scope:3.0.0"     // 局部浮窗（Activity / ViewGroup / Fragment）
    implementation "io.github.petterpx:floatingx-compose:3.0.0"   // Jetpack Compose 内容
}
```

只引用到的模块就行，core 会跟着带上。minSdk 21（compose 模块 23），清单里的权限和 provider 都在库里声明好了。

## 🚀 快速开始

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

## 📖 文档

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

## 🏄‍♀️ 效果图

| App 级：拖动 / 吸附 / 换页跟随 | 系统级：回到桌面仍在 | 旋转：锚点保持 |
| --- | --- | --- |
| ![App 级：拖动 / 吸附 / 换页跟随](https://github.com/Petterpx/FloatingX/blob/main/image/fx-app-host.gif?raw=true) | ![系统级：回到桌面仍在](https://github.com/Petterpx/FloatingX/blob/main/image/fx-system-host.gif?raw=true) | ![旋转：锚点保持](https://github.com/Petterpx/FloatingX/blob/main/image/fx-rotate.gif?raw=true) |

| 尺寸变化：锚点不动（#187） | Compose：计数跨页存活 | 局部浮窗：ViewGroup / Fragment |
| --- | --- | --- |
| ![尺寸变化：锚点不动（#187）](https://github.com/Petterpx/FloatingX/blob/main/image/fx-resize.gif?raw=true) | ![Compose：计数跨页存活](https://github.com/Petterpx/FloatingX/blob/main/image/fx-compose.gif?raw=true) | ![局部浮窗：ViewGroup / Fragment](https://github.com/Petterpx/FloatingX/blob/main/image/fx-scope.gif?raw=true) |

## 📱 Demo

`app` 模块是完整的示例与回归工程，`./gradlew app:installDebug` 即可安装，页面清单见
[Demo 说明](https://github.com/Petterpx/FloatingX/wiki/Demo-说明)。

## 👍 感谢

基础 **悬浮窗View** 的初版实现思想源自 [EnFloatingView](https://github.com/leotyndale/EnFloatingView) 的
[FloatingMagnetView](https://github.com/leotyndale/EnFloatingView/blob/master/floatingview/src/main/java/com/imuxuan/floatingview/FloatingMagnetView.java)，
并在其之上进行了彻底的重构与演变；导航栏测量部分代码来自 wenlu，并在其之上增加了更多机型适配。

## 关于我

欢迎关注我的公众号，期待一同进步；使用上有问题也可以加我微信 **Petterpx**。

![Petterp-wechat](https://user-images.githubusercontent.com/41142188/226162520-93796619-81ca-4e61-bfff-4a5b95e4fa0b.png)
