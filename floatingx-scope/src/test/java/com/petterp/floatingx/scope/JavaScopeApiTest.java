package com.petterp.floatingx.scope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import com.petterp.floatingx.core.FloatingX;
import com.petterp.floatingx.core.FxControl;
import com.petterp.floatingx.core.FxState;
import com.petterp.floatingx.core.config.FxConfig;
import com.petterp.floatingx.core.config.FxContent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Java 侧只用 ViewGroupHost.of + FloatingX.create（spec §5） */
@RunWith(RobolectricTestRunner.class)
public class JavaScopeApiTest {

    @Test
    public void createWithViewGroupHost() {
        Context context = ApplicationProvider.getApplicationContext();
        FrameLayout parent = new FrameLayout(context);
        FxConfig config = FxConfig.builder(FxContent.view(new View(context))).modal(true, true).build();
        FxControl control = FloatingX.create(config, ViewGroupHost.of(parent), "java");
        control.show();
        assertSame(parent, ((ViewGroupHost) control.getHost()).getViewGroup());
        assertEquals(FxState.SHOWN, control.getState());
        control.cancel();
    }
}
