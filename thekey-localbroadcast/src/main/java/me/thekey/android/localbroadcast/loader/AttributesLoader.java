package me.thekey.android.localbroadcast.loader;

import android.content.Context;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.loader.content.AsyncTaskLoader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import me.thekey.android.Attributes;
import me.thekey.android.TheKey;
import me.thekey.android.exception.TheKeySocketException;
import me.thekey.android.localbroadcast.TheKeyBroadcastReceiver;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

public final class AttributesLoader extends AsyncTaskLoader<Attributes> {
    private static final long DEFAULT_MAX_AGE = 24 * 60 * 60 * 1000; /* 1 day */

    @NonNull
    private final TheKey mTheKey;
    @NonNull
    private final TheKeyBroadcastReceiver mReceiver;
    @Nullable
    @RestrictTo(LIBRARY)
    final String mGuid;
    private final long mMaxAge = DEFAULT_MAX_AGE;
    private boolean mRefresh = false;

    public AttributesLoader(@NonNull final Context context, @NonNull final TheKey thekey, @Nullable String guid) {
        super(context);
        mTheKey = thekey;
        mReceiver = new MyTheKeyBroadcastReceiver(guid);
        mGuid = guid;
    }

    /* BEGIN lifecycle */

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        mReceiver.registerReceiver(LocalBroadcastManager.getInstance(getContext()));

        // deliver any cached attributes
        final Attributes attrs = mTheKey.getCachedAttributes(getGuid());
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
        final String guid = getGuid();
        Attributes attrs = mTheKey.getCachedAttributes(guid);

        // check to see if the current attributes are stale
        if (needRefresh(attrs)) {
            try {
                mTheKey.loadAttributes(guid);
                attrs = mTheKey.getCachedAttributes(guid);
            } catch (final TheKeySocketException ignored) {
            }
        }

        // return the loaded attributes
        return attrs;
    }

    private boolean needRefresh(@NonNull final Attributes attrs) {
        return mRefresh || !attrs.areValid() ||
                attrs.getLoadedTime().before(new Date(System.currentTimeMillis() - mMaxAge));
    }

    @Nullable
    private String getGuid() {
        return mGuid != null ? mGuid : mTheKey.getDefaultSessionGuid();
    }

    private final class MyTheKeyBroadcastReceiver extends TheKeyBroadcastReceiver {
        MyTheKeyBroadcastReceiver(@Nullable final String guid) {
            super(guid);
        }

        @Override
        protected void onLogin(@NonNull final String guid) {
            onContentChanged();
        }

        @Override
        protected void onLogout(@NonNull final String guid, final boolean changingUser) {
            onContentChanged();
        }

        @Override
        protected void onChangeDefaultSession(@NonNull final String newGuid) {
            if (mGuid == null) {
                onContentChanged();
            }
        }

        @Override
        protected void onAttributesLoaded(@NonNull final String guid) {
            onContentChanged();
        }
    }
}
