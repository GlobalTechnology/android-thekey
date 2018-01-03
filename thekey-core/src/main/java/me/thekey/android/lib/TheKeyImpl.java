package me.thekey.android.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.Uri.Builder;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import me.thekey.android.TheKey;
import me.thekey.android.TheKeyInvalidSessionException;
import me.thekey.android.TheKeySocketException;
import me.thekey.android.core.EventsManager;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static me.thekey.android.core.Constants.CAS_SERVER;
import static me.thekey.android.core.Constants.OAUTH_GRANT_TYPE_AUTHORIZATION_CODE;
import static me.thekey.android.core.Constants.OAUTH_GRANT_TYPE_REFRESH_TOKEN;
import static me.thekey.android.core.Constants.OAUTH_PARAM_ACCESS_TOKEN;
import static me.thekey.android.core.Constants.OAUTH_PARAM_CLIENT_ID;
import static me.thekey.android.core.Constants.OAUTH_PARAM_CODE;
import static me.thekey.android.core.Constants.OAUTH_PARAM_GRANT_TYPE;
import static me.thekey.android.core.Constants.OAUTH_PARAM_REDIRECT_URI;
import static me.thekey.android.core.Constants.OAUTH_PARAM_REFRESH_TOKEN;
import static me.thekey.android.core.Constants.OAUTH_PARAM_RESPONSE_TYPE;
import static me.thekey.android.core.Constants.OAUTH_PARAM_STATE;
import static me.thekey.android.core.Constants.OAUTH_PARAM_THEKEY_GUID;
import static me.thekey.android.core.Constants.OAUTH_RESPONSE_TYPE_CODE;
import static me.thekey.android.core.Constants.THEKEY_PARAM_SERVICE;
import static me.thekey.android.core.Constants.THEKEY_PARAM_TICKET;

/**
 * The Key interaction library, handles all interactions with The Key OAuth API
 * endpoints and correctly stores/utilizes OAuth access_tokens locally
 */
public abstract class TheKeyImpl implements TheKey {
    private static final String PREFFILE_THEKEY = "me.thekey";
    private static final String PREF_DEFAULT_GUID = "default_guid";

    private static final Object INSTANCE_LOCK = new Object();
    @Nullable
    private static Configuration sInstanceConfig = null;
    private static TheKeyImpl sInstance = null;

    private final Map<String, Object> mLockAuth = new HashMap<>();

    @NonNull
    final Context mContext;
    @NonNull
    protected final EventsManager mEventsManager;

    @NonNull
    final Configuration mConfig;
    @NonNull
    private final Uri mServer;
    private final long mClientId;
    @Nullable
    private TheKeyImpl mMigrationSource;

    @Nullable
    private String mDefaultGuid;

    TheKeyImpl(@NonNull final Context context, @NonNull final Configuration config) {
        mContext = context;
        mEventsManager = new LocalBroadcastManagerEventsManager(mContext);
        mConfig = config;
        mServer = mConfig.mServer;
        mClientId = mConfig.mClientId;
        if (mClientId == INVALID_CLIENT_ID) {
            throw new IllegalStateException("client_id is invalid or not provided");
        }
        if (mConfig.mMigrationSource != null) {
            mMigrationSource = createInstance(mContext, mConfig.mMigrationSource);
        }
        mDefaultGuid = getPrefs().getString(PREF_DEFAULT_GUID, null);
    }

    @NonNull
    private static TheKeyImpl createInstance(@NonNull final Context context, @NonNull final Configuration config) {
        final TheKeyImpl instance;
        if (TextUtils.isEmpty(config.mAccountType)) {
            instance = new PreferenceTheKeyImpl(context.getApplicationContext(), config);
        } else {
            // dynamically look for AccountManager implementation
            try {
                instance = (TheKeyImpl) Class.forName("me.thekey.android.lib.AccountManagerTheKeyImpl")
                        .getDeclaredConstructor(Context.class, Configuration.class)
                        .newInstance(context.getApplicationContext(), config);
            } catch (final Exception e) {
                throw new RuntimeException("Unable to find AccountManagerTheKeyImpl, " +
                                                   "make sure thekey-accountmanager library is loaded", e);
            }
        }

        // trigger account migration for this instance
        instance.migrateAccounts();

        return instance;
    }

