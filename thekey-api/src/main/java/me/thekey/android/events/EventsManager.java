package me.thekey.android.events;

import androidx.annotation.NonNull;
import me.thekey.android.TheKeyService;

public interface EventsManager extends TheKeyService {
    default void loginEvent(@NonNull String guid) {}

    default void logoutEvent(@NonNull String guid, boolean changingUser) {}

    default void changeDefaultSessionEvent(@NonNull String guid) {}

    default void attributesUpdatedEvent(@NonNull String guid) {}
}
