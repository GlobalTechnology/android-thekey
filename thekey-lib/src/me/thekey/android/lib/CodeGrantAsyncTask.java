package me.thekey.android.lib;

import static me.thekey.android.lib.Constant.REDIRECT_URI;
import me.thekey.android.TheKeySocketException;
import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

public abstract class CodeGrantAsyncTask extends AsyncTask<String, Void, Boolean> {
    protected final TheKeyImpl thekey;

    public CodeGrantAsyncTask(final TheKeyImpl thekey) {
        this.thekey = thekey;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public final AsyncTask<String, Void, Boolean> execute(final String code) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return this.executeOnExecutor(THREAD_POOL_EXECUTOR, new String[] { code });
        } else {
            return this.execute(new String[] { code });
        }
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
