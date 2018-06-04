package me.thekey.android.eventbus.event;

import android.support.annotation.NonNull;

public final class LoginEvent extends TheKeyEvent {
    public LoginEvent(@NonNull final String guid) {
        super(guid);
    }
}
