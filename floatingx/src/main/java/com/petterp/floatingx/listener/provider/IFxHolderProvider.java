package com.petterp.floatingx.listener.provider;

import androidx.annotation.NonNull;

import com.petterp.floatingx.view.FxViewHolder;

/**
 * 提供Holder供外部使用
 * 提供 kotlin&&java 一致的调用体验
 *
 * @author petterp
 */
public interface IFxHolderProvider {
    void apply(@NonNull FxViewHolder holder);
}