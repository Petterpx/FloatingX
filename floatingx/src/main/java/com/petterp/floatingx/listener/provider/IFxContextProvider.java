package com.petterp.floatingx.listener.provider;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Fx-Context提供者,用于构建合适的浮窗View
 * 提供 kotlin&&java 一致的调用体验
 *
 * @author petterp
 */
public interface IFxContextProvider {
    @NonNull
    View build(@NonNull Context context);
}
