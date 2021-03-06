package me.thekey.android;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import me.thekey.android.exception.TheKeyApiError;
import me.thekey.android.exception.TheKeySocketException;

interface TheKeyTokenApi {
    String PARAM_GRANT_TYPE = "grant_type";
    String PARAM_CODE_VERIFIER = "code_verifier";
    String PARAM_REFRESH_TOKEN = "refresh_token";
    String PARAM_USERNAME = "username";
    String PARAM_PASSWORD = "password";

    String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    String GRANT_TYPE_PASSWORD = "password";
    String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    String JSON_EXPIRES_IN = "expires_in";
    String JSON_REFRESH_TOKEN = "refresh_token";
    String JSON_THEKEY_GUID = "thekey_guid";

    /**
     * Process an OAuth code grant request. This method is blocking, do not call it from the UI thread.
     *
     * @param code        The authorization code being processed
     * @param redirectUri The redirect_uri the authorization code was issued for
     * @param state       The state used with this grant. This will be used to lookup any code_verifier for this grant.
     * @return The guid the code grant was successfully processed for, null if there was an error.
     * @throws TheKeyApiError if there is an error returned from the API.
     */
    @Nullable
    @WorkerThread
    String processCodeGrant(@NonNull String code, @NonNull Uri redirectUri, @Nullable String state)
            throws TheKeyApiError, TheKeySocketException;

    /**
     * Process an OAuth password grant request. This method is blocking, do not call it from the UI thread.
     *
     * @param username The username of the user being authenticated.
     * @param password The password of the user being authenticated.
     * @return The guid the password grant was successfully processed for, null if there was an error.
     * @throws TheKeyApiError if there is an error returned from the API.
     */
    @Nullable
    @WorkerThread
    String processPasswordGrant(@NonNull String username, @NonNull String password)
            throws TheKeyApiError, TheKeySocketException;
}
