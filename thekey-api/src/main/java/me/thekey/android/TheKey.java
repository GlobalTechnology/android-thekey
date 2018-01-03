package me.thekey.android;

import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.Collection;
import java.util.Date;

public interface TheKey {
    String ACTION_LOGIN = "thekey.action_login";
    String ACTION_LOGOUT = "thekey.action_logout";
    String ACTION_CHANGE_DEFAULT_SESSION = "thekey.action_change_default_session";
    String ACTION_ATTRIBUTES_LOADED = "thekey.action_attributes_loaded";

    String EXTRA_GUID = "guid";
    String EXTRA_CHANGING_USER = "changing_user";

    long INVALID_CLIENT_ID = -1;

    interface Attributes {
        @Nullable
        String getUsername();

        @Nullable
        String getGuid();

        @NonNull
        Date getLoadedTime();

        boolean areValid();

        @Nullable
        String getEmail();

        @Nullable
        String getFirstName();

        @Nullable
        String getLastName();
    }

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
     * This method will return if the session for the specified guid is valid. This is a non-blocking method and may be
     * called on the UI thread.
     *
     * @param guid the guid of the session being checked
     * @return whether or not the specified session is valid
     */
    @AnyThread
    boolean isValidSession(@Nullable String guid);

    /**
     * This method will load the attributes for the default OAuth session from
     * The Key. This method is a blocking method and should never be called
     * directly on the UI thread.
     *
     * @return whether or not attributes were loaded
     */
    @WorkerThread
    boolean loadAttributes() throws TheKeySocketException;

    /**
     * This method will load the attributes for the specified OAuth session from
     * The Key. This method is a blocking method and should never be called
     * directly on the UI thread.
     *
     * @return whether or not attributes were loaded
     */
    @WorkerThread
    boolean loadAttributes(@Nullable String guid) throws TheKeySocketException;

    /**
     * This method will return the most recently loaded attributes for the
     * active OAuth session. This method does not attempt to load the
     * attributes if they haven't been loaded yet, to load the attributes see
     * {@link TheKey#loadAttributes()}. This is a non-blocking method and may be
     * called on the UI thread.
     *
     * @return The attributes for the current OAuth session
     */
    @NonNull
    @AnyThread
    Attributes getAttributes();

    /**
     * This method will return the most recently loaded attributes for the
     * specified OAuth session. This method does not attempt to load the
     * attributes if they haven't been loaded yet, to load the attributes see
     * {@link TheKey#loadAttributes()}. This is a non-blocking method and may be
     * called on the UI thread.
     *
     * @return The attributes for the current OAuth session
     */
    @NonNull
    @AnyThread
    Attributes getAttributes(@Nullable String guid);

    /**
     * This method returns a ticket for the specified service. This method is a
     * blocking method and should never be called directly on the UI thread.
     *
     * @param service
     * @return The ticket
     */
    @Nullable
    @WorkerThread
    String getTicket(@NonNull String service) throws TheKeySocketException;

    /**
     * This method returns a ticket for the specified service for the specified session. This method is a
     * blocking method and should never be called directly on the UI thread.
     *
     * @param service
     * @return The ticket
     */
    @Nullable
    @WorkerThread
    String getTicket(@NonNull String guid, @NonNull String service) throws TheKeySocketException;

    /**
     * This method will logout the default user. This is a non-blocking method
     * and may be called on the UI thread.
     */
    @AnyThread
    void logout();

    /**
     * This method will logout the specified user. This is a non-blocking method
     * and may be called on the UI thread.
     */
    @AnyThread
    void logout(@NonNull String guid);
}
