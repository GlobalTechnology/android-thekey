package me.thekey.android.events;

import androidx.annotation.NonNull;
import me.thekey.android.TheKeyService;

public interface EventsManager extends TheKeyService {
    void loginEvent(@NonNull String guid);

    void logoutEvent(@NonNull String guid, boolean changingUser);

    void changeDefaultSessionEvent(@NonNull String guid);

    void attributesUpdatedEvent(@NonNull String guid);
}
