package me.thekey.android;

import android.net.Uri;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

    /**
     * @see TheKey#getAuthorizeUri(Uri, String)
     */
    @NonNull
    @AnyThread
    default Uri getAuthorizeUri() {
        return getAuthorizeUri(null, null);
    }

    /**
     * @see TheKey#getAuthorizeUri(Uri, String)
     */
    @NonNull
    @AnyThread
    default Uri getAuthorizeUri(@Nullable final Uri redirectUri) {
        return getAuthorizeUri(redirectUri, null);
    }

    /**
     * @see TheKey#getAuthorizeUri(Uri, String)
     */
    @NonNull
    @AnyThread
    default Uri getAuthorizeUri(@Nullable final String state) {
        return getAuthorizeUri(null, state);
    }

    /**
     * Generate an authorize URI that the user should be directed to in order to authenticate.
     *
     * @param redirectUri The redirect_uri that a user will be returned to after authenticating.
     * @param state       an opaque state string that can be used to associate generation of authorize URI and callback
     *                    after authenticating.
     * @return the authorize URI to send the user to for them to authenticate.
     */
    @NonNull
    @AnyThread
    Uri getAuthorizeUri(@Nullable Uri redirectUri, @Nullable String state);
}
