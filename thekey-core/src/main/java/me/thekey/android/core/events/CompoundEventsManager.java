package me.thekey.android.core.events;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import me.thekey.android.TheKey;
import me.thekey.android.events.EventsManager;

public final class CompoundEventsManager implements EventsManager {
    private final Set<EventsManager> mEventsManagers = new LinkedHashSet<>();

    public CompoundEventsManager(@NonNull final EventsManager... managers) {
        Collections.addAll(mEventsManagers, managers);
    }

    public void addEventsManager(@NonNull final EventsManager manager) {
        mEventsManagers.add(manager);
    }

    public void removeEventsManager(@NonNull final EventsManager manager) {
        mEventsManagers.remove(manager);
    }

    @Override
    public void init(@NonNull final TheKey thekey) { }

    @Override
    public void loginEvent(@NonNull final String guid) {
        for (final EventsManager eventsManager : mEventsManagers) {
            eventsManager.loginEvent(guid);
        }
    }

    @Override
    public void logoutEvent(@NonNull final String guid, final boolean changingUser) {
        for (final EventsManager eventsManager : mEventsManagers) {
            eventsManager.logoutEvent(guid, changingUser);
        }
    }

    @Override
    public void changeDefaultSessionEvent(@NonNull final String guid) {
        for (final EventsManager eventsManager : mEventsManagers) {
            eventsManager.changeDefaultSessionEvent(guid);
        }
    }

    @Override
    public void attributesUpdatedEvent(@NonNull final String guid) {
        for (final EventsManager eventsManager : mEventsManagers) {
            eventsManager.attributesUpdatedEvent(guid);
        }
    }
}
