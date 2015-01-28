package me.thekey.android.lib.content;

import static me.thekey.android.TheKey.ACTION_ATTRIBUTES_LOADED;
import static me.thekey.android.TheKey.ACTION_LOGIN;
import static me.thekey.android.TheKey.ACTION_LOGOUT;
import static me.thekey.android.TheKey.EXTRA_GUID;
import static me.thekey.android.TheKey.EXTRA_CHANGING_USER;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

public abstract class TheKeyBroadcastReceiver extends BroadcastReceiver {
    public final void registerReceiver(final LocalBroadcastManager manager) {
        if (manager != null) {
            manager.registerReceiver(this, new IntentFilter(ACTION_LOGIN));
            manager.registerReceiver(this, new IntentFilter(ACTION_LOGOUT));
            manager.registerReceiver(this, new IntentFilter(ACTION_ATTRIBUTES_LOADED));
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
        if (ACTION_LOGIN.equals(action)) {
            this.onLogin(guid);
        } else if (ACTION_LOGOUT.equals(action)) {
            this.onLogout(guid, extras != null && extras.getBoolean(EXTRA_CHANGING_USER, false));
        } else if (ACTION_ATTRIBUTES_LOADED.equals(action)) {
            this.onAttributesLoaded(guid);
        }
    }

    protected void onLogin(final String guid) {
    }

    protected void onLogout(final String guid, final boolean changingUser) {
    }

    protected void onAttributesLoaded(final String guid) {
    }
}
