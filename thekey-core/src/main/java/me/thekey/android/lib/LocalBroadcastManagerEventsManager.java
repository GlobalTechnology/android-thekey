package me.thekey.android.lib;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import me.thekey.android.EventsManager;

import static me.thekey.android.TheKey.ACTION_ATTRIBUTES_LOADED;
import static me.thekey.android.TheKey.ACTION_CHANGE_DEFAULT_SESSION;
import static me.thekey.android.TheKey.ACTION_LOGIN;
import static me.thekey.android.TheKey.ACTION_LOGOUT;
import static me.thekey.android.TheKey.EXTRA_CHANGING_USER;
import static me.thekey.android.TheKey.EXTRA_GUID;
import static me.thekey.android.lib.BroadcastUtils.theKeyUri;

public class LocalBroadcastManagerEventsManager implements EventsManager {
    @NonNull
    private final LocalBroadcastManager mBroadcastManager;

    public LocalBroadcastManagerEventsManager(@NonNull final Context context) {
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void loginEvent(@NonNull final String guid) {
        mBroadcastManager.sendBroadcast(new Intent(ACTION_LOGIN, theKeyUri(guid)).putExtra(EXTRA_GUID, guid));
    }

    @Override
    public void logoutEvent(@NonNull final String guid, final boolean changingUser) {
        mBroadcastManager.sendBroadcast(new Intent(ACTION_LOGOUT, theKeyUri(guid)).putExtra(EXTRA_GUID, guid)
                                                .putExtra(EXTRA_CHANGING_USER, changingUser));
    }

    @Override
    public void changeDefaultSessionEvent(@NonNull final String guid) {
        mBroadcastManager
                .sendBroadcast(new Intent(ACTION_CHANGE_DEFAULT_SESSION, theKeyUri(guid)).putExtra(EXTRA_GUID, guid));
    }

    @Override
    public void attributesUpdatedEvent(@NonNull final String guid) {
        mBroadcastManager
                .sendBroadcast(new Intent(ACTION_ATTRIBUTES_LOADED, theKeyUri(guid)).putExtra(EXTRA_GUID, guid));
    }
}
