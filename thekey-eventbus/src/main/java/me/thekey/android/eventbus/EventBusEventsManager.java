package me.thekey.android.eventbus;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;

import me.thekey.android.eventbus.event.AttributesUpdatedEvent;
import me.thekey.android.eventbus.event.ChangeDefaultSessionEvent;
import me.thekey.android.eventbus.event.LoginEvent;
import me.thekey.android.eventbus.event.LogoutEvent;
import me.thekey.android.events.EventsManager;

public final class EventBusEventsManager implements EventsManager {
    @NonNull
    private final EventBus mEventBus;

    public EventBusEventsManager() {
        this(EventBus.getDefault());
    }

    public EventBusEventsManager(@NonNull final EventBus eventBus) {
        mEventBus = eventBus;
    }

    @Override
    public void loginEvent(@NonNull final String guid) {
        mEventBus.post(new LoginEvent(guid));
    }

    @Override
    public void logoutEvent(@NonNull final String guid, final boolean changingUser) {
        mEventBus.post(new LogoutEvent(guid, changingUser));
    }

    @Override
    public void changeDefaultSessionEvent(@NonNull final String guid) {
        mEventBus.post(new ChangeDefaultSessionEvent(guid));
    }

    @Override
    public void attributesUpdatedEvent(@NonNull final String guid) {
        mEventBus.post(new AttributesUpdatedEvent(guid));
    }
}
