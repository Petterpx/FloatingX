# FloatingX



![image-20210810161316095](https://tva1.sinaimg.cn/large/008i3skNly1gtbrg85hlhj61040k80ui02.jpg)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a9edd107b5444b7ca31738f5a96b3cb9)](https://app.codacy.com/gh/Petterpx/FloatingX?utm_source=github.com&utm_medium=referral&utm_content=Petterpx/FloatingX&utm_campaign=Badge_Grade_Settings)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petterpx/floatingx)](https://search.maven.org/search?q=g:io.github.petterpx%20AND%20a:floatingx)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/) 

**FloatingX** 一个灵活且强大的悬浮窗解决方案。

[English Introduction](https://github.com/Petterpx/FloatingX/blob/main/README_EN.md)

[具体使用文档见这里](https://cskf7l0wab.feishu.cn/wiki/wikcnLLBCe3fIDUTAzrEg754tzc)

## 👏 特性 

- 支持 **JetPack Compose**
- 支持 **浮窗半隐藏模式**
- 支持 **自定义隐藏显示动画**;
- 支持 **多指触摸**，精准决策触摸手势;
- 支持 自定义是否保存历史位置及还原;
- 支持 **系统浮窗**、**应用内浮窗**、**局部浮窗**；
- 支持 **越界回弹**，**边缘悬停**，**边界设置**;
- 支持 以 **layout**, **View**  的方式设置浮窗内容；
- 支持 自定义浮窗显示位置，**支持辅助定位**;
- 支持 **黑名单与白名单** 功能，指定页面禁止显示浮窗等;
- 支持 `kotlin` 构建扩展, 及对 `Java` 的友好兼容;
- 支持显示位置[强行修复],应对特殊机型(需要单独开启)
- 支持 **局部浮窗**，可在`ViewGroup` , `Fragment` , `Activity` 中进行显示；
- 完善的日志系统，打开即可看到不同级别的Fx运行过程,更利于发现问题

## 👨‍💻‍ 依赖方式

### Gradle

```groovy
dependencies {
    implementation 'io.github.petterpx:floatingx:2.2.6'
  
    // system浮窗&&compose时需要导入
    // 记得AppHelper里调用 enableComposeSupport()
    implementation 'io.github.petterpx:floatingx-compose:2.2.6'
}
```


## 🏄‍♀️ 效果图

| 全屏,activity,fragment,单view                                | 小屏展示                                                     | 非正常比例缩放屏幕                                           |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![效果-展示1](https://github.com/Petterpx/FloatingX/blob/main/image/fx-api-simple.gif?raw=true) | ![演示-小屏](https://github.com/Petterpx/FloatingX/blob/main/image/fx-small-gif.gif?raw=true) | ![非正常比例缩放](https://github.com/Petterpx/FloatingX/blob/main/image/fx-view-deformed-simple.gif?raw=true) |

| 屏幕旋转                                                     | 功能演示                                                     |      |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ---- |
| ![演示-旋转](https://github.com/Petterpx/FloatingX/blob/main/image/fx-rotate-simple.gif?raw=true) | ![演示-局部功能](https://github.com/Petterpx/FloatingX/blob/main/image/fx-api-simple.gif?raw=true) |      |

### 完善的日志-查看器

开启日志查看器，将看到Fx整个运行轨迹，更便于发现问题以及追踪解决。同时支持自定义日志tag

| App                                                          | Activity                                                     | ViewGroup                                                    |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![image-20210808123000851](https://tva1.sinaimg.cn/large/008i3skNly1gtbk1ujkqfj31160s8444.jpg) | ![image-20210808123414921](https://tva1.sinaimg.cn/large/008i3skNly1gt99vralyqj313o0r4jwk.jpg) | ![image-20210808123553402](https://tva1.sinaimg.cn/large/008i3skNly1gt99xfpfwgj311y0jctc8.jpg) |



## 👨‍🔧‍ 使用方式

### 全局悬浮窗管理

**AndroidManifest (非必须)**

```xml
// 如果不使用系统浮窗可以忽略此步骤(即FxScopeType.App时可跳过)
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.SYSTEM_OVERLAY_WINDOW" />
```

**kt**

```kotlin
FloatingX.install {
	setContext(context)
        setLayout(R.layout.item_floating)
  	setScopeType(FxScopeType.SYSTEM_AUTO)
}.show()
```

**Java**

```java
AppHelper helper = AppHelper.builder()
	.setContext(context)
        .setLayout(R.layout.item_floating)
  	.setScopeType(FxScopeType.SYSTEM_AUTO)
        .build();
FloatingX.install(helper).show();
```



### 局部悬浮窗管理

#### 通用创建方式

**kt**

```kotlin
ScopeHelper.builder {
  setLayout(R.layout.item_floating)
}.toControl(activity)
```

**kt && java**

```kotlin
ScopeHelper.builder()
            .setLayout(R.layout.item_floating)
            .build()
            .toControl(activity)
            .toControl(fragment)
            .toControl(viewgroup)
```

#### 对kt的扩展支持

##### activity创建悬浮窗

```kotlin
private val scopeFx by createFx {
    setLayout(R.layout.item_floating)
    build().toControl(this/Activity)
}

```

##### fragment创建悬浮窗

```kotlin
private val activityFx by createFx {
    setLayout(R.layout.item_floating)
    build().toControl(this/Fragment)
}
```

##### viewGroup创建悬浮窗

```kotlin
private val activityFx by createFx {
    setLayout(R.layout.item_floating)
    build().toControl(this/Viewgroup)
}
```

## 🤔 技术实现
> **System** 级别悬浮窗 基于 `WindowsManager` 的实现方案，全局持有一个单独的悬浮窗 `View` ,通过 `AppLifecycle` 监听 `Activity` 生命周期，并在相应时机 插入到 `WindowManager` 上 ;

> **App** 级别悬浮窗 基于 `DecorView` 的的实现方案，全局持有一个单独的悬浮窗 `View` ,通过 `AppLifecycle` 监听 `Activity` 生命周期，并在相应时机 插入到 `DecorView` 上 ;
>
> **View** 级别悬浮窗，基于给定的 `ViewGroup` ;
>
> **Fragment** 级别，基于其对应的 `rootView` ;
>
> **Acrtivity** 级别,基于 `DecorView` 内部的 `R.id.content` ;

具体如下：

<img src="https://tva1.sinaimg.cn/large/008i3skNly1gr20ks7780j30rc0i5dim.jpg" alt="Activity-setContentView"  />

具体见我的博客：[源码分析 | Activity-setContentView](https://juejin.cn/post/6897453195342610445) 

Ps: 为什么App级别悬浮窗 要插入到 `DecorView` ,而不是 **R.id.content** -> `FrameLayout` ?

> 插入到 `DecorView` 可以最大程度控制悬浮窗的自由度，即悬浮窗可以真正意义上[`全屏`]拖动。
>
> 插入到 `content` 中,其拖动范围其实为 **应用视图范围** ,即摆放位置 受到 **状态栏** 和 **底部导航栏** 以及 默认的 `AppBar` 影响, 比如当用户隐藏了状态栏或者导航栏，相对应的视图大小会发生改变，将影响悬浮窗的位置摆放。



## 👍 感谢

基础 **悬浮窗View** 的 初版实现思想 源自 [EnFloatingView](https://github.com/leotyndale/EnFloatingView) 的 [FloatingMagnetView](https://github.com/leotyndale/EnFloatingView/blob/master/floatingview/src/main/java/com/imuxuan/floatingview/FloatingMagnetView.java) 实现方式，并在其之上进行了彻底的重构与演变。

对于导航栏的测量部分代码来自，wenlu,并在其之上增加了更多适配，已覆盖市场95%机型，可以说是目前能搜到的唯一可以准确测量的工具。

## 关于我

欢迎关注我的公众号，期待一同进步，如果有使用上的问题，也可以加我微信。

**微信**：**Petterpx**

![Petterp-wechat](https://user-images.githubusercontent.com/41142188/226162520-93796619-81ca-4e61-bfff-4a5b95e4fa0b.png)

