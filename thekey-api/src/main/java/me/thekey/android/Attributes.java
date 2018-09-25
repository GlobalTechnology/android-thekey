package me.thekey.android;

import java.util.Collection;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

public interface Attributes {
    String ATTR_SSO_GUID = "ssoGuid";
    String ATTR_GR_MASTER_PERSON_ID = "grMasterPersonId";
    String ATTR_EMAIL = "email";
    String ATTR_FIRST_NAME = "firstName";
    String ATTR_LAST_NAME = "lastName";

    @RestrictTo(LIBRARY)
    long STALE_AGE = 24 * 60 * 60 * 1000;

    @Nullable
    String getUsername();

    @Nullable
    String getGuid();

    @NonNull
    Date getLoadedTime();

    default boolean areStale() {
        return getLoadedTime().before(new Date(System.currentTimeMillis() - STALE_AGE));
    }

    boolean areValid();

    @NonNull
    Collection<String> getAttributeNames();

    @Nullable
    String getAttribute(@NonNull String name);

    @Nullable
    default String getEmail() {
        return getAttribute(ATTR_EMAIL);
    }

    @Nullable
    default String getFirstName() {
        return getAttribute(ATTR_FIRST_NAME);
    }

    @Nullable
    default String getLastName() {
        return getAttribute(ATTR_LAST_NAME);
    }
}
