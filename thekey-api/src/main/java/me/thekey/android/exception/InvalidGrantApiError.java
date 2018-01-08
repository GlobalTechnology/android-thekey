package me.thekey.android.exception;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import org.json.JSONObject;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;
import static me.thekey.android.exception.InvalidCredentialsAuthError.ERROR_INVALID_CREDENTIALS;

public class InvalidGrantApiError extends TheKeyApiError {
    public static final String ERROR_INVALID_GRANT = "invalid_grant";

    private static final String JSON_AUTH_ERROR = "thekey_authn_error";

    InvalidGrantApiError(final int responseCode, @NonNull final JSONObject json) {
        super(responseCode, json);
    }

    @Nullable
    public String getAuthError() {
        return mJsonResponse.optString(JSON_AUTH_ERROR, null);
    }

    @NonNull
    @RestrictTo(LIBRARY)
    static InvalidGrantApiError parseInvalidGrantError(final int responseCode, @NonNull final JSONObject json) {
        switch (json.optString(JSON_AUTH_ERROR, "")) {
            case ERROR_INVALID_CREDENTIALS:
                return new InvalidCredentialsAuthError(responseCode, json);
            default:
                return new InvalidGrantApiError(responseCode, json);
        }
    }
}
