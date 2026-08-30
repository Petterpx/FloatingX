package com.petterp.floatingx.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import com.petterp.floatingx.core.config.FxConfig;
import com.petterp.floatingx.core.config.FxContent;
import com.petterp.floatingx.core.gesture.FxGesture;
import com.petterp.floatingx.core.layout.FxAdsorb;
import com.petterp.floatingx.core.layout.FxGravity;
import com.petterp.floatingx.core.layout.FxHalfHide;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** 保证公开 API 对 Java 友好（spec §7）；只覆写一个方法的匿名 FxListener 依赖 jvmDefault=enable */
@RunWith(RobolectricTestRunner.class)
public class JavaApiTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @After
    public void tearDown() {
        FloatingX.uninstallAll();
    }

    @Test
    public void buildersAndListenersCompileFromJava() {
        FxConfig config = FxConfig.builder(FxContent.view(new View(context)))
                .anchor(FxGravity.BOTTOM_END, 0f, 16f)
                .gesture(FxGesture.LongPressToDrag)
                .adsorb(FxAdsorb.horizontal(new FxHalfHide(0.3f)))
                .enableLog("java")
                .build();

        final int[] shown = {0};
        FxListener listener = new FxListener() {
            @Override
            public void onShow(FxControl control) {
                shown[0]++;
            }
        };

        FrameLayout parent = new FrameLayout(context);
        FxControl control = FloatingX.install("java", config, new TestHost(parent, true, com.petterp.floatingx.core.layout.FxInsets.NONE));
        control.addListener(listener);
        control.show();
        control.moveTo(10f, 10f);
        control.updateContent(holder -> {
            assertNotNull(holder.getView());
            return kotlin.Unit.INSTANCE;
        });

        assertEquals(FxState.SHOWN, control.getState());
        assertEquals(1, shown[0]);
        assertEquals("java", FloatingX.control("java").getTag());
    }
}
