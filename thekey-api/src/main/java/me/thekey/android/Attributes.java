package me.thekey.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;

public interface Attributes {
    @Nullable
    String getUsername();

    @Nullable
    String getGuid();

    @NonNull
    Date getLoadedTime();

    boolean areValid();

    @Nullable
    String getEmail();

    @Nullable
    String getFirstName();

    @Nullable
    String getLastName();
}
