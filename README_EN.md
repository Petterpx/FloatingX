# FloatingX



![image-20210810161316095](https://tva1.sinaimg.cn/large/008i3skNly1gwgttgkxlnj31040k840a.jpg)

[![](https://jitpack.io/v/Petterpx/FloatingX.svg)](https://jitpack.io/#Petterpx/FloatingX) [![Scan with Detekt](https://github.com/Petterpx/FloatingX/actions/workflows/detekt-analysis.yml/badge.svg)](https://github.com/Petterpx/FloatingX/actions/workflows/detekt-analysis.yml)  [![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/) 

**FloatingX** A flexible and powerful ``permission-free`` hover window solution.

[中文简介](https://github.com/Petterpx/FloatingX/READDME.md)

[中午使用文档见这里](https://github.com/Petterpx/FloatingX/wiki)

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

### Add jitpack repository

**build.gradle**

Gradle 7.0 and below

```groovy
allprojects {
		repositories {
			// ...
			maven { url 'https://jitpack.io' }
		}
}
```

> AndroidStudio-Arctic Fox && Gradle7.0+, and have adjusted the dependency method, you may need to add to the following location.
>
> **settings.gradle**
>
> ```groovy
> dependencyResolutionManagement {
> repositories {
> 
> // ...
> maven { url 'https://jitpack.io' }
> }
> }
> ```

### Gradle

```groovy
dependencies {
	  implementation 'com.github.Petterpx:FloatingX:1.0.7'
}
```

## 🏄‍♀️ Render

| fullscreen,activity,fragment,singleview                      | small-screen display                                         | non-normal-scaled screen                                     |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![效果-展示1](https://github.com/Petterpx/FloatingX/blob/master/image/fx-api-simple.gif) | ![演示-小屏](https://github.com/Petterpx/FloatingX/blob/master/image/fx-small-gif.gif) | ![非正常比例缩放](https://github.com/Petterpx/FloatingX/blob/master/image/fx-view-deformed-simple.gif) |

| Screen Rotation                                              | Function Demo                                                |      |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ---- |
| ![演示-旋转](https://github.com/Petterpx/FloatingX/blob/master/image/fx-rotate-simple.gif) | ![演示-局部功能](https://github.com/Petterpx/FloatingX/blob/master/image/fx-api-simple.gif) |      |

### Complete log-viewer

Open the log viewer, you will see the whole track of Fx, which is easier to find the problem and track the solution. Also support custom log tag。



| App                                                          | Activity                                                     | ViewGroup                                                    |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![image-20210808123000851](https://tva1.sinaimg.cn/large/008i3skNly1gwgtxtbx5aj31160s8444.jpg) | ![image-20210808123414921](https://tva1.sinaimg.cn/large/008i3skNly1gwgtxu2pkyj313o0r4jwk.jpg) | ![image-20210808123553402](https://tva1.sinaimg.cn/large/008i3skNly1gwgtxunhmwj311y0jctc8.jpg) |



## 👨‍🔧‍ Usage

### Global hover window management

**kt**

```kotlin
FloatingX.init {
        setContext(this@CustomApplication)
        setLayout(R.layout.item_floating_new)
  			addBlackClass(
                MainActivity::class.java,
                NewActivity::class.java,
                ImmersedActivity::class.java
         )
  			//only if show is called, it will listen to the app-lifecycle, and then it will be inserted into the activity automatically
        show()
}
````

**Java**

``` java
AppHelper helper = AppHelper.builder()
        .setContext(application)
        .setLayout(R.layout.item_floating)
        .build();
FloatingX.init(helper);
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

## 👍 Thanks

Base **HoverView** sourced from [FloatingMagnetView](https://github.com/leotyndale/EnFloatingView) of [EnFloatingView](EnFloatingView/blob/master/floatingview/src/main/java/com/imuxuan/floatingview/FloatingMagnetView.java) implementation with some improvements on top of it.

For the measurement of the navigation bar part of the code from, wenlu, and on top of it added more adaptations, has covered 95% of the market models, can be said to be the only tool that can be searched for accurate measurement.

