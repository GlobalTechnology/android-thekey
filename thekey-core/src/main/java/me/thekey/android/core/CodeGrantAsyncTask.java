package me.thekey.android.core;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import me.thekey.android.TheKey;
import me.thekey.android.exception.TheKeyApiError;
import me.thekey.android.exception.TheKeySocketException;

public abstract class CodeGrantAsyncTask extends AsyncTask<Void, Void, String> {
    @NonNull
    protected final TheKey mTheKey;

    @NonNull
    private final Uri mRedirectUri;
    @NonNull
    private final String mCode;
    @Nullable
    private final String mState;

    protected CodeGrantAsyncTask(@NonNull final TheKey thekey, @NonNull final String code,
                                 @Nullable final String state) {
        this(thekey, null, code, state);
    }

    protected CodeGrantAsyncTask(@NonNull final TheKey thekey, @Nullable final Uri redirectUri,
                                 @NonNull final String code, @Nullable final String state) {
        mTheKey = thekey;
        mRedirectUri = redirectUri != null ? redirectUri : mTheKey.getDefaultRedirectUri();
        mCode = code;
        mState = state;
    }

    public final AsyncTask<Void, Void, String> execute() {
        return executeOnExecutor(THREAD_POOL_EXECUTOR);
    }

    @Override
    protected final String doInBackground(final Void... params) {
        try {
            return mTheKey.processCodeGrant(mCode, mRedirectUri, mState);
        } catch (final TheKeyApiError | TheKeySocketException e) {
            return null;
        }
    }
}
