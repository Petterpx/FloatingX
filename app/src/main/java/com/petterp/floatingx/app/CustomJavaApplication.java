package com.petterp.floatingx.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.petterp.floatingx.FloatingX;
import com.petterp.floatingx.assist.helper.AppHelper;
import com.petterp.floatingx.assist.helper.ScopeHelper;
import com.petterp.floatingx.listener.control.IFxControl;
import com.petterp.floatingx.listener.control.IFxScopeControl;
import com.petterp.floatingx.view.FxViewHolder;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

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
                .setContext(this)
                .setEnableAllBlackClass(true)
                .setEnableLog()
                .setEnableAllBlackClass(true, MainActivity.class, ScopeActivity.class)
                .show()
                .build();
        FloatingX.init(helper);
    }

    public void createScopeFxSimple() {
//        ScopeHelper.builder()
//                .setLayout(R.layout.item_floating)
//                .build()
//                .toControl(AppCompatActivity());
    }
}
