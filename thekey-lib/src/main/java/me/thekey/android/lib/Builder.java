package me.thekey.android.lib;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface Builder<T> {
    @NonNull
    @Deprecated
    Builder<T> casServer(@Nullable String server);

    @NonNull
    @Deprecated
    Builder<T> clientId(long id);

    @NonNull
    Builder<T> selfService(boolean enable);

    @NonNull
    T build();

    void start();
}
