package me.thekey.android.eventbus.event;

import androidx.annotation.NonNull;

public final class LoginEvent extends TheKeyEvent {
    public LoginEvent(@NonNull final String guid) {
        super(guid);
    }
}
