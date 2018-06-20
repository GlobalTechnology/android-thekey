package me.thekey.android.core;

import android.net.Uri;
import android.support.annotation.RestrictTo;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

public final class Constants {
    @RestrictTo(LIBRARY)
    public static final int DEFAULT_TRAFFIC_STATS_TAG = 815358825;

    public static final Uri CAS_SERVER = Uri.parse("https://thekey.me/cas/");

    // OAuth request/response params
    public static final String OAUTH_PARAM_ACCESS_TOKEN = "access_token";
    public static final String OAUTH_PARAM_CLIENT_ID = "client_id";
    public static final String OAUTH_PARAM_ERROR = "error";
    public static final String OAUTH_PARAM_REDIRECT_URI = "redirect_uri";
    public static final String OAUTH_PARAM_RESPONSE_TYPE = "response_type";
    public static final String OAUTH_PARAM_THEKEY_USERNAME = "thekey_username";

    // OAuth response types
    public static final String OAUTH_RESPONSE_TYPE_CODE = "code";

    // configuration arguments
    public static final String ARG_SELF_SERVICE = "thekey.enable_self_service";
    public static final String ARG_REDIRECT_URI = "thekey.redirect_uri";
    public static final String ARG_SIGNUP = "thekey.signup";
}
