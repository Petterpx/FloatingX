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
import com.petterp.floatingx.core.layout.FxAnchor;
import com.petterp.floatingx.core.layout.FxBounds;
import com.petterp.floatingx.core.layout.FxGravity;
import com.petterp.floatingx.core.layout.FxHalfHide;
import com.petterp.floatingx.core.layout.FxInsets;
import com.petterp.floatingx.core.layout.FxLayoutInput;
import com.petterp.floatingx.core.layout.FxMargin;
import com.petterp.floatingx.core.layout.FxOverflow;
import com.petterp.floatingx.core.layout.FxRect;
import com.petterp.floatingx.core.layout.FxSize;

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
        FxControl control = FloatingX.install("java", config, new TestHost(parent, true, FxInsets.NONE));
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

    /** 几何/锚点类型带默认值，Java 侧必须能只传必需参数（@JvmOverloads） */
    @Test
    public void geometryTypesHaveJavaFriendlyOverloads() {
        FxAnchor anchor = new FxAnchor(FxGravity.CENTER);
        FxMargin margin = new FxMargin(1f);
        FxInsets insets = new FxInsets();
        FxOverflow overflow = new FxOverflow(true);
        FxBounds bounds = new FxBounds(new FxRect(0f, 0f, 100f, 200f));
        FxLayoutInput input = new FxLayoutInput(bounds, new FxSize(10f, 20f));

        assertEquals(0f, anchor.getDx(), 0f);
        assertEquals(0f, margin.getBottom(), 0f);
        assertEquals(0f, insets.getTop(), 0f);
        assertEquals(false, overflow.getBottom());
        assertEquals(FxInsets.NONE, bounds.getInsets());
        assertEquals(true, input.getSafeArea());

        // FxLogger.e 的单参重载（接口默认参数对 Java 不可见）
        FxLogger logger = new FxLogcatLogger("java");
        logger.e("only message");
        logger.e("message with error", new IllegalStateException("boom"));
    }

    /** create 的 tag 重载：不传 tag = 不持久化 */
    @Test
    public void createAcceptsAnOptionalTag() {
        FxConfig config = FxConfig.builder(FxContent.view(new View(context))).build();
        FrameLayout parent = new FrameLayout(context);
        FxControl anonymous = FloatingX.create(config, new TestHost(parent, true, FxInsets.NONE));
        assertEquals("", anonymous.getTag());
        anonymous.cancel();

        FxConfig other = FxConfig.builder(FxContent.view(new View(context))).build();
        FxControl tagged = FloatingX.create(other, new TestHost(new FrameLayout(context), true, FxInsets.NONE), "local");
        assertEquals("local", tagged.getTag());
        tagged.cancel();
    }
}
