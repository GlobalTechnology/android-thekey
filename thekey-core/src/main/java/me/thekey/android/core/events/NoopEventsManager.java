package me.thekey.android.core.events;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import me.thekey.android.events.EventsManager;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

@RestrictTo(LIBRARY)
public final class NoopEventsManager implements EventsManager {
    @Override
    public void loginEvent(@NonNull final String guid) {}

    @Override
    public void logoutEvent(@NonNull final String guid, final boolean changingUser) {}

    @Override
    public void changeDefaultSessionEvent(@NonNull final String guid) {}

    @Override
    public void attributesUpdatedEvent(@NonNull final String guid) {}
}
