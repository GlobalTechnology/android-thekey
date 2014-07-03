package me.thekey.android.lib.util;

import static me.thekey.android.TheKey.ACTION_ATTRIBUTES_LOADED;
import static me.thekey.android.TheKey.ACTION_LOGIN;
import static me.thekey.android.TheKey.ACTION_LOGOUT;
import static me.thekey.android.TheKey.EXTRA_CHANGING_USER;
import static me.thekey.android.TheKey.EXTRA_GUID;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public final class BroadcastUtils {
    public static void broadcastLogin(final Context context, final String guid) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_LOGIN).putExtra(EXTRA_GUID, guid));
    }

    public static void broadcastLogout(final Context context, final String guid, final boolean changingUser) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(
                new Intent(ACTION_LOGOUT).putExtra(EXTRA_GUID, guid).putExtra(EXTRA_CHANGING_USER, changingUser));
    }

    public static void broadcastAttributesLoaded(final Context context, final String guid) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(
                new Intent(ACTION_ATTRIBUTES_LOADED).putExtra(EXTRA_GUID, guid));
    }
}
