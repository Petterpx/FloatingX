package com.petterp.floatingx.app.java;

import android.app.Application;

import com.petterp.floatingx.FloatingX;
import com.petterp.floatingx.app.MainActivity;
import com.petterp.floatingx.app.R;
import com.petterp.floatingx.app.ScopeActivity;
import com.petterp.floatingx.assist.helper.AppHelper;

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
