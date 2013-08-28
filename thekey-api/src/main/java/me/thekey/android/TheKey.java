package me.thekey.android;

import android.util.Pair;

public interface TheKey {
    interface Attributes {
        String getGuid();
    }

    String getGuid();

    /**
     * This method returns a ticket for the specified service. This method is a
     * blocking method and should never be called directly on the UI thread.
     * 
     * @param service
     * @return The ticket
     */
    public String getTicket(final String service) throws TheKeySocketException;

    /**
     * This method returns a ticket for the specified service and attributes the
     * ticket was issued for. This is a blocking method and should never be
     * called directly on the UI thread.
     * 
     * @param service
     * @return The ticket & attributes
     */
    public Pair<String, Attributes> getTicketAndAttributes(final String service) throws TheKeySocketException;
}
