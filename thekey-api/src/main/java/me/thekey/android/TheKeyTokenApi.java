package me.thekey.android;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

interface TheKeyTokenApi {
    String PARAM_GRANT_TYPE = "grant_type";
    String PARAM_REFRESH_TOKEN = "refresh_token";

    String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    String JSON_REFRESH_TOKEN = "refresh_token";

    /**
     * Process an OAuth code grant request. This method is blocking, do not call it from the UI thread.
     *
     * @param code        The authorization code being processed
     * @param redirectUri The redirect_uri the authorization code was issued for
     * @return The guid the code grant was successfully processed for, null if there was an error.
     */
    @WorkerThread
    String processCodeGrant(@NonNull String code, @NonNull Uri redirectUri) throws TheKeySocketException;
}
