package org.ccci.gto.android.thekey;

import static org.ccci.gto.android.thekey.Constant.REDIRECT_URI;
import android.os.AsyncTask;

public abstract class CodeGrantAsyncTask extends AsyncTask<String, Void, Boolean> {
    protected final TheKey thekey;

    public CodeGrantAsyncTask(final TheKey thekey) {
        this.thekey = thekey;
    }

    @Override
    protected Boolean doInBackground(final String... code) {
        if (code.length > 0) {
            return this.thekey.processCodeGrant(code[0], REDIRECT_URI);
        } else {
            return false;
        }
    }
}
