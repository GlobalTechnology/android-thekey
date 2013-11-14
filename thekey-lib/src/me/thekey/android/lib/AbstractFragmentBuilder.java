package me.thekey.android.lib;

import android.os.Bundle;

public abstract class AbstractFragmentBuilder<T> extends AbstractBuilder<T> {
    protected final Bundle mArgs = new Bundle();

    @Override
    protected final Builder<T> setLong(final String key, final long value) {
        mArgs.putLong(key, value);
        return this;
    }

    @Override
    protected final Builder<T> setString(final String key, final String value) {
        mArgs.putString(key, value);
        return this;
    }

    @Override
    public final void start() {
        throw new UnsupportedOperationException("Cannot start() a Fragment");
    }
}
