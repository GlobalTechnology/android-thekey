package me.thekey.android;

import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import me.thekey.android.exception.TheKeySocketException;

interface TheKeyAttributeApi extends TheKeySessions {
    String JSON_ATTR_GUID = "ssoGuid";
    String JSON_ATTR_EMAIL = "email";
    String JSON_ATTR_FIRST_NAME = "firstName";
    String JSON_ATTR_LAST_NAME = "lastName";

    /**
     * This method will load the attributes for the default OAuth session from
     * The Key. This method is a blocking method and should never be called
     * directly on the UI thread.
     *
     * @return whether or not attributes were loaded
     */
    @WorkerThread
    default boolean loadAttributes() throws TheKeySocketException {
        return loadAttributes(getDefaultSessionGuid());
    }

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
    default Attributes getAttributes() {
        return getAttributes(getDefaultSessionGuid());
    }

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
}
