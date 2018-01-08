package me.thekey.android.exception;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import org.json.JSONObject;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

public final class InvalidCredentialsAuthError extends InvalidGrantApiError {
    @RestrictTo(LIBRARY)
    static final String ERROR_INVALID_CREDENTIALS = "invalid_credentials";

    InvalidCredentialsAuthError(final int responseCode, @NonNull final JSONObject json) {
        super(responseCode, json);
    }
}
