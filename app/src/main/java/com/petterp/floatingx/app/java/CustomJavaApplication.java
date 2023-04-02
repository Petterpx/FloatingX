package com.petterp.floatingx.app.java;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.petterp.floatingx.FloatingX;
import com.petterp.floatingx.app.R;
import com.petterp.floatingx.app.simple.FxAnimationImpl;
import com.petterp.floatingx.app.simple.FxConfigStorageToSpImpl;
import com.petterp.floatingx.assist.helper.AppHelper;
import com.petterp.floatingx.assist.helper.ScopeHelper;
import com.petterp.floatingx.impl.lifecycle.FxTagActivityLifecycleImpl;

/**
 * java 中的配置示例
 *
 * @author petterp
 */
public class CustomJavaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppHelper helper = AppHelper.builder()
                .setLayout(R.layout.item_floating)
                // 设置启用日志,tag可以自定义，最终显示为FloatingX-xxx
                .setEnableLog(true, "自定义的tag")

                //1. 是否允许全局显示悬浮窗,默认true
//                .setEnableAllInstall(true)
                //2. 禁止插入Activity的页面, setEnableAllBlackClass(true)时,此方法生效
//                .addInstallBlackClass(BlackActivity.class)
                //3. 允许插入Activity的页面, setEnableAllBlackClass(false)时,此方法生效
//                .addInstallWhiteClass(MainActivity.class, ScopeActivity.class)

                // 启用辅助方向
                .setEnableAssistDirection(0f, 0f, 0f, 100f)

                // 设置启用边缘吸附
                .setEnableEdgeAdsorption(true)
                // 设置边缘偏移量
                .setEdgeOffset(10f)
                // 设置启用悬浮窗可屏幕外回弹
                .setEnableScrollOutsideScreen(true)
                // 设置辅助方向辅助
                // 设置点击事件
//                .setOnClickListener()
                // 设置view-lifecycle监听
//            setViewLifecycle()
                // 设置启用动画
                .setEnableAnimation(true)
                // 设置启用动画实现
                .setAnimationImpl(new FxAnimationImpl())
                // 设置方向保存impl
                .setSaveDirectionImpl(new FxConfigStorageToSpImpl(this))

                // 设置底部偏移量
                .setBottomBorderMargin(100f)
                // 设置顶部偏移量
//            setTopBorderMargin(100f)
                // 设置左侧偏移量
                .setLeftBorderMargin(100f)
                // 设置右侧偏移量
                .setRightBorderMargin(100f)
                // 设置允许触摸事件,默认为true
                .setEnableTouch(true)
                //启用悬浮窗,即默认会插入到允许的activity中
                .setTagActivityLifecycle(new FxTagActivityLifecycleImpl() {
                    @Override
                    public void onCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
                        // 允许插入的浮窗activity执行到onCreated时会回调相应方法
                    }
                })
                // 启用悬浮窗,相当于一个标记,会自动插入允许的activity中
                .enableFx()
                .build();
        FloatingX.install(helper);
    }

    /**
     * 创建一个局部悬浮窗
     */
    public void createScopeFxSimple(Activity activity) {
        ScopeHelper.builder()
                .setLayout(R.layout.item_floating)
                .build()
                .toControl(activity);
    }
}
