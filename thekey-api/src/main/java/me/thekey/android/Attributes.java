package me.thekey.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public interface Attributes {
    String ATTR_SSO_GUID = "ssoGuid";
    String ATTR_EMAIL = "email";
    String ATTR_FIRST_NAME = "firstName";
    String ATTR_LAST_NAME = "lastName";

    @Nullable
    String getUsername();

    @Nullable
    String getGuid();

    @NonNull
    Date getLoadedTime();

    boolean areValid();

    //TODO: this shouldn't be default
    @NonNull
    default Collection<String> getAttributeNames() {
        return Collections.emptyList();
    }

    //TODO: this shouldn't be default
    @Nullable
    default String getAttribute(@NonNull final String name) {
        return null;
    }

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
