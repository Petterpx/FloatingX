package com.petterp.floatingx.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.petterp.floatingx.core.FloatingX;
import com.petterp.floatingx.core.FxControl;
import com.petterp.floatingx.core.FxState;
import com.petterp.floatingx.core.config.FxConfig;
import com.petterp.floatingx.core.config.FxContent;
import com.petterp.floatingx.core.layout.FxGravity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** spec §7 的 Java 样例必须能编译并工作 */
@RunWith(RobolectricTestRunner.class)
public class JavaAppApiTest {

    /** 线上由 FxAppInitProvider 在进程启动时做；Robolectric 不会自动创建清单里的 provider，这里显式跑一遍 */
    @Before
    public void setUp() {
        Robolectric.setupContentProvider(FxAppInitProvider.class);
    }

    @After
    public void tearDown() {
        FloatingX.uninstallAll();
    }

    @Test
    public void builderChainAndInstall() {
        Application app = ApplicationProvider.getApplicationContext();
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().postResume().visible().get();
        FxConfig config = FxConfig.builder(FxContent.view(new View(app)))
                .anchor(FxGravity.CENTER_END, 0f, 120f)
                .modal()
                .build();
        AppHost host = AppHost.builder(app)
                .blacklist(AppHostTest.BlackActivity.class)
                .blacklist("com.example.SplashActivity")
                .whitelist(Activity.class)
                .filter(a -> !a.isFinishing())
                .attachTo(AppAttachTarget.DECOR)
                .build();
        FxControl control = FloatingX.install("java", config, host);
        control.show();
        assertEquals(FxState.SHOWN, control.getState());
        assertSame(activity, host.getAttachedActivity());
        assertSame(activity, FxAppExtKt.getAttachedActivity(control));
        assertTrue(host.accepts(activity));
        activity.finish();
    }
}
