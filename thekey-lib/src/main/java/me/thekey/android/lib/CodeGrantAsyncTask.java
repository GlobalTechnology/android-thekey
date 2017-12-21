package me.thekey.android.lib;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import me.thekey.android.TheKeySocketException;

public abstract class CodeGrantAsyncTask extends AsyncTask<String, Void, Boolean> {
    @NonNull
    protected final TheKeyImpl mTheKey;

    public CodeGrantAsyncTask(@NonNull final TheKeyImpl thekey) {
        mTheKey = thekey;
    }

    public final AsyncTask<String, Void, Boolean> execute(final String code) {
        return executeOnExecutor(THREAD_POOL_EXECUTOR, code);
    }

    @Override
    protected final Boolean doInBackground(final String... code) {
        if (code.length > 0) {
            try {
                return mTheKey.processCodeGrant(code[0], mTheKey.getRedirectUri());
            } catch (final TheKeySocketException e) {
                return false;
            }
        } else {
            return false;
        }
    }
}
