package me.thekey.android.lib;

import android.support.annotation.NonNull;

public interface Builder<T> {
    @NonNull
    Builder<T> selfService(boolean enable);

    @NonNull
    T build();

    void start();
}
