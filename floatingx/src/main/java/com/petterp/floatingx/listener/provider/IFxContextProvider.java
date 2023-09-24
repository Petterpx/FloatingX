package com.petterp.floatingx.listener.provider;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Fx-Context提供者,用于构建合适的浮窗View
 * 提供 kotlin&&java 一致的调用体验
 *
 * @author petterp
 */
public interface IFxContextProvider {

    /**
     * 用于获取合适的context,从而构建合适位置的view
     *
     * @param context 注意该context在非全局浮窗时可能为null,建议在调用时注意做好check
     */
    @NonNull
    View build(@Nullable Context context);
}
