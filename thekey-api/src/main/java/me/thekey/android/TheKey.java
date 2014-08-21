package me.thekey.android;

import java.util.Date;

public interface TheKey {
    public static final String ACTION_LOGIN = TheKey.class.getName() + ".ACTION_LOGIN";
    public static final String ACTION_LOGOUT = TheKey.class.getName() + ".ACTION_LOGOUT";
    public static final String ACTION_ATTRIBUTES_LOADED = TheKey.class.getName() + ".ACTION_ATTRIBUTES_LOADED";

    public static final String EXTRA_GUID = "guid";
    public static final String EXTRA_CHANGING_USER = "changing_user";

    public static final long INVALID_CLIENT_ID = -1;

    interface Attributes {
        String getGuid();

        Date getLoadedTime();

        boolean areValid();

        String getEmail();

        String getFirstName();

        String getLastName();
    }

    public final class TicketAttributesPair {
        public final String ticket;

        public final Attributes attributes;

        public TicketAttributesPair(final String ticket, final Attributes attributes) {
            this.ticket = ticket;
            this.attributes = attributes;
        }
    }

    /**
     * This method will return the guid of the current OAuth session. This is a
     * non-blocking method and may be called on the UI thread.
     *
     * @return the user's guid
     */
    String getGuid();

    /**
     * This method will load the attributes for the current OAuth session from
     * The Key. This method is a blocking method and should never be called
     * directly on the UI thread.
     *
     * @return whether or not attributes were loaded
     */
    boolean loadAttributes() throws TheKeySocketException;

    /**
     * This method will return the most recently loaded attributes for the
     * current OAuth session. This method does not attempt to load the
     * attributes if they haven't been loaded yet, to load the attributes see
     * {@link TheKey#loadAttributes()}. This is a non-blocking method and may be
     * called on the UI thread.
     *
     * @return The attributes for the current OAuth session
     */
    Attributes getAttributes();

    /**
     * This method returns a ticket for the specified service. This method is a
     * blocking method and should never be called directly on the UI thread.
     *
     * @param service
     * @return The ticket
     */
    String getTicket(String service) throws TheKeySocketException;

    /**
     * This method returns a ticket for the specified service and attributes the
     * ticket was issued for. This is a blocking method and should never be
     * called directly on the UI thread.
     *
     * @param service
     * @return The ticket & attributes for the current session, or null if no ticket could be retrieved
     */
    TicketAttributesPair getTicketAndAttributes(String service) throws TheKeySocketException;

    /**
     * This method will logout the current user. This is a non-blocking method
     * and may be called on the UI thread.
     */
    void logout();
}
