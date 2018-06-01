package me.thekey.android.view;

import android.os.Bundle;
import android.support.annotation.NonNull;

import static me.thekey.android.core.Constants.ARG_SELF_SERVICE;

public abstract class AbstractBuilder<T> implements Builder<T> {
    protected final Bundle mArgs = new Bundle();

    protected AbstractBuilder() {
        // set default configuration
        selfService(false);
    }

    @NonNull
    @Override
    public Builder<T> selfService(final boolean enable) {
        mArgs.putBoolean(ARG_SELF_SERVICE, enable);
        return this;
    }
}
