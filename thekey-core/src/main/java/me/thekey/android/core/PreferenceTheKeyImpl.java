package me.thekey.android.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import me.thekey.android.Attributes;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;
import static android.support.annotation.RestrictTo.Scope.SUBCLASSES;
import static me.thekey.android.Attributes.ATTR_SSO_GUID;
import static me.thekey.android.core.Constants.OAUTH_PARAM_ACCESS_TOKEN;
import static me.thekey.android.core.Constants.OAUTH_PARAM_THEKEY_USERNAME;

@RestrictTo(LIBRARY)
final class PreferenceTheKeyImpl extends TheKeyImpl {
    private static final String PREFFILE_THEKEY = "thekey";
    static final String PREF_ACCESS_TOKEN = "access_token";
    static final String PREF_EXPIRE_TIME = "expire_time";
    static final String PREF_USERNAME = "username";
    static final String PREF_GUID = "guid";
    static final String PREF_REFRESH_TOKEN = "refresh_token";
    static final String PREF_ATTR_LOAD_TIME = "attr_load_time";
    static final String PREF_ATTR_PREFIX = "attr.";

    private final Object mLockPrefs = new Object();

    PreferenceTheKeyImpl(@NonNull final Context context, @NonNull final Configuration config) {
        super(context, config);
    }

    @NonNull
    @Override
    public Collection<String> getSessions() {
        final String guid = getSessionGuid();
        return guid != null ? Collections.singleton(guid) : Collections.emptySet();
    }

    @Nullable
    private String getSessionGuid() {
        return getPrefs().getString(PREF_GUID, null);
    }

    @Override
    public boolean isValidSession(@Nullable final String guid) {
        return guid != null && guid.equals(getSessionGuid());
    }

    @NonNull
    @Override
    public Attributes getAttributes(@Nullable final String guid) {
        synchronized (mLockPrefs) {
            if (TextUtils.equals(guid, getSessionGuid())) {
                // return the attributes for the current OAuth session
                return new AttributesImpl(getPrefs().getAll());
            } else {
                throw new UnsupportedOperationException(
                        "cannot get attributes for users other than the active session");
            }
        }
    }

    @NonNull
    private SharedPreferences getPrefs() {
        return mContext.getSharedPreferences(PREFFILE_THEKEY, Context.MODE_PRIVATE);
    }

    @Override
    @RestrictTo(SUBCLASSES)
    boolean storeGrants(@NonNull final String guid, @NonNull final JSONObject json) {
        try {
            final SharedPreferences.Editor prefs = this.getPrefs().edit();

            // store access_token
            if (json.has(OAUTH_PARAM_ACCESS_TOKEN)) {
                prefs.putString(PREF_ACCESS_TOKEN, json.getString(OAUTH_PARAM_ACCESS_TOKEN));
                prefs.remove(PREF_EXPIRE_TIME);
                if (json.has(JSON_EXPIRES_IN)) {
                    prefs.putLong(PREF_EXPIRE_TIME, System.currentTimeMillis() + json.getLong(JSON_EXPIRES_IN) * 1000);
                }
                prefs.remove(PREF_GUID);
                prefs.remove(PREF_USERNAME);
                if (json.has(JSON_THEKEY_GUID)) {
                    prefs.putString(PREF_GUID, json.getString(JSON_THEKEY_GUID));
                }
                if (json.has(OAUTH_PARAM_THEKEY_USERNAME)) {
                    prefs.putString(PREF_USERNAME, json.getString(OAUTH_PARAM_THEKEY_USERNAME));
                }
            }

            // store refresh_token
            if (json.has(JSON_REFRESH_TOKEN)) {
                prefs.putString(PREF_REFRESH_TOKEN, json.getString(JSON_REFRESH_TOKEN));
            }

            // we synchronize actual update to prevent race conditions
            final String oldGuid;
            synchronized (mLockPrefs) {
                oldGuid = getPrefs().getString(PREF_GUID, null);

                // store updates
                prefs.apply();
            }

            // trigger logout/login broadcasts based on guid changes
            final String newGuid = json.optString(JSON_THEKEY_GUID, null);
            if (oldGuid != null && !oldGuid.equals(newGuid)) {
                mEventsManager.logoutEvent(oldGuid, newGuid != null);
            }
            if (newGuid != null && !newGuid.equals(oldGuid)) {
                mEventsManager.loginEvent(newGuid);
            }
        } catch (final JSONException e) {
            clearAuthState(guid, true);
        }

        return true;
    }

