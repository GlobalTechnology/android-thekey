package me.thekey.android.eventbus.event;

import android.support.annotation.NonNull;

public final class ChangeDefaultSessionEvent extends TheKeyEvent {
    public ChangeDefaultSessionEvent(@NonNull final String guid) {
        super(guid);
    }
}
