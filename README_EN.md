# FloatingX



![image-20210810161316095](https://tva1.sinaimg.cn/large/008i3skNly1gtbrg85hlhj61040k80ui02.jpg)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a9edd107b5444b7ca31738f5a96b3cb9)](https://app.codacy.com/gh/Petterpx/FloatingX?utm_source=github.com&utm_medium=referral&utm_content=Petterpx/FloatingX&utm_campaign=Badge_Grade_Settings)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petterpx/floatingx)](https://search.maven.org/search?q=g:io.github.petterpx%20AND%20a:floatingx)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)


**FloatingX** A flexible and powerful ``permission-free`` hover window solution.

> "Note: After version 1.3.4, the repository has been migrated to [Maven](https://central.sonatype.com/artifact/io.github.petterpx/floatingx). Make sure to update your dependency accordingly."

[中文简介](https://github.com/Petterpx/FloatingX/READDME.md)

[中午使用文档见这里](https://cskf7l0wab.feishu.cn/wiki/wikcnLLBCe3fIDUTAzrEg754tzc)

## 👏 Features 

- Single instance holding floating window view
- Support for various callback listeners
- Chain calls, senseless insertion
- Support customizing whether to save history position and restore
- Support inserting `ViewGroup` , `Fragment` , `Activity`
- Allow custom hover window indicators, custom hidden display animation
- Support cross-border rebound, multi-finger touch, small screen adaptation, screen rotation
- Support custom position direction, with auxiliary positioning display coordinates
- Perfect `kotlin` build extensions, and friendly compatibility with `Java`.
- Support display location [force fix], for special models (need to open separately)
- Perfect logging system, open to see different levels of Fx running process, more convenient to find problems
- ...

## 👨‍💻‍ Dependencies

### Gradle

```groovy
dependencies {
    implementation 'io.github.petterpx:floatingx:2.0.4'
}
```


## 🏄‍♀️ 效果图

| 全屏,activity,fragment,单view                                | 小屏展示                                                     | 非正常比例缩放屏幕                                           |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![效果-展示1](https://github.com/Petterpx/FloatingX/blob/main/image/fx-api-simple.gif?raw=true) | ![演示-小屏](https://github.com/Petterpx/FloatingX/blob/main/image/fx-small-gif.gif?raw=true) | ![非正常比例缩放](https://github.com/Petterpx/FloatingX/blob/main/image/fx-view-deformed-simple.gif?raw=true) |

| 屏幕旋转                                                     | 功能演示                                                     |      |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ---- |
| ![演示-旋转](https://github.com/Petterpx/FloatingX/blob/main/image/fx-rotate-simple.gif?raw=true) | ![演示-局部功能](https://github.com/Petterpx/FloatingX/blob/main/image/fx-api-simple.gif?raw=true) |      |



### Complete log-viewer

Open the log viewer, you will see the whole track of Fx, which is easier to find the problem and track the solution. Also support custom log tag。



| App                                                          | Activity                                                     | ViewGroup                                                    |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![image-20210808123000851](https://tva1.sinaimg.cn/large/008i3skNly1gwgtxtbx5aj31160s8444.jpg) | ![image-20210808123414921](https://tva1.sinaimg.cn/large/008i3skNly1gwgtxu2pkyj313o0r4jwk.jpg) | ![image-20210808123553402](https://tva1.sinaimg.cn/large/008i3skNly1gwgtxunhmwj311y0jctc8.jpg) |



## 👨‍🔧‍ Usage

### Global hover window management

**AndroidManifest**

```xml
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



### Local hover window management

#### Generic creation method

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

#### extended support for kt

##### activity create hover window

```kotlin
private val activityFx by activityToFx(activity) {
    setLayout(R.layout.item_floating)
}
```

##### fragment to create a hover window

```kotlin
private val fragment by fragmentToFx(fragment) {
    setLayout(R.layout.item_floating)
}
```

##### viewGroup creates a hover window

```kotlin
private val viewFx by createFx({
        init(viewGroup)
    }) {
        setLayout(R.layout.item_floating)
        setEnableLog(true, "main_fx")
    }
```

##### Quickly create an arbitrary scope hover window

```kotlin
private val customCreateFx by createFx {
    setLayout(R.layout.item_floating)
    build().toControl(activity)
    build().toControl(fragment)
    build().toControl(viewgroup)
```

## Thanks

Base **HoverView** sourced from [FloatingMagnetView](https://github.com/leotyndale/EnFloatingView) of [EnFloatingView](EnFloatingView/blob/master/floatingview/src/main/java/com/imuxuan/floatingview/FloatingMagnetView.java) implementation with some improvements on top of it.

For the measurement of the navigation bar part of the code from, wenlu, and on top of it added more adaptations, has covered 95% of the market models, can be said to be the only tool that can be searched for accurate measurement.

## About me

Welcome to follow my public number, look forward to progress together, if there are any problems in use, you can also add me WeChat.

Wechat: **Petterpx**

![Petterp-wechat](https://user-images.githubusercontent.com/41142188/226162520-93796619-81ca-4e61-bfff-4a5b95e4fa0b.png)
