package me.thekey.android.view;

import android.support.annotation.NonNull;

public interface Builder<T> {
    @NonNull
    Builder<T> selfService(boolean enable);

    @NonNull
    default T build() {
        throw new UnsupportedOperationException("Cannot call build() on this Builder");
    }

    default void start() {
        throw new UnsupportedOperationException("Cannot call start() on this Builder");
    }
}
