package me.thekey.android;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

import me.thekey.android.exception.TheKeyInvalidSessionException;

interface TheKeySessions {
    /**
     * This method will return a list of all current sessions. This is a non-blocking method and may be called on the UI
     * thread.
     *
     * @return a {@link Collection} of currently active session guids
     */
    @NonNull
    @AnyThread
    Collection<String> getSessions();

    /**
     * This method will return if the session for the specified guid is valid. This is a non-blocking method and may be
     * called on the UI thread.
     *
     * @param guid the guid of the session being checked
     * @return whether or not the specified session is valid
     */
    @AnyThread
    boolean isValidSession(@Nullable String guid);

    /**
     * This method will switch the current active OAuth session to use by default.
     *
     * @param guid the guid of the session to use as the default session
     * @throws TheKeyInvalidSessionException Thrown if there isn't a valid session for the specified guid
     */
    @AnyThread
    void setDefaultSession(@NonNull String guid) throws TheKeyInvalidSessionException;

    /**
     * This method will return the guid of the default OAuth session. This is a non-blocking method and may be called
     * on the UI thread.
     *
     * @return the user's guid
     */
    @Nullable
    @AnyThread
    String getDefaultSessionGuid();

    /**
     * This method will logout the default user. This is a non-blocking method
     * and may be called on the UI thread.
     */
    @AnyThread
    default void logout() {
        final String guid = getDefaultSessionGuid();
        if (guid != null) {
            logout(guid);
        }
    }

    /**
     * This method will logout the specified user. This is a non-blocking method
     * and may be called on the UI thread.
     */
    @AnyThread
    void logout(@NonNull String guid);
}
