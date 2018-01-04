package me.thekey.android.lib;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import me.thekey.android.TheKeySocketException;
import me.thekey.android.core.TheKeyImpl;

public abstract class CodeGrantAsyncTask extends AsyncTask<String, Void, String> {
    @NonNull
    protected final TheKeyImpl mTheKey;

    public CodeGrantAsyncTask(@NonNull final TheKeyImpl thekey) {
        mTheKey = thekey;
    }

    public final AsyncTask<String, Void, String> execute(final String code) {
        return executeOnExecutor(THREAD_POOL_EXECUTOR, code);
    }

    @Override
    protected final String doInBackground(final String... code) {
        if (code.length > 0) {
            try {
                return mTheKey.processCodeGrant(code[0], mTheKey.getDefaultRedirectUri());
            } catch (final TheKeySocketException e) {
                return null;
            }
        } else {
            return null;
        }
    }
}
