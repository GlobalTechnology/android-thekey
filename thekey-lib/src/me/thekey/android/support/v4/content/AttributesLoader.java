package me.thekey.android.support.v4.content;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.thekey.android.TheKey;
import me.thekey.android.TheKey.Attributes;
import me.thekey.android.TheKeySocketException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.LocalBroadcastManager;

public final class AttributesLoader extends AsyncTaskLoader<Attributes> {
    private static final long DEFAULT_MAX_AGE = 24 * 60 * 60 * 1000; /* 1 day */
    private final long maxAge = DEFAULT_MAX_AGE;

    private boolean refresh = false;

    private final TheKey mTheKey;
    private final LocalBroadcastManager mBroadcastManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            onContentChanged();
        }
    };

    private final List<IntentFilter> mFilters = new ArrayList<IntentFilter>();

    public AttributesLoader(final Context context, final TheKey thekey) {
        super(context);
        mTheKey = thekey;
        mBroadcastManager = LocalBroadcastManager.getInstance(getContext());

        mFilters.add(new IntentFilter(TheKey.ACTION_ATTRIBUTES_LOADED));
        mFilters.add(new IntentFilter(TheKey.ACTION_LOGIN));
        mFilters.add(new IntentFilter(TheKey.ACTION_LOGOUT));
    }

    /* BEGIN lifecycle */

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        for (final IntentFilter filter : mFilters) {
            mBroadcastManager.registerReceiver(mReceiver, filter);
        }

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
        mBroadcastManager.unregisterReceiver(mReceiver);
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
