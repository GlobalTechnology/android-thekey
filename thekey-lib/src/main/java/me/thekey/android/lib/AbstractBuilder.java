package me.thekey.android.lib;

import static me.thekey.android.TheKey.INVALID_CLIENT_ID;
import static me.thekey.android.lib.Constant.ARG_ACCOUNT_TYPE;
import static me.thekey.android.lib.Constant.ARG_CAS_SERVER;
import static me.thekey.android.lib.Constant.ARG_CLIENT_ID;
import static me.thekey.android.lib.Constant.ARG_SELF_SERVICE;
import static me.thekey.android.lib.Constant.CAS_SERVER;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class AbstractBuilder<T> implements Builder<T> {
    protected final Bundle mArgs = new Bundle();

    public AbstractBuilder() {
        // set default configuration
        this.casServer(CAS_SERVER.toString());
        this.clientId(INVALID_CLIENT_ID);
        this.selfService(false);
    }

    @NonNull
    @Override
    public final Builder<T> accountType(@Nullable final String type) {
        mArgs.putString(ARG_ACCOUNT_TYPE, type);
        return this;
    }

    @NonNull
    @Override
    public final Builder<T> casServer(@Nullable final String server) {
        mArgs.putString(ARG_CAS_SERVER, server);
        return this;
    }

    @NonNull
    @Override
    public final Builder<T> clientId(final long id) {
        mArgs.putLong(ARG_CLIENT_ID, id);
        return this;
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
