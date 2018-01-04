package me.thekey.android;

import android.net.Uri;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;

public interface TheKey extends TheKeyAttributeApi, TheKeyTicketApi, TheKeyTokenApi {
    String ACTION_LOGIN = "thekey.action_login";
    String ACTION_LOGOUT = "thekey.action_logout";
    String ACTION_CHANGE_DEFAULT_SESSION = "thekey.action_change_default_session";
    String ACTION_ATTRIBUTES_LOADED = "thekey.action_attributes_loaded";

    String EXTRA_GUID = "guid";
    String EXTRA_CHANGING_USER = "changing_user";

    long INVALID_CLIENT_ID = -1;

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
     * @return the configured default redirect_uri
     */
    @NonNull
    @AnyThread
    Uri getDefaultRedirectUri();

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
