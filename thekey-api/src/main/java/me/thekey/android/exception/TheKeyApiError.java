package me.thekey.android.exception;

import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.SUBCLASSES;
import static me.thekey.android.exception.InvalidGrantApiError.ERROR_INVALID_GRANT;
import static me.thekey.android.exception.InvalidGrantApiError.parseInvalidGrantError;
import static me.thekey.android.exception.RateLimitExceededApiError.ERROR_RATE_LIMIT_EXCEEDED;

public class TheKeyApiError extends TheKeyException {
    public static final String ERROR_UNKNOWN_ERROR = "unknown_error";

    private static final String JSON_ERROR = "error";

    private final int mResponseCode;
    @NonNull
    @RestrictTo(SUBCLASSES)
    protected final JSONObject mJsonResponse;

    @RestrictTo(SUBCLASSES)
    TheKeyApiError(final int responseCode, @NonNull final JSONObject json) {
        mResponseCode = responseCode;
        mJsonResponse = json;
    }

    public final int getResponseCode() {
        return mResponseCode;
    }

    @NonNull
    public final String getError() {
        return mJsonResponse.optString(JSON_ERROR, ERROR_UNKNOWN_ERROR);
    }

    /**
     * Return the JSON response for the API error. The caller should not modify this object even though the JSONObject
     * is mutable.
     */
    @NonNull
    public JSONObject getJsonResponse() {
        return mJsonResponse;
    }

    @NonNull
    public static TheKeyApiError parse(final int responseCode, @NonNull final JSONObject json) {
        switch (json.optString(JSON_ERROR, ERROR_UNKNOWN_ERROR)) {
            case ERROR_INVALID_GRANT:
                return parseInvalidGrantError(responseCode, json);
            case ERROR_RATE_LIMIT_EXCEEDED:
                return new RateLimitExceededApiError(responseCode, json);
            case ERROR_UNKNOWN_ERROR:
            default:
                return new TheKeyApiError(responseCode, json);
        }
    }
}
