package me.thekey.android;

import android.net.Uri;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;

public interface TheKey extends TheKeySessions, TheKeyAttributeApi, TheKeyTicketApi, TheKeyTokenApi {
    long INVALID_CLIENT_ID = -1;

    // RFC-7636 PKCE
    String PARAM_CODE_CHALLENGE_METHOD = "code_challenge_method";
    String PARAM_CODE_CHALLENGE = "code_challenge";
    String CODE_CHALLENGE_METHOD_S256 = "S256";

    /**
     * @return the configured default redirect_uri
     */
    @NonNull
    @AnyThread
    Uri getDefaultRedirectUri();
}
