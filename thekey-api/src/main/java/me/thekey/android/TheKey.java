package me.thekey.android;

import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TheKey {
    String ACTION_LOGIN = TheKey.class.getName() + ".ACTION_LOGIN";
    String ACTION_LOGOUT = TheKey.class.getName() + ".ACTION_LOGOUT";
    String ACTION_ATTRIBUTES_LOADED = TheKey.class.getName() + ".ACTION_ATTRIBUTES_LOADED";

    String EXTRA_GUID = "guid";
    String EXTRA_CHANGING_USER = "changing_user";

    long INVALID_CLIENT_ID = -1;

    interface Attributes {
        @Nullable
        String getUsername();

        @Nullable
        String getGuid();

        @Nonnull
        Date getLoadedTime();

        boolean areValid();

        @Nullable
        String getEmail();

        @Nullable
        String getFirstName();

        @Nullable
        String getLastName();
    }

    @Deprecated
    final class TicketAttributesPair {
        @Nonnull
        public final String ticket;

        @Nonnull
        public final Attributes attributes;

        public TicketAttributesPair(@Nonnull final String ticket, @Nonnull final Attributes attributes) {
            this.ticket = ticket;
            this.attributes = attributes;
        }
    }

    /**
     * This method will switch the current active OAuth session to use by default.
     *
     * @param guid the guid of the session to use as the default session
     * @throws TheKeyInvalidSessionException Thrown if there isn't a valid session for the specified guid
     */
    void setDefaultSession(@Nonnull String guid) throws TheKeyInvalidSessionException;

    /**
     * This method will return the guid of the default OAuth session. This is a non-blocking method and may be called
     * on the UI thread.
     *
     * @return the user's guid
     */
    @Nullable
    String getDefaultSessionGuid();

    /**
     * This method will return the guid of the current OAuth session. This is a
     * non-blocking method and may be called on the UI thread.
     *
     * @return the user's guid
     * @deprecated use {@link TheKey#getDefaultSessionGuid()} instead
     */
    @Deprecated
    @Nullable
    String getGuid();

    /**
     * This method will return if the session for the specified guid is valid. This is a non-blocking method and may be
     * called on the UI thread.
     *
     * @param guid the guid of the session being checked
     * @return whether or not the specified session is valid
     */
    boolean isValidSession(@Nullable String guid);

    /**
     * This method will load the attributes for the default OAuth session from
     * The Key. This method is a blocking method and should never be called
     * directly on the UI thread.
     *
     * @return whether or not attributes were loaded
     */
    boolean loadAttributes() throws TheKeySocketException;

    /**
     * This method will load the attributes for the specified OAuth session from
     * The Key. This method is a blocking method and should never be called
     * directly on the UI thread.
     *
     * @return whether or not attributes were loaded
     */
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
    @Nonnull
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
    @Nonnull
    Attributes getAttributes(@Nullable String guid);

    /**
     * This method returns a ticket for the specified service. This method is a
     * blocking method and should never be called directly on the UI thread.
     *
     * @param service
     * @return The ticket
     */
    @Nullable
    String getTicket(@Nonnull String service) throws TheKeySocketException;

    /**
     * This method returns a ticket for the specified service for the specified session. This method is a
     * blocking method and should never be called directly on the UI thread.
     *
     * @param service
     * @return The ticket
     */
    @Nullable
    String getTicket(@Nonnull String guid, @Nonnull String service) throws TheKeySocketException;

    /**
     * This method returns a ticket for the specified service and attributes the
     * ticket was issued for. This is a blocking method and should never be
     * called directly on the UI thread.
     *
     * @param service
     * @return The ticket &amp; attributes for the current session, or null if no ticket could be retrieved
     * @deprecated This functionality has been deprecated in favor of discrete {@link TheKey#getTicket(String, String)} and {@link TheKey#getAttributes(String)} methods.
     */
    @Deprecated
    @Nullable
    TicketAttributesPair getTicketAndAttributes(@Nonnull String service) throws TheKeySocketException;

    /**
     * This method will logout the default user. This is a non-blocking method
     * and may be called on the UI thread.
     */
    void logout();

    /**
     * This method will logout the specified user. This is a non-blocking method
     * and may be called on the UI thread.
     */
    void logout(@Nonnull String guid);
}
