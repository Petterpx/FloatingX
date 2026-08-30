package com.petterp.floatingx.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.view.View;
import android.view.WindowManager;

import androidx.test.core.app.ApplicationProvider;

import com.petterp.floatingx.core.FloatingX;
import com.petterp.floatingx.core.FxControl;
import com.petterp.floatingx.core.FxState;
import com.petterp.floatingx.core.config.FxConfig;
import com.petterp.floatingx.core.config.FxContent;
import com.petterp.floatingx.core.layout.FxGravity;
import com.petterp.floatingx.system.permission.FxPermissionStrategy;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowSettings;

/** spec §7 的 Java 样例必须能编译并工作 */
@RunWith(RobolectricTestRunner.class)
public class JavaSystemApiTest {

    @After
    public void tearDown() {
        FloatingX.uninstallAll();
    }

    @Test
    public void builderChainAndInstall() {
        ShadowSettings.setCanDrawOverlays(true);
        Application app = ApplicationProvider.getApplicationContext();
        FxConfig config = FxConfig.builder(FxContent.view(new View(app)))
                .anchor(FxGravity.CENTER_END, 0f, 120f)
                .build();
        SystemHost host = SystemHost.builder(app)
                .layoutParams(lp -> lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                .permission(FxPermissionStrategy.auto())
                .keyboard(android.R.id.edit)
                .onBackPressed(() -> true)
                .build();
        FxControl control = FloatingX.install("java", config, host);
        control.show();
        assertEquals(FxState.SHOWN, control.getState());
        assertTrue(host.isPermissionGranted());
        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, host.getWindowLayoutParams().type);
    }
}
