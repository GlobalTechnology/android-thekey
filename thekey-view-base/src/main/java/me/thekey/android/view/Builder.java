package me.thekey.android.view;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface Builder<T> {
    @NonNull
    Builder<T> redirectUri(@Nullable Uri uri);

    @NonNull
    Builder<T> selfService(boolean enable);

    @NonNull
    Builder<T> signup(boolean enable);

    @NonNull
    default T build() {
        throw new UnsupportedOperationException("Cannot call build() on this Builder");
    }

    default void start() {
        throw new UnsupportedOperationException("Cannot call start() on this Builder");
    }
}
