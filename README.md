# FloatingX



[![](https://jitpack.io/v/Petterpx/FloatingX.svg)](https://jitpack.io/#Petterpx/FloatingX) [![Scan with Detekt](https://github.com/Petterpx/FloatingX/actions/workflows/detekt-analysis.yml/badge.svg)](https://github.com/Petterpx/FloatingX/actions/workflows/detekt-analysis.yml) [![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/) 

**FloatingX** 一个灵活的 `免权限` 悬浮窗解决方案。

## 👏 特性 

- 链式调用，无感知插入
- 支持滑动，自动吸附，兼容多指触摸
- Kotlin-dsl
- 自动修复显示位置
- 支持对悬浮窗生命周期的监听
- 支持自定义悬浮窗各项配置
- 支持保存及恢复历史坐标位置
- 支持自定义拖动边框，吸附边框
- 支持App 内全局悬浮窗，单 `Activity` 悬浮窗



## 👨‍🔧‍ Loading 

**1.x Todo**

- 支持拖动到指定方向显示回收悬浮窗设计
- 初始化方式简化
- 多个悬浮窗时的自动规避覆盖
- 自定义显示隐藏动画效果
- 单个 `ViewGroup` 悬浮窗

**2.x Todo**

- 悬浮窗方案自动选择
  - 全局悬浮窗(需要申请权限)
  - 单App 悬浮窗
  - 单个 `Activity` 悬浮窗
  - 单个 `ViewGroup` 悬浮窗



## 👨‍💻‍ 使用方式

#### Gradle

```groovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

```groovy
dependencies {
	        implementation 'com.github.Petterpx:FloatingX:1.0-beta17'
}
```



#### 初始化-Application

**Kotlin**

```kotlin
FloatingX.init {
    context(this)
    marginEdge(10f)
    addBlackClass(MainActivity::class.java)
    layout(R.layout.item_floating)
    defaultDirection(Direction.RIGHT_OR_BOTTOM)
}
```

**Java**

```java
FxHelper config = FxHelper.builder()
    .context(this)
    .marginEdge(10f)
    .layout(R.layout.item_floating)
    .addBlackClass(MainActivity.class)
    .build();
FloatingX.init(config);
```

#### 控制器

```kotlin
 FloatingX.show(Activity?)
 FloatingX.hide()
 FloatingX.dismiss()
 FloatingX.cancel()
 FloatingX.clearConfig()
```

**更多控制**

```kotlin
FloatingX.control() - >

    @MainThread
    fun show(activity: Activity)

    /** 安装在指定activity上 */
    fun attach(activity: Activity)

    /** 安装在指定FrameLayout上 */
    fun attach(container: FrameLayout)

    /** 从指定activity上删除 */
    fun detach(activity: Activity)

    /** 从指定FrameLayout上删除 */
    fun detach(container: FrameLayout)
    
    @MainThread
    fun show()

    @MainThread
    fun hide()

    /** 关闭 */
    @MainThread
    fun dismiss()

    /** 获取自定义的view */
    fun getView(): View?

    /** 当前是否显示 */
    fun isShowRunning(): Boolean

    /** 更新params */
    @MainThread
    fun updateParams(params: ViewGroup.LayoutParams)

    /** 提供一个回调入口,用于快捷刷新 */
    @MainThread
    fun updateView(obj: (FxViewHolder) -> Unit)

    /** 更新当前view */
    @MainThread
    fun updateView(@LayoutRes resource: Int)

    fun setClickListener(obj: (View) -> Unit)
```



#### 进阶使用

wiki **loading**

---



## 🚴‍♀️ 业务决定方案

对于悬浮窗方案，我们常见有两种实现方式：

- 前者是基于 `WindowManager`，从而不依赖 `Activity` ,实现添加 `View` ，达到悬浮窗的目的。
- 后者基于 `Activity-DecorView` 动态添加移除 `view` ，从而达到悬浮窗目的。

#### 🖐 综合对比：

|      实现方案      | app内显示 | app外显示 | 头部app方案 | 兼容性  | 显示效果 | 权限请求 |
| :----------------: | :-------: | :-------: | :---------: | :-----: | :------: | :------: |
|   WindowManager    |     ✅     |     ✅     |      ✅      |    ✅    | ✅  ✅  ✅  | ✅ (8.0+) |
| Activity-DecorView |     ✅     |     ❎     |      ✅      | ✅  ✅  ✅ |    ✅     |    ❎     |

> ✅  代表支持 、 ✅  ✅ 效果好 、 ✅  ✅  ✅ 效果极佳
>
> ❎  代表 不需要或者不支持。



如果app需要 **app外显示悬浮窗** ，及是 **游戏sdk** 相关，以及需要支持 **语音通话** 这种,那么 `WMS` 这种方案是最好。

如果app **不需要app外显示** ,不想申请权限，对显示效果可接受(接受 `Activity` 切换时悬浮窗的闪动，当然也有补救措施)，那么后者方案适合你。

头部app采用悬浮窗的：

**微信**，QQ，采用的基本都是 `WMS` 的方案(很大程度是因为音视频)，**知乎** 采用的是免权限。



---

## 🐬 技术实现

> 基于 `DecorView` 的的实现方案，全局持有一个单独的悬浮窗 `View` ,通过 `AppLifecycle` 监听 `Activity` 生命周期，并在相应时机 插入到 `DecorView` 上 。

具体如下：

<img src="https://tva1.sinaimg.cn/large/008i3skNly1gr20ks7780j30rc0i5dim.jpg" alt="Activity-setContentView"  />

具体参见我的博客：[源码分析 | Activity-setContentView](https://juejin.cn/post/6897453195342610445) 

Ps: 为什么要插入到 `DecorView` ,而不是 **R.id.content** -> `FrameLayout` ?

> 插入到 `DecorView` 可以最大程度控制悬浮窗的自由度，即悬浮窗可以真正意义上[`全屏`]拖动。
>
> 插入到 `content` 中,其拖动范围其实为 **应用视图范围** ,即摆放位置 受到 **状态栏** 和 **底部导航栏** 以及 默认的 `AppBar` 影响, 比如当用户隐藏了状态栏或者导航栏，相对应的视图大小会发生改变，将影响悬浮窗的位置摆放。

## 👍 感谢

基础 **悬浮窗View** 源自 [EnFloatingView](https://github.com/leotyndale/EnFloatingView) 的 [FloatingMagnetView](https://github.com/leotyndale/EnFloatingView/blob/master/floatingview/src/main/java/com/imuxuan/floatingview/FloatingMagnetView.java) 实现方式，并在其基础上增加了一些改进！

