package me.thekey.android.core;

import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.thekey.android.TheKey;
import me.thekey.android.exception.TheKeyApiError;
import me.thekey.android.exception.TheKeySocketException;
import timber.log.Timber;

import static me.thekey.android.TheKey.PARAM_CODE;
import static me.thekey.android.TheKey.PARAM_STATE;

public class CodeGrantAsyncTask extends AsyncTask<Void, Void, String> {
    @NonNull
    private final TheKey mTheKey;

    @NonNull
    private final Uri mRedirectUri;
    @Nullable
    private final String mCode;
    @Nullable
    private final String mState;

    public CodeGrantAsyncTask(@NonNull final TheKey thekey, @NonNull final Uri dataUri) {
        mTheKey = thekey;
        mRedirectUri = DataUriUtils.removeCodeAndState(dataUri);
        mCode = dataUri.getQueryParameter(PARAM_CODE);
        mState = dataUri.getQueryParameter(PARAM_STATE);
    }

    public CodeGrantAsyncTask(@NonNull final TheKey thekey, @NonNull final String code, @Nullable final String state) {
        this(thekey, null, code, state);
    }

    public CodeGrantAsyncTask(@NonNull final TheKey thekey, @Nullable final Uri redirectUri, @NonNull final String code,
                              @Nullable final String state) {
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
        if (mCode != null) {
            try {
                return mTheKey.processCodeGrant(mCode, mRedirectUri, mState);
            } catch (final TheKeyApiError | TheKeySocketException e) {
                Timber.tag("CodeGrantAsyncTask")
                        .d(e, "error processing the code grant");
            }
        }
        return null;
    }
}
