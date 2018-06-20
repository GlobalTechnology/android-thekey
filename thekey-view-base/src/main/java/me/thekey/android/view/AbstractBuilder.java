package me.thekey.android.view;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static me.thekey.android.core.Constants.ARG_REDIRECT_URI;
import static me.thekey.android.core.Constants.ARG_SELF_SERVICE;
import static me.thekey.android.core.Constants.ARG_SIGNUP;

public abstract class AbstractBuilder<T> implements Builder<T> {
    protected final Bundle mArgs = new Bundle();

    protected AbstractBuilder() {
        // set default configuration
        selfService(false);
    }

    @Override
    public Builder<T> redirectUri(@Nullable final Uri uri) {
        mArgs.putParcelable(ARG_REDIRECT_URI, uri);
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
    public Builder<T> signup(final boolean enable) {
        mArgs.putBoolean(ARG_SIGNUP, enable);
        return this;
    }
}
