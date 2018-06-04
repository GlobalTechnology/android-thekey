package me.thekey.android.eventbus.event;

import android.support.annotation.NonNull;

public final class LogoutEvent extends TheKeyEvent {
    private final boolean mChangingUser;

    public LogoutEvent(@NonNull final String guid, final boolean changingUser) {
        super(guid);
        mChangingUser = changingUser;
    }
}
