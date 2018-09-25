package me.thekey.android.eventbus.event;

import androidx.annotation.NonNull;

public final class ChangeDefaultSessionEvent extends TheKeyEvent {
    public ChangeDefaultSessionEvent(@NonNull final String guid) {
        super(guid);
    }
}