    @Nullable
    @Override
    String getRefreshToken(@NonNull final String guid) {
        final Map<String, ?> attrs = getPrefs().getAll();
        if (guid.equals(attrs.get(PREF_GUID))) {
            return (String) attrs.get(PREF_REFRESH_TOKEN);
        }
        return null;
    }

    @Override
    void removeRefreshToken(@NonNull final String guid, @NonNull final String token) {
        final SharedPreferences.Editor prefs = getPrefs().edit();
        prefs.remove(PREF_REFRESH_TOKEN);

        synchronized (mLockPrefs) {
            // short-circuit if the specified guid is different from the stored session
            if (!TextUtils.equals(guid, getSessionGuid())) {
                return;
            }

            if (token.equals(getPrefs().getString(PREF_REFRESH_TOKEN, null))) {
                prefs.apply();
            }
        }
    }

    @Nullable
    @Override
    String getAccessToken(@NonNull final String guid) {
        final Map<String, ?> attrs = getPrefs().getAll();
        final long currentTime = System.currentTimeMillis();
        final Long rawExpireTime = (Long) attrs.get(PREF_EXPIRE_TIME);
        final long expireTime = rawExpireTime != null ? rawExpireTime : currentTime;

        // return access_token only if it hasn't expired (and is for the requested user)
        return expireTime >= currentTime && guid.equals(attrs.get(PREF_GUID)) ? (String) attrs.get(PREF_ACCESS_TOKEN) :
                null;
    }

    @Override
    void removeAccessToken(@NonNull final String guid, @NonNull final String token) {
        final SharedPreferences.Editor prefs = getPrefs().edit();
        prefs.remove(PREF_ACCESS_TOKEN);
        prefs.remove(PREF_EXPIRE_TIME);

        synchronized (mLockPrefs) {
            // short-circuit if the specified guid is different from the stored session
            if (!TextUtils.equals(guid, getSessionGuid())) {
                return;
            }

            if (token.equals(getPrefs().getString(PREF_ACCESS_TOKEN, null))) {
                prefs.apply();
            }
        }
    }

    @Override
    void storeAttributes(@NonNull final String guid, @NonNull final JSONObject json) {
        final SharedPreferences.Editor prefs = getPrefs().edit();
        prefs.putLong(PREF_ATTR_LOAD_TIME, System.currentTimeMillis());
        final Iterator<String> attrs = json.keys();
        while (attrs.hasNext()) {
            final String key = attrs.next();
            prefs.putString(PREF_ATTR_PREFIX + key, json.optString(key, null));
        }

        // we synchronize this to prevent race conditions with getAttributes
        synchronized (mLockPrefs) {
            // short-circuit if the specified guid is different from the stored session
            if (!TextUtils.equals(guid, getSessionGuid())) {
                return;
            }

            // apply updates
            removeOldAttributes(prefs);
            prefs.apply();
        }
    }

    @Override
    void removeAttributes(@NonNull final String guid) {
        final SharedPreferences.Editor prefs = getPrefs().edit();

        // we synchronize this to prevent race conditions with getAttributes
        synchronized (mLockPrefs) {
            // short-circuit if the specified guid is different from the stored session
            if (!TextUtils.equals(guid, getSessionGuid())) {
                return;
            }

            // apply updates
            removeOldAttributes(prefs);
            prefs.apply();
        }
    }

    private void removeOldAttributes(final SharedPreferences.Editor prefs) {
        // remove all stored attributes
        prefs.remove(PREF_ATTR_LOAD_TIME);
        for (final String key : getPrefs().getAll().keySet()) {
            if (key.startsWith(PREF_ATTR_PREFIX)) {
                prefs.remove(key);
            }
        }

        // TODO: remove after 2.1.0
        // legacy attributes, no longer used
        prefs.remove("attr_guid");
        prefs.remove("attr_email");
        prefs.remove("attr_firstName");
        prefs.remove("attr_lastName");
    }

