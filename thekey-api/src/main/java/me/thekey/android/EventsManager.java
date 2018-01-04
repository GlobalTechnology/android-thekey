package me.thekey.android;

import android.support.annotation.NonNull;

public interface EventsManager {
    void loginEvent(@NonNull String guid);

    void logoutEvent(@NonNull String guid, boolean changingUser);

    void changeDefaultSessionEvent(@NonNull String guid);

    void attributesUpdatedEvent(@NonNull String guid);
}
