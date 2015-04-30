package me.thekey.android.lib;

import static me.thekey.android.lib.Constant.ARG_SELF_SERVICE;

import android.os.Bundle;
import android.support.annotation.NonNull;

public abstract class AbstractBuilder<T> implements Builder<T> {
    protected final Bundle mArgs = new Bundle();

    public AbstractBuilder() {
        // set default configuration
        this.selfService(false);
    }

    @NonNull
    @Override
    public Builder<T> selfService(final boolean enable) {
        mArgs.putBoolean(ARG_SELF_SERVICE, enable);
        return this;
    }

    @NonNull
    @Override
    public T build() {
        throw new UnsupportedOperationException("Cannot call build() on this Builder");
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException("Cannot call start() on this Builder");
    }
}
