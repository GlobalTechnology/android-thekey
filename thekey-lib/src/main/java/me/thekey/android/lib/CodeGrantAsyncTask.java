package me.thekey.android.lib;

import static me.thekey.android.lib.Constant.REDIRECT_URI;

import android.os.AsyncTask;
import android.support.v4.os.AsyncTaskCompat;

import me.thekey.android.TheKeySocketException;

public abstract class CodeGrantAsyncTask extends AsyncTask<String, Void, Boolean> {
    protected final TheKeyImpl thekey;

    public CodeGrantAsyncTask(final TheKeyImpl thekey) {
        this.thekey = thekey;
    }

    public final AsyncTask<String, Void, Boolean> execute(final String code) {
        return AsyncTaskCompat.executeParallel(this, code);
    }

    @Override
    protected final Boolean doInBackground(final String... code) {
        if (code.length > 0) {
            try {
                return this.thekey.processCodeGrant(code[0], REDIRECT_URI);
            } catch (final TheKeySocketException e) {
                return false;
            }
        } else {
            return false;
        }
    }
}
