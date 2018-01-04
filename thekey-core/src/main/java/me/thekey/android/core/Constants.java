package me.thekey.android.core;

import android.net.Uri;

public final class Constants {
    public static final Uri CAS_SERVER = Uri.parse("https://thekey.me/cas/");

    // OAuth request/response params
    public static final String OAUTH_PARAM_ACCESS_TOKEN = "access_token";
    public static final String OAUTH_PARAM_CLIENT_ID = "client_id";
    public static final String OAUTH_PARAM_CODE = "code";
    public static final String OAUTH_PARAM_ERROR = "error";
    public static final String OAUTH_PARAM_EXPIRES_IN = "expires_in";
    public static final String OAUTH_PARAM_GRANT_TYPE = "grant_type";
    public static final String OAUTH_PARAM_REDIRECT_URI = "redirect_uri";
    public static final String OAUTH_PARAM_REFRESH_TOKEN = "refresh_token";
    public static final String OAUTH_PARAM_RESPONSE_TYPE = "response_type";
    public static final String OAUTH_PARAM_STATE = "state";
    public static final String OAUTH_PARAM_THEKEY_GUID = "thekey_guid";
    public static final String OAUTH_PARAM_THEKEY_USERNAME = "thekey_username";

    // OAuth attribute params
    public static final String OAUTH_PARAM_ATTR_GUID = "ssoGuid";
    public static final String OAUTH_PARAM_ATTR_EMAIL = "email";
    public static final String OAUTH_PARAM_ATTR_FIRST_NAME = "firstName";
    public static final String OAUTH_PARAM_ATTR_LAST_NAME = "lastName";

    // OAuth response types
    public static final String OAUTH_RESPONSE_TYPE_CODE = "code";

    // OAuth grant types
    public static final String OAUTH_GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String OAUTH_GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    // The Key API params
    public static final String THEKEY_PARAM_SERVICE = "service";
    public static final String THEKEY_PARAM_TICKET = "ticket";

    // configuration arguments
    public static final String ARG_SELF_SERVICE = "enable.self_service";
}
