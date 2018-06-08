package me.thekey.android.core.events;

import android.support.annotation.NonNull;

import me.thekey.android.events.EventsManager;

public final class NoopEventsManager implements EventsManager {
    public static final NoopEventsManager INSTANCE = new NoopEventsManager();

    private NoopEventsManager() { }

    @Override
    public void loginEvent(@NonNull final String guid) {}

    @Override
    public void logoutEvent(@NonNull final String guid, final boolean changingUser) {}

    @Override
    public void changeDefaultSessionEvent(@NonNull final String guid) {}

    @Override
    public void attributesUpdatedEvent(@NonNull final String guid) {}
}
