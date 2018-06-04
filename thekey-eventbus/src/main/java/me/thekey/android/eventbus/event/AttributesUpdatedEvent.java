package me.thekey.android.eventbus.event;

import android.support.annotation.NonNull;

public final class AttributesUpdatedEvent extends TheKeyEvent {
    public AttributesUpdatedEvent(@NonNull final String guid) {
        super(guid);
    }
}
