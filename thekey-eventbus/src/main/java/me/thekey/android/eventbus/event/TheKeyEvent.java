package me.thekey.android.eventbus.event;

import androidx.annotation.NonNull;

public abstract class TheKeyEvent {
    @NonNull
    private final String mGuid;

    public TheKeyEvent(@NonNull final String guid) {
        mGuid = guid;
    }

    @NonNull
    public String getGuid() {
        return mGuid;
    }
}