    public static void configure(@NonNull final Configuration config) {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstanceConfig = config;
            } else if (sInstanceConfig == null) {
                throw new IllegalStateException("Strange, we have an instance, but no instance config");
            } else if (!sInstanceConfig.equals(config)) {
                throw new IllegalArgumentException("Configuration cannot be changed after TheKeyImpl is initialized");
            }
        }
    }

    @NonNull
    public static TheKeyImpl getInstance(@NonNull Context context) {
        synchronized (INSTANCE_LOCK) {
            // initialize the instance if we haven't already and we have configuration
            if (sInstance == null && sInstanceConfig != null) {
                sInstance = createInstance(context.getApplicationContext(), sInstanceConfig);
            }

            if (sInstance != null) {
                return sInstance;
            }
        }

        throw new IllegalStateException("TheKeyImpl has not been configured yet!");
    }

    @NonNull
    public static TheKeyImpl getInstance(@NonNull final Context context, @NonNull final Configuration config) {
        configure(config);
        return getInstance(context);
    }

    @NonNull
    private SharedPreferences getPrefs() {
        return mContext.getSharedPreferences(PREFFILE_THEKEY, Context.MODE_PRIVATE);
    }

    @Override
    public final void setDefaultSession(@NonNull final String guid) throws TheKeyInvalidSessionException {
        if (!isValidSession(guid)) {
            throw new TheKeyInvalidSessionException();
        }

        final String oldGuid = mDefaultGuid;

        // persist updated default guid
        mDefaultGuid = guid;
        getPrefs().edit()
                .putString(PREF_DEFAULT_GUID, guid)
                .apply();

        // broadcast that the default session changed
        if (!TextUtils.equals(oldGuid, mDefaultGuid)) {
            mEventsManager.changeDefaultSessionEvent(mDefaultGuid);
        }
    }

    @Nullable
    @Override
    public final String getDefaultSessionGuid() {
        // reset an invalid session
        String guid = mDefaultGuid;
        if (!isValidSession(guid)) {
            resetDefaultSession(guid);
        }

        // return the default session
        return mDefaultGuid;
    }

    private void initDefaultSession() {
        for (final String guid : getSessions()) {
            try {
                setDefaultSession(guid);
                return;
            } catch (final TheKeyInvalidSessionException ignored) {
            }
        }
    }

    final void resetDefaultSession(@Nullable final String guid) {
        if (TextUtils.equals(mDefaultGuid, guid)) {
            // remove persisted default guid
            mDefaultGuid = null;
            getPrefs().edit()
                    .remove(PREF_DEFAULT_GUID)
                    .apply();

            // reinitialize the default guid
            initDefaultSession();
        }
    }

    @NonNull
    @Override
    public final Attributes getAttributes() {
        return getAttributes(getDefaultSessionGuid());
    }

    @Override
    public final boolean loadAttributes() throws TheKeySocketException {
        return loadAttributes(getDefaultSessionGuid());
    }

    @Nullable
    @Override
    @WorkerThread
    public final String getTicket(@NonNull final String service) throws TheKeySocketException {
        final String guid = getDefaultSessionGuid();
        return guid != null ? getTicket(guid, service) : null;
    }

    @Override
    @AnyThread
    public final void logout() {
        final String guid = getDefaultSessionGuid();
        if (guid != null) {
            logout(guid);
        }
    }

    @AnyThread
    final Uri getCasUri(final String... segments) {
        final Builder uri = mServer.buildUpon();
        for (final String segment : segments) {
            uri.appendPath(segment);
        }
        return uri.build();
    }

    @NonNull
    Uri getRedirectUri() {
        return getCasUri("oauth", "client", "public");
    }

    /**
     * @hide
     */
    public final Uri getAuthorizeUri() {
        return this.getAuthorizeUri(null);
    }

    private Uri getAuthorizeUri(final String state) {
        // build oauth authorize url
        final Builder uri = this.getCasUri("login").buildUpon()
                .appendQueryParameter(OAUTH_PARAM_RESPONSE_TYPE, OAUTH_RESPONSE_TYPE_CODE)
                .appendQueryParameter(OAUTH_PARAM_CLIENT_ID, Long.toString(mClientId))
                .appendQueryParameter(OAUTH_PARAM_REDIRECT_URI, getRedirectUri().toString());
        if (state != null) {
            uri.appendQueryParameter(OAUTH_PARAM_STATE, state);
        }

        return uri.build();
    }

    @Override
    @WorkerThread
    public final boolean loadAttributes(@Nullable final String guid) throws TheKeySocketException {
        if (guid == null) {
            return false;
        }

        String accessToken;
        while ((accessToken = getValidAccessToken(guid, 0)) != null) {
            // request the attributes from CAS
            HttpsURLConnection conn = null;
            try {
                // generate & send request
                final Uri attrsUri = this.getCasUri("api", "oauth", "attributes").buildUpon()
                        .appendQueryParameter(OAUTH_PARAM_ACCESS_TOKEN, accessToken).build();
                conn = (HttpsURLConnection) new URL(attrsUri.toString()).openConnection();

                if (conn.getResponseCode() == HTTP_OK) {
                    // parse the json response
                    final JSONObject json = this.parseJsonResponse(conn.getInputStream());

                    storeAttributes(guid, json);

                    // broadcast that we just loaded the attributes
                    mEventsManager.attributesUpdatedEvent(guid);

                    // return that attributes were loaded
                    return true;
                } else if (conn.getResponseCode() == HTTP_UNAUTHORIZED) {
                    // parse the Authenticate header
                    final String auth = conn.getHeaderField("WWW-Authenticate");
                    if (auth != null) {
                        final HttpHeaderUtils.Challenge challenge = HttpHeaderUtils.parseChallenge(auth);

                        // OAuth Bearer auth
                        if ("BEARER".equals(challenge.getScheme())) {
                            // extract the error encountered
                            final String error = challenge.getParameterValue("error");
                            if ("insufficient_scope".equals(error)) {
                                removeAttributes(guid);
                                return false;
                            } else if ("invalid_token".equals(error)) {
                                removeAccessToken(guid, accessToken);
                                continue;
                            }
                        }
                    }
                }
            } catch (final MalformedURLException e) {
                throw new RuntimeException("malformed CAS URL", e);
            } catch (final IOException e) {
                throw new TheKeySocketException("connect error", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            // the access token didn't work, remove it and restart processing
            removeAccessToken(guid, accessToken);
        }

        return false;
    }

    abstract void storeAttributes(@NonNull String guid, @NonNull JSONObject json);

    abstract void removeAttributes(@NonNull String guid);

    @Nullable
    @Override
    @WorkerThread
    public final String getTicket(@NonNull final String guid, @NonNull final String service)
            throws TheKeySocketException {
        String accessToken;
        while ((accessToken = getValidAccessToken(guid, 0)) != null) {
            // fetch a ticket
            final String ticket = getTicketWithAccessToken(accessToken, service);
            if (ticket != null) {
                return ticket;
            }

            // the access token didn't work, remove it and restart processing
            removeAccessToken(guid, accessToken);
        }

        // the user needs to authenticate before a ticket can be retrieved
        return null;
    }

    @Nullable
    @WorkerThread
    private String getTicketWithAccessToken(@NonNull final String accessToken, @NonNull final String service)
            throws TheKeySocketException {
        HttpsURLConnection conn = null;
        try {
            // generate & send request
            final Uri ticketUri = getCasUri("api", "oauth", "ticket").buildUpon()
                    .appendQueryParameter(OAUTH_PARAM_ACCESS_TOKEN, accessToken)
                    .appendQueryParameter(THEKEY_PARAM_SERVICE, service).build();
            conn = (HttpsURLConnection) new URL(ticketUri.toString()).openConnection();

            // parse the json response if we have a valid response
            if (conn.getResponseCode() == 200) {
                final JSONObject json = parseJsonResponse(conn.getInputStream());
                return json.optString(THEKEY_PARAM_TICKET, null);
            }
        } catch (final MalformedURLException e) {
            throw new RuntimeException("malformed CAS URL", e);
        } catch (final IOException e) {
            throw new TheKeySocketException("connect error", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }

    @Nullable
    abstract String getAccessToken(@NonNull String guid);

    @Nullable
    abstract String getRefreshToken(@NonNull String guid);

    @Nullable
    @WorkerThread
    private String getValidAccessToken(@NonNull final String guid, final int depth) throws TheKeySocketException {
        // prevent infinite recursion
        if (depth > 2) {
            return null;
        }

        synchronized (getLock(mLockAuth, guid)) {
            // check for an existing accessToken
            final String accessToken = getAccessToken(guid);
            if (accessToken != null) {
                return accessToken;
            }

            // try fetching a new access_token using a refresh_token
            final String refreshToken = getRefreshToken(guid);
            if (refreshToken != null) {
                if (processRefreshTokenGrant(guid, refreshToken)) {
                    return getValidAccessToken(guid, depth + 1);
                }

                // the refresh_token isn't valid anymore
                removeRefreshToken(guid, refreshToken);
            }

            // no valid access_token was found, clear auth state
            clearAuthState(guid, true);
        }

        return null;
    }

    @Override
    @AnyThread
    public final void logout(@NonNull final String guid) {
        // clearAuthState() may block on synchronization, so process the call on a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                clearAuthState(guid, true);
            }
        }).start();
    }

    abstract void removeAccessToken(@NonNull String guid, @NonNull String token);

    abstract void removeRefreshToken(@NonNull String guid, @NonNull String token);

    @WorkerThread
    abstract void clearAuthState(@NonNull String guid, boolean sendBroadcast);

    /**
     * @return The guid the code grant was successfully processed for, null if there was an error.
     */
    @WorkerThread
    final String processCodeGrant(final String code, final Uri redirectUri) throws TheKeySocketException {
        final Uri tokenUri = this.getCasUri("api", "oauth", "token");
        HttpsURLConnection conn = null;
        try {
            // generate & send request
            conn = (HttpsURLConnection) new URL(tokenUri.toString()).openConnection();
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            final byte[] data = (encodeParam(OAUTH_PARAM_GRANT_TYPE, OAUTH_GRANT_TYPE_AUTHORIZATION_CODE) + "&" +
                    encodeParam(OAUTH_PARAM_CLIENT_ID, Long.toString(mClientId)) + "&" +
                    encodeParam(OAUTH_PARAM_REDIRECT_URI, redirectUri.toString()) + "&" +
                    encodeParam(OAUTH_PARAM_CODE, code)).getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(data.length);
            conn.getOutputStream().write(data);

            if (conn.getResponseCode() == HTTP_OK) {
                // parse the json response
                final JSONObject json = this.parseJsonResponse(conn.getInputStream());
                final String guid = json.optString(OAUTH_PARAM_THEKEY_GUID, null);
                if (guid != null) {
                    if (storeGrants(guid, json)) {
                        return guid;
                    }
                }
            }
        } catch (final MalformedURLException e) {
            throw new RuntimeException("invalid CAS URL", e);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding??? this shouldn't happen", e);
        } catch (final IOException e) {
            throw new TheKeySocketException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }

    @WorkerThread
    private boolean processRefreshTokenGrant(@NonNull final String guid, @NonNull final String refreshToken)
            throws TheKeySocketException {
        final Uri tokenUri = this.getCasUri("api", "oauth", "token");
        HttpsURLConnection conn = null;
        try {
            // generate & send request
            conn = (HttpsURLConnection) new URL(tokenUri.toString()).openConnection();
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            final byte[] data = (encodeParam(OAUTH_PARAM_GRANT_TYPE, OAUTH_GRANT_TYPE_REFRESH_TOKEN) + "&" +
                    encodeParam(OAUTH_PARAM_CLIENT_ID, Long.toString(mClientId)) + "&" +
                    encodeParam(OAUTH_PARAM_REFRESH_TOKEN, refreshToken)).getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(data.length);
            conn.getOutputStream().write(data);

            // store the grants in the JSON response
            if (conn.getResponseCode() == HTTP_OK) {
                storeGrants(guid, parseJsonResponse(conn.getInputStream()));
            } else {
                return false;
            }

            // return success
            return true;
        } catch (final MalformedURLException e) {
            throw new RuntimeException("invalid CAS URL", e);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding??? this shouldn't happen", e);
        } catch (final IOException e) {
            throw new TheKeySocketException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    abstract boolean storeGrants(@NonNull String guid, @NonNull JSONObject json);

    private void migrateAccounts() {
        if (mMigrationSource != null) {
            for (final MigratingAccount account : mMigrationSource.getMigratingAccounts()) {
                if (!account.isValid() || createMigratingAccount(account)) {
                    mMigrationSource.removeMigratingAccount(account);
                }
            }

            mMigrationSource = null;
        }
    }

    @NonNull
    private Collection<MigratingAccount> getMigratingAccounts() {
        final List<MigratingAccount> accounts = new ArrayList<>();
        for (final String guid : getSessions()) {
            accounts.add(getMigratingAccount(guid));
        }
        return accounts;
    }

    @NonNull
    final MigratingAccount getMigratingAccount(@NonNull final String guid) {
        final MigratingAccount account = new MigratingAccount(guid);
        account.accessToken = getAccessToken(guid);
        account.refreshToken = getRefreshToken(guid);
        account.attributes = getAttributes(guid);
        return account;
    }

    private boolean removeMigratingAccount(@NonNull final MigratingAccount account) {
        removeAttributes(account.guid);
        clearAuthState(account.guid, false);
        return true;
    }

    abstract boolean createMigratingAccount(@NonNull MigratingAccount account);

    @SuppressWarnings("deprecation")
    private String encodeParam(final String name, final String value) {
        try {
            return URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            // we should never get here, but if for some odd reason we do, use
            // the default encoder which should be UTF-8
            return URLEncoder.encode(name) + "=" + URLEncoder.encode(value);
        }
    }

    @NonNull
    private JSONObject parseJsonResponse(final InputStream in) {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            final StringBuilder json = new StringBuilder();
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                json.append(buffer);
            }
            return new JSONObject(json.toString());
        } catch (final Exception e) {
            // return an empty object on error
            return new JSONObject();
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static <K> Object getLock(@NonNull final Map<K, Object> locks, @NonNull final K key) {
        synchronized (locks) {
            if (!locks.containsKey(key)) {
                locks.put(key, new Object());
            }
            return locks.get(key);
        }
    }

    static final class MigratingAccount {
        @NonNull
        final String guid;
        @Nullable
        String accessToken;
        @Nullable
        String refreshToken;
        @NonNull
        Attributes attributes;

        MigratingAccount(@NonNull final String guid) {
            this.guid = guid;
        }

        boolean isValid() {
            return accessToken != null || refreshToken != null;
        }
    }

    public static final class Configuration {
        @NonNull
        final Uri mServer;
        final long mClientId;
        @Nullable
        final String mAccountType;
        @Nullable
        final Configuration mMigrationSource;

        private Configuration(@Nullable final Uri server, final long id, @Nullable final String accountType,
                              @Nullable final Configuration migrationSource) {
            mServer = server != null ? server : CAS_SERVER;
            mClientId = id;
            mAccountType = accountType;
            mMigrationSource = migrationSource;
        }

        @NonNull
        public static Configuration base() {
            return new Configuration(null, INVALID_CLIENT_ID, null, null);
        }

        @NonNull
        public Configuration server(@Nullable final String server) {
            return new Configuration(server != null ? Uri.parse(server + (server.endsWith("/") ? "" : "/")) : null,
                                     mClientId, mAccountType, mMigrationSource);
        }

        @NonNull
        public Configuration server(@Nullable final Uri server) {
            return new Configuration(server, mClientId, mAccountType, mMigrationSource);
        }

        @NonNull
        public Configuration clientId(final long id) {
            return new Configuration(mServer, id, mAccountType, mMigrationSource);
        }

        @NonNull
        public Configuration accountType(@Nullable final String type) {
            return new Configuration(mServer, mClientId, type, mMigrationSource);
        }

        @NonNull
        public Configuration migrationSource(@Nullable final Configuration source) {
            return new Configuration(mServer, mClientId, mAccountType, source);
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Configuration)) {
                return false;
            }
            final Configuration that = (Configuration) o;
            return mClientId == that.mClientId && mServer.equals(that.mServer) &&
                    TextUtils.equals(mAccountType, that.mAccountType);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[] {mServer, mClientId, mAccountType});
        }
    }
}
