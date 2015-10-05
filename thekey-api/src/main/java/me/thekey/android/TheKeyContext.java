package me.thekey.android;

import android.support.annotation.NonNull;

@Deprecated
public interface TheKeyContext {
    /**
     * return TheKey object used within this Application
     *
     * @return the Key object to use for Key related requests
     */
    @NonNull
    TheKey getTheKey();
}
