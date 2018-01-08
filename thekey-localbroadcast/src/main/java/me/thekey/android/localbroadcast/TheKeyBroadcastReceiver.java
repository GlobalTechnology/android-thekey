package me.thekey.android.localbroadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import static me.thekey.android.localbroadcast.Constants.ACTION_ATTRIBUTES_LOADED;
import static me.thekey.android.localbroadcast.Constants.ACTION_CHANGE_DEFAULT_SESSION;
import static me.thekey.android.localbroadcast.Constants.ACTION_LOGIN;
import static me.thekey.android.localbroadcast.Constants.ACTION_LOGOUT;
import static me.thekey.android.localbroadcast.Constants.EXTRA_CHANGING_USER;
import static me.thekey.android.localbroadcast.Constants.EXTRA_GUID;

public abstract class TheKeyBroadcastReceiver extends BroadcastReceiver {
    @Nullable
    private final String mGuid;

    protected TheKeyBroadcastReceiver() {
        this(null);
    }

    protected TheKeyBroadcastReceiver(@Nullable final String guid) {
        mGuid = guid;
    }

    public final void registerReceiver(final LocalBroadcastManager manager) {
        if (manager != null) {
            manager.registerReceiver(this, BroadcastUtils.loginFilter(mGuid));
            manager.registerReceiver(this, BroadcastUtils.logoutFilter(mGuid));
            manager.registerReceiver(this, BroadcastUtils.changeDefaultSessionFilter());
            manager.registerReceiver(this, BroadcastUtils.attributesLoadedFilter(mGuid));
        }
    }

    public final void unregisterReceiver(final LocalBroadcastManager manager) {
        if (manager != null) {
            manager.unregisterReceiver(this);
        }
    }

    @Override
    public final void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        final Bundle extras = intent.getExtras();
        final String guid = extras != null ? extras.getString(EXTRA_GUID) : null;

        if (action != null && guid != null) {
            switch (action) {
                case ACTION_LOGIN:
                    onLogin(guid);
                    break;
                case ACTION_LOGOUT:
                    onLogout(guid, extras.getBoolean(EXTRA_CHANGING_USER, false));
                    break;
                case ACTION_CHANGE_DEFAULT_SESSION:
                    onChangeDefaultSession(guid);
                    break;
                case ACTION_ATTRIBUTES_LOADED:
                    onAttributesLoaded(guid);
                    break;
                default:
                    // ignore broadcast
            }
        }
    }

    protected void onLogin(@NonNull final String guid) {
    }

    protected void onLogout(@NonNull final String guid, final boolean changingUser) {
    }

    protected void onChangeDefaultSession(@NonNull final String newGuid) {
    }

    protected void onAttributesLoaded(@NonNull final String guid) {
    }
}
