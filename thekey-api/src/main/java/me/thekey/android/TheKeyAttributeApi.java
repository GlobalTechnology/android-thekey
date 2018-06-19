package me.thekey.android;

import android.os.AsyncTask;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import me.thekey.android.exception.TheKeySocketException;

interface TheKeyAttributeApi extends TheKeySessions {
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
    default Attributes getCachedAttributes() {
        return getCachedAttributes(getDefaultSessionGuid());
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
    Attributes getCachedAttributes(@Nullable String guid);

    /**
     * This method will return attributes for the specified user.
     * This method attempts to use the cached attributes, if the cached attributes are not valid or are stale we attempt
     * to load fresh attributes before populating the returned future.
     * This method is non-blocking, but the returned Future is blocking.
     *
     * @param guid The user we are looking for attributes for.
     * @return a Future that will eventually resolve to the user's attributes.
     */
    @NonNull
    @AnyThread
    default Future<Attributes> getAttributes(@Nullable final String guid) {
        final FutureTask<Attributes> task = new FutureTask<>(() -> {
            final Attributes attributes = getCachedAttributes(guid);

            // refresh attributes if they aren't valid or are stale
            if (!attributes.areValid() || attributes.areStale()) {
                loadAttributes(guid);
                return getCachedAttributes(guid);
            }

            return attributes;
        });
        AsyncTask.THREAD_POOL_EXECUTOR.execute(task);

        return task;
    }
}
