package me.thekey.android.lib.support.v4.content;

import java.util.Date;

import me.thekey.android.TheKey;
import me.thekey.android.TheKey.Attributes;
import me.thekey.android.TheKeySocketException;
import me.thekey.android.lib.content.TheKeyBroadcastReceiver;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.LocalBroadcastManager;

public final class AttributesLoader extends AsyncTaskLoader<Attributes> {
    private static final long DEFAULT_MAX_AGE = 24 * 60 * 60 * 1000; /* 1 day */
    private final long maxAge = DEFAULT_MAX_AGE;

    private boolean refresh = false;

    private final TheKey mTheKey;
    private final TheKeyBroadcastReceiver mReceiver = new TheKeyBroadcastReceiver() {
        @Override
        protected void onLogin(final String guid) {
            onContentChanged();
        }

        @Override
        protected void onLogout(final String guid, final boolean changingUser) {
            onContentChanged();
        }

        @Override
        protected void onAttributesLoaded(final String guid) {
            onContentChanged();
        }
    };

    public AttributesLoader(final Context context, final TheKey thekey) {
        super(context);
        mTheKey = thekey;
    }

    /* BEGIN lifecycle */

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        mReceiver.registerReceiver(LocalBroadcastManager.getInstance(getContext()));

        // deliver any cached attributes
        final Attributes attrs = mTheKey.getAttributes();
        deliverResult(attrs);

        // trigger a load if we need a refresh of the attributes
        if (takeContentChanged() || needRefresh(attrs)) {
            forceLoad();
        }
    }

    @Override
    protected void onAbandon() {
        mReceiver.unregisterReceiver(LocalBroadcastManager.getInstance(getContext()));
        super.onAbandon();
    }

    /* END lifecycle */

    @Override
    public Attributes loadInBackground() {
        Attributes attrs = mTheKey.getAttributes();

        // check to see if the current attributes are stale
        if (needRefresh(attrs)) {
            try {
                mTheKey.loadAttributes();
                attrs = mTheKey.getAttributes();
            } catch (final TheKeySocketException ignored) {
            }
        }

        // return the loaded attributes
        return attrs;
    }

    private boolean needRefresh(final Attributes attrs) {
        return this.refresh || attrs == null || !attrs.areValid()
                || attrs.getLoadedTime().before(new Date(System.currentTimeMillis() - maxAge));
    }
}
