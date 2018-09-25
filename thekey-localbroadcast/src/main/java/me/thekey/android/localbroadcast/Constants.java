package me.thekey.android.localbroadcast;

import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

@RestrictTo(LIBRARY)
final class Constants {
    static final String ACTION_LOGIN = "thekey.action_login";
    static final String ACTION_LOGOUT = "thekey.action_logout";
    static final String ACTION_CHANGE_DEFAULT_SESSION = "thekey.action_change_default_session";
    static final String ACTION_ATTRIBUTES_LOADED = "thekey.action_attributes_loaded";

    static final String EXTRA_GUID = "guid";
    static final String EXTRA_CHANGING_USER = "changing_user";
}