    @Override
    void clearAuthState(@NonNull final String guid, final boolean sendBroadcast) {
        final SharedPreferences.Editor prefs = this.getPrefs().edit();
        prefs.remove(PREF_ACCESS_TOKEN);
        prefs.remove(PREF_REFRESH_TOKEN);
        prefs.remove(PREF_EXPIRE_TIME);
        prefs.remove(PREF_GUID);
        prefs.remove(PREF_USERNAME);

        synchronized (mLockPrefs) {
            // short-circuit if the specified guid is different from the stored session
            if (!TextUtils.equals(guid, getSessionGuid())) {
                return;
            }

            prefs.apply();
        }

        if (sendBroadcast) {
            // broadcast a logout action if we had a guid
            mEventsManager.logoutEvent(guid, false);
        }
    }

    @Override
    boolean createMigratingAccount(@NonNull final MigratingAccount account) {
        if (account.isValid()) {
            final SharedPreferences.Editor prefs = getPrefs().edit();
            prefs.putString(PREF_GUID, account.guid);
            prefs.putString(PREF_USERNAME, account.attributes.getUsername());
            prefs.putString(PREF_ACCESS_TOKEN, account.accessToken);
            prefs.putString(PREF_REFRESH_TOKEN, account.refreshToken);

            prefs.putLong(PREF_ATTR_LOAD_TIME, account.attributes.getLoadedTime().getTime());
            prefs.putString(PREF_ATTR_PREFIX + ATTR_SSO_GUID, account.guid);
            for (final String name : account.attributes.getAttributeNames()) {
                prefs.putString(PREF_ATTR_PREFIX + name, account.attributes.getAttribute(name));
            }

            prefs.apply();

            return true;
        }

        return false;
    }

    private static final class AttributesImpl implements Attributes {
        private final Map<String, ?> mAttrs;
        private final boolean mValid;

        @Nullable
        private transient Set<String> mAttrNames;

        AttributesImpl(final Map<String, ?> prefsMap) {
            mAttrs = new HashMap<String, Object>(prefsMap);
            mAttrs.remove(PREF_ACCESS_TOKEN);
            mAttrs.remove(PREF_REFRESH_TOKEN);
            mAttrs.remove(PREF_EXPIRE_TIME);

            // determine if the attributes are valid
            final String guid = (String) mAttrs.get(PREF_GUID);
            mValid = mAttrs.containsKey(PREF_ATTR_LOAD_TIME) &&
                    guid != null && guid.equalsIgnoreCase((String) mAttrs.get(PREF_ATTR_PREFIX + ATTR_SSO_GUID));
        }

        @Nullable
        @Override
        public String getUsername() {
            final Object username = mAttrs.get(PREF_USERNAME);
            return username instanceof String ? (String) username : getEmail();
        }

        @Nullable
        @Override
        public String getGuid() {
            return (String) mAttrs.get(PREF_GUID);
        }

        @Override
        public boolean areValid() {
            return mValid;
        }

        @NonNull
        @Override
        public Date getLoadedTime() {
            final Long time = mValid ? (Long) mAttrs.get(PREF_ATTR_LOAD_TIME) : null;
            return new Date(time != null ? time : 0);
        }

        @NonNull
        @Override
        public Collection<String> getAttributeNames() {
            if (mAttrNames == null) {
                mAttrNames = Collections.unmodifiableSet(extractAttributeNames());
            }

            return mAttrNames;
        }

        @Nullable
        public String getAttribute(@NonNull final String name) {
            return mValid ? (String) mAttrs.get(PREF_ATTR_PREFIX + name) : null;
        }

        @NonNull
        private Set<String> extractAttributeNames() {
            final Set<String> names = new HashSet<>();
            for (final String key : mAttrs.keySet()) {
                if (key.startsWith(PREF_ATTR_PREFIX)) {
                    names.add(key.substring(PREF_ATTR_PREFIX.length()));
                }
            }
            return names;
        }
    }
}
