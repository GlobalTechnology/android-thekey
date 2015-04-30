package me.thekey.android;

import javax.annotation.Nonnull;

@Deprecated
public interface TheKeyContext {
    /**
     * return TheKey object used within this Application
     *
     * @return the Key object to use for Key related requests
     */
    @Nonnull
    TheKey getTheKey();
}
