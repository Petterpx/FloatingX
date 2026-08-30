package com.petterp.floatingx.demo.java;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.view.ViewGroup;

import com.petterp.floatingx.app.AppHost;
import com.petterp.floatingx.core.FloatingX;
import com.petterp.floatingx.core.FxControl;
import com.petterp.floatingx.core.FxListener;
import com.petterp.floatingx.core.config.FxConfig;
import com.petterp.floatingx.core.config.FxContent;
import com.petterp.floatingx.core.gesture.FxGesture;
import com.petterp.floatingx.core.layout.FxAdsorb;
import com.petterp.floatingx.core.layout.FxEdge;
import com.petterp.floatingx.core.layout.FxGravity;
import com.petterp.floatingx.core.layout.FxHalfHide;
import com.petterp.floatingx.core.storage.FxSpStorage;
import com.petterp.floatingx.demo.R;
import com.petterp.floatingx.demo.pages.BaseBlackActivity;
import com.petterp.floatingx.demo.ui.DemoContent;
import com.petterp.floatingx.scope.ViewGroupHost;
import com.petterp.floatingx.system.SystemHost;
import com.petterp.floatingx.system.permission.FxPermissionStrategy;

import java.util.EnumSet;

/**
 * Java 侧的 3.0 用法（spec §7）；每个方法对应 MainActivity「Java」分组里的一个按钮。
 *
 * <p>3.0 的公开 API 全是 Kotlin 写的，但都留了 Java 入口：
 * <ul>
 *   <li>配置用 {@link FxConfig#builder}（Kotlin 侧的 DSL 是 {@code @JvmSynthetic}，Java 看不到，只能走 Builder）；</li>
 *   <li>host 各自有 {@code builder(...)} / {@code of(...)} 静态工厂；</li>
 *   <li>{@link FxListener} 的方法都有默认实现（库开了 {@code jvmDefault=enable}），Java 匿名类只覆写用得上的；</li>
 *   <li>{@code fun interface}（AppActivityFilter / SystemLayoutParamsCustomizer）可以直接写 lambda。</li>
 * </ul>
 */
public final class JavaDemo {

    public static final String TAG_APP = "java-app";
    public static final String TAG_SYSTEM = "java-system";
    public static final String TAG_SCOPE = "java-scope";

    private JavaDemo() {
    }

    /** 三个示例共用的配置：锚点 + 留白 + 左右吸附半隐 + 长按才可拖 + 位置持久化 */
    private static FxConfig config(Application app) {
        return FxConfig.builder(FxContent.layout(R.layout.fx_card))
                // dx/dy 是从所依附的那条边「向内」的偏移：贴左下角，向右 24、向上 120
                .anchor(FxGravity.BOTTOM_START, 24f, 120f)
                .margin(16f, 16f, 16f, 16f)
                // Edges 是 data class，Java 没有默认参数，三个参数都得给
                .adsorb(new FxAdsorb.Edges(EnumSet.of(FxEdge.START, FxEdge.END), new FxHalfHide(0.3f), true))
                // 预设常量是 @JvmField，Java 直接取静态字段；要细调就 new FxGesture(...)
                .gesture(FxGesture.LongPressToDrag)
                .storage(new FxSpStorage(app))
                .enableLog("Fx-java")
                .build();
    }

    /** App 级全局浮窗：黑名单按父类命中（#221），filter 是 fun interface，可以写 lambda */
    public static FxControl installApp(Application app) {
        AppHost host = AppHost.builder(app)
                .blacklist(BaseBlackActivity.class)
                .filter(activity -> !activity.isFinishing())
                // 内容用 Application context 创建，MaterialCardView 需要 Material 主题
                .theme(R.style.Theme_FloatingX)
                .build();
        // 同 tag 已存在会先 cancel 旧的，按钮可以重复点
        FxControl control = FloatingX.install(TAG_APP, config(app), host);
        control.addListener(new FxListener() {
            @Override
            public void onClick(FxControl c, View view) {
                // DemoContent 是 Kotlin object，Java 侧通过 INSTANCE 访问
                DemoContent.INSTANCE.toast(view.getContext(), "点击了 " + c.getTag());
            }
        });
        control.show();
        return control;
    }

    /** 系统级浮窗：自动申请权限，被拒降级为 App 级浮窗 */
    public static FxControl installSystem(Application app) {
        SystemHost host = SystemHost.builder(app)
                .theme(R.style.Theme_FloatingX)
                // customizer 在默认 LayoutParams 之后执行，可覆盖任何字段
                .layoutParams(lp -> lp.alpha = 0.9f)
                .permission(FxPermissionStrategy.auto())
                .fallback(AppHost.builder(app).theme(R.style.Theme_FloatingX).build())
                .build();
        FxControl control = FloatingX.install(TAG_SYSTEM, config(app), host);
        control.show();
        return control;
    }

    /**
     * 局部浮窗：挂在传入 Activity 的 android.R.id.content 上。
     * create 出来的浮窗不进注册表，生命周期归调用方——调用方需要在页面销毁时 cancel。
     */
    public static FxControl createScope(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        FxControl control = FloatingX.create(config(activity.getApplication()), ViewGroupHost.of(content), TAG_SCOPE);
        control.show();
        return control;
    }
}
