package me.thekey.android.exception;

import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

public final class InvalidCredentialsAuthError extends InvalidGrantApiError {
    @RestrictTo(LIBRARY)
    static final String ERROR_INVALID_CREDENTIALS = "invalid_credentials";

    InvalidCredentialsAuthError(final int responseCode, @NonNull final JSONObject json) {
        super(responseCode, json);
    }
}
