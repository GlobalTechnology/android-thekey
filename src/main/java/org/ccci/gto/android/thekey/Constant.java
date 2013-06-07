package org.ccci.gto.android.thekey;

import android.net.Uri;

public final class Constant {
    public static final Uri CAS_SERVER = Uri.parse("https://casdev.gcx.org/cas/");
    protected static final Uri REDIRECT_URI = Uri.parse("thekey:/oauth/mobile/android");

    // OAuth request/response params
    public static final String OAUTH_PARAM_ACCESS_TOKEN = "access_token";
    public static final String OAUTH_PARAM_CLIENT_ID = "client_id";
    public static final String OAUTH_PARAM_CODE = "code";
    public static final String OAUTH_PARAM_ERROR = "error";
    public static final String OAUTH_PARAM_GRANT_TYPE = "grant_type";
    public static final String OAUTH_PARAM_REDIRECT_URI = "redirect_uri";
    public static final String OAUTH_PARAM_REFRESH_TOKEN = "refresh_token";
    public static final String OAUTH_PARAM_STATE = "state";
    public static final String OAUTH_PARAM_THEKEY_GUID = "thekey_guid";

    // OAuth grant types
    public static final String OAUTH_GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String OAUTH_GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    // The Key API params
    public static final String THEKEY_PARAM_SERVICE = "service";
    public static final String THEKEY_PARAM_TICKET = "ticket";
}
