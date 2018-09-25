package me.thekey.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import me.thekey.android.exception.TheKeySocketException;

interface TheKeyTicketApi extends TheKeySessions {
    String PARAM_SERVICE = "service";
    String JSON_TICKET = "ticket";

    /**
     * This method returns a ticket for the specified service. This method is a
     * blocking method and should never be called directly on the UI thread.
     *
     * @param service
     * @return The ticket
     */
    @Nullable
    @WorkerThread
    default String getTicket(@NonNull String service) throws TheKeySocketException {
        final String guid = getDefaultSessionGuid();
        return guid != null ? getTicket(guid, service) : null;
    }

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
}
