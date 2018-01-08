package me.thekey.android.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.Uri.Builder;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import org.json.JSONException;
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

import me.thekey.android.Attributes;
import me.thekey.android.TheKey;
import me.thekey.android.TheKeyInvalidSessionException;
import me.thekey.android.TheKeySocketException;
import me.thekey.android.core.events.NoopEventsManager;
import me.thekey.android.events.EventsManager;
import me.thekey.android.exception.TheKeyApiError;
import timber.log.Timber;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static android.support.annotation.RestrictTo.Scope.SUBCLASSES;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static me.thekey.android.core.Constants.CAS_SERVER;
import static me.thekey.android.core.Constants.OAUTH_PARAM_ACCESS_TOKEN;
import static me.thekey.android.core.Constants.OAUTH_PARAM_CLIENT_ID;
import static me.thekey.android.core.Constants.OAUTH_PARAM_CODE;
import static me.thekey.android.core.Constants.OAUTH_PARAM_ERROR;
import static me.thekey.android.core.Constants.OAUTH_PARAM_REDIRECT_URI;
import static me.thekey.android.core.Constants.OAUTH_PARAM_RESPONSE_TYPE;
import static me.thekey.android.core.Constants.OAUTH_PARAM_STATE;
import static me.thekey.android.core.Constants.OAUTH_RESPONSE_TYPE_CODE;
import static me.thekey.android.core.PkceUtils.encodeS256Challenge;
import static me.thekey.android.core.PkceUtils.generateUrlSafeBase64String;
import static me.thekey.android.core.PkceUtils.generateVerifier;

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
    @SuppressLint("StaticFieldLeak")
    private static TheKeyImpl sInstance = null;

    private final Map<String, Object> mLockAuth = new HashMap<>();

    @NonNull
    @RestrictTo(SUBCLASSES)
    final Context mContext;
    @NonNull
    @RestrictTo(SUBCLASSES)
    final EventsManager mEventsManager;

    @NonNull
    @RestrictTo(SUBCLASSES)
    final Configuration mConfig;
    @Nullable
    private TheKeyImpl mMigrationSource;
    @NonNull
    private final Uri mServer;
    private final long mClientId;

    @NonNull
    private final Uri mDefaultRedirectUri;

    @Nullable
    private String mDefaultGuid;

    TheKeyImpl(@NonNull final Context context, @NonNull final Configuration config) {
        mContext = context;
        mConfig = config;
        mServer = mConfig.mServer;
        mClientId = mConfig.mClientId;
        if (mClientId == INVALID_CLIENT_ID) {
            throw new IllegalStateException("client_id is invalid or not provided");
        }
        mDefaultGuid = getPrefs().getString(PREF_DEFAULT_GUID, null);

        mDefaultRedirectUri = mConfig.mDefaultRedirectUri != null ? mConfig.mDefaultRedirectUri :
                getCasUri("oauth", "client", "public");

        mEventsManager = resolveEventsManager(mContext, mConfig);
        if (mConfig.mMigrationSource != null) {
            mMigrationSource = createInstance(mContext, mConfig.mMigrationSource);
        }
    }

    @NonNull
    private static TheKeyImpl createInstance(@NonNull final Context context, @NonNull final Configuration config) {
        final TheKeyImpl instance;
        if (TextUtils.isEmpty(config.mAccountType)) {
            instance = new PreferenceTheKeyImpl(context, config);
        } else {
            // dynamically look for AccountManager implementation
            try {
                instance = (TheKeyImpl) Class.forName("me.thekey.android.core.AccountManagerTheKeyImpl")
                        .getDeclaredConstructor(Context.class, Configuration.class)
                        .newInstance(context, config);
            } catch (final Exception e) {
                throw new RuntimeException("Unable to find AccountManagerTheKeyImpl, " +
                                                   "make sure thekey-accountmanager library is loaded", e);
            }
        }

        // trigger account migration for this instance
        instance.migrateAccounts();

        return instance;
    }

    @NonNull
    @SuppressLint("BinaryOperationInTimber")
    private static EventsManager resolveEventsManager(@NonNull final Context context,
                                                      @NonNull final Configuration config) {
        // use configured EventsManager
        EventsManager manager = config.mEventsManager;

        // try creating a default LocalBroadcastManagerEventsManager
        if (manager == null) {
            try {
                manager = (EventsManager) Class.forName("me.thekey.android.content.LocalBroadcastManagerEventsManager")
                        .getDeclaredConstructor(Context.class)
                        .newInstance(context);
            } catch (final Exception e) {
                Timber.e(e, "Unable to initialize default EventsManager, try initializing one manually");
            }
        }

        // create a NoopEventsManager if all else failed
        if (manager == null) {
            manager = new NoopEventsManager();
        }

        return manager;
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
    public static TheKeyImpl getInstance(@NonNull final Context context) {
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

    @RestrictTo(SUBCLASSES)
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
    @RestrictTo(LIBRARY_GROUP)
    public final Uri getCasUri(final String... segments) {
        final Builder uri = mServer.buildUpon();
        for (final String segment : segments) {
            uri.appendPath(segment);
        }
        return uri.build();
    }

    @NonNull
    @Override
    public Uri getDefaultRedirectUri() {
        return mDefaultRedirectUri;
    }

    @RestrictTo(LIBRARY_GROUP)
    public final Uri getAuthorizeUri() {
        return getAuthorizeUri(getDefaultRedirectUri(), null);
    }

    private Uri getAuthorizeUri(@NonNull final Uri redirectUri, @Nullable String state) {
        if (state == null) {
            state = generateUrlSafeBase64String(16);
        }
        final String challenge = encodeS256Challenge(generateAndStoreCodeVerifier(state));

        // build oauth authorize url
        final Builder uri = getCasUri("login").buildUpon()
                .appendQueryParameter(OAUTH_PARAM_RESPONSE_TYPE, OAUTH_RESPONSE_TYPE_CODE)
                .appendQueryParameter(OAUTH_PARAM_CLIENT_ID, Long.toString(mClientId))
                .appendQueryParameter(OAUTH_PARAM_REDIRECT_URI, redirectUri.toString())
                .appendQueryParameter(OAUTH_PARAM_STATE, state)
                .appendQueryParameter(PARAM_CODE_CHALLENGE_METHOD, CODE_CHALLENGE_METHOD_S256)
                .appendQueryParameter(PARAM_CODE_CHALLENGE, challenge);

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
                    final JSONObject json = parseJsonResponse(conn.getInputStream());

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
                            final String error = challenge.getParameterValue(OAUTH_PARAM_ERROR);
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

    @RestrictTo(SUBCLASSES)
    abstract void storeAttributes(@NonNull String guid, @NonNull JSONObject json);

    @RestrictTo(SUBCLASSES)
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
                    .appendQueryParameter(PARAM_SERVICE, service).build();
            conn = (HttpsURLConnection) new URL(ticketUri.toString()).openConnection();

            // parse the json response if we have a valid response
            if (conn.getResponseCode() == HTTP_OK) {
                final JSONObject json = parseJsonResponse(conn.getInputStream());
                return json.optString(JSON_TICKET, null);
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
    @RestrictTo(SUBCLASSES)
    abstract String getAccessToken(@NonNull String guid);

    @Nullable
    @RestrictTo(SUBCLASSES)
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
                try {
                    if (processRefreshTokenGrant(guid, refreshToken)) {
                        return getValidAccessToken(guid, depth + 1);
                    }
                } catch (final TheKeyApiError ignored) {
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

    @RestrictTo(SUBCLASSES)
    abstract void removeAccessToken(@NonNull String guid, @NonNull String token);

    @RestrictTo(SUBCLASSES)
    abstract void removeRefreshToken(@NonNull String guid, @NonNull String token);

    @WorkerThread
    @RestrictTo(SUBCLASSES)
    abstract void clearAuthState(@NonNull String guid, boolean sendBroadcast);

    @Override
    @WorkerThread
    public final String processCodeGrant(@NonNull final String code, @NonNull final Uri redirectUri,
                                         @Nullable final String state)
            throws TheKeyApiError, TheKeySocketException {
        // build the request params
        final Map<String, String> params = new HashMap<>();
        params.put(PARAM_GRANT_TYPE, GRANT_TYPE_AUTHORIZATION_CODE);
        params.put(OAUTH_PARAM_CLIENT_ID, Long.toString(mClientId));
        params.put(OAUTH_PARAM_REDIRECT_URI, redirectUri.toString());
        params.put(OAUTH_PARAM_CODE, code);
        final String verifier = lookupCodeVerifier(state);
        if (verifier != null) {
            params.put(PARAM_CODE_VERIFIER, verifier);
        }

        // perform the token api request and process the response
        final JSONObject resp = sendTokenApiRequest(params);
        if (resp != null) {
            final String guid = resp.optString(JSON_THEKEY_GUID, null);
            if (guid != null) {
                if (storeGrants(guid, resp)) {
                    // clear any dangling code verifiers
                    clearCodeVerifiers();

                    // return the guid this grant was for
                    return guid;
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    @WorkerThread
    public final String processPasswordGrant(@NonNull final String username, @NonNull final String password)
            throws TheKeyApiError, TheKeySocketException  {
        // build the request params
        final Map<String, String> params = new HashMap<>();
        params.put(PARAM_GRANT_TYPE, GRANT_TYPE_PASSWORD);
        params.put(OAUTH_PARAM_CLIENT_ID, Long.toString(mClientId));
        params.put(PARAM_USERNAME, username);
        params.put(PARAM_PASSWORD, password);

        // perform the token api request and process the response
        final JSONObject resp = sendTokenApiRequest(params);
        if (resp != null) {
            final String guid = resp.optString(JSON_THEKEY_GUID, null);
            if (guid != null) {
                if (storeGrants(guid, resp)) {
                    // return the guid this grant was for
                    return guid;
                }
            }
        }

        return null;
    }

    @WorkerThread
    private boolean processRefreshTokenGrant(@NonNull final String guid, @NonNull final String refreshToken)
            throws TheKeyApiError, TheKeySocketException {
        // build the request params
        final Map<String, String> params = new HashMap<>();
        params.put(PARAM_GRANT_TYPE, GRANT_TYPE_REFRESH_TOKEN);
        params.put(OAUTH_PARAM_CLIENT_ID, Long.toString(mClientId));
        params.put(PARAM_REFRESH_TOKEN, refreshToken);

        // perform the token api request and process the response
        final JSONObject resp = sendTokenApiRequest(params);
        if (resp != null) {
            return storeGrants(guid, resp);
        }
        return false;
    }

    @Nullable
    private JSONObject sendTokenApiRequest(@NonNull final Map<String, String> params)
            throws TheKeyApiError, TheKeySocketException {
        final Uri tokenUri = getCasUri("api", "oauth", "token");
        HttpsURLConnection conn = null;
        try {
            // convert params into request data
            final Uri.Builder dataBuilder = new Uri.Builder();
            for (final Map.Entry<String, String> entry : params.entrySet()) {
                dataBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
            }
            final byte[] data = dataBuilder.build().getQuery().getBytes("UTF-8");

            // connect & send request
            conn = (HttpsURLConnection) new URL(tokenUri.toString()).openConnection();
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setFixedLengthStreamingMode(data.length);
            conn.getOutputStream().write(data);

            // if it's a successful request, return the parsed JSON
            if (conn.getResponseCode() == HTTP_OK) {
                return parseJsonResponse(conn.getInputStream());
            } else if (conn.getResponseCode() == HTTP_BAD_REQUEST) {
                throw TheKeyApiError.parse(conn.getResponseCode(), parseJsonResponse(conn.getErrorStream()));
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

    @RestrictTo(SUBCLASSES)
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
    @RestrictTo(SUBCLASSES)
    final MigratingAccount getMigratingAccount(@NonNull final String guid) {
        final MigratingAccount account = new MigratingAccount(guid);
        account.accessToken = getAccessToken(guid);
        account.refreshToken = getRefreshToken(guid);
        account.attributes = getAttributes(guid);
        return account;
    }

    private void removeMigratingAccount(@NonNull final MigratingAccount account) {
        removeAttributes(account.guid);
        clearAuthState(account.guid, false);
    }

    @RestrictTo(SUBCLASSES)
    abstract boolean createMigratingAccount(@NonNull MigratingAccount account);

    @NonNull
    private String codeVerifierKey(@NonNull final String state) {
        return "code_verifier-" + state;
    }

    @NonNull
    private String generateAndStoreCodeVerifier(@NonNull final String state) {
        final String verifier = generateVerifier();
        getPrefs().edit().putString(codeVerifierKey(state), verifier).apply();
        return verifier;
    }

    @Nullable
    private String lookupCodeVerifier(@Nullable final String state) {
        if (state == null) {
            return null;
        }

        final String key = codeVerifierKey(state);
        final String verifier = getPrefs().getString(key, null);
        getPrefs().edit().remove(key).apply();
        return verifier;
    }

    private void clearCodeVerifiers() {
        final SharedPreferences prefs = getPrefs();
        final SharedPreferences.Editor editor = prefs.edit();
        for (final String key : prefs.getAll().keySet()) {
            if (key.startsWith("code_verifier-")) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

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
    private JSONObject parseJsonResponse(@Nullable final InputStream response) throws IOException {
        if (response == null) {
            throw new IOException("no data in response from The Key");
        }

        // read response into string builder
        final BufferedReader reader = new BufferedReader(new InputStreamReader(response, "UTF-8"));
        final StringBuilder json = new StringBuilder();
        String buffer;
        while ((buffer = reader.readLine()) != null) {
            json.append(buffer);
        }

        // parse response as JSON
        try {
            return new JSONObject(json.toString());
        } catch (final JSONException e) {
            // throw an IOException on error
            throw new IOException("Invalid JSON response from The Key", e);
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

    @RestrictTo(LIBRARY_GROUP)
    static final class MigratingAccount {
        @NonNull
        @RestrictTo(SUBCLASSES)
        final String guid;
        @Nullable
        @RestrictTo(SUBCLASSES)
        String accessToken;
        @Nullable
        @RestrictTo(SUBCLASSES)
        String refreshToken;
        @NonNull
        @RestrictTo(SUBCLASSES)
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
        @RestrictTo(SUBCLASSES)
        final String mAccountType;

        @Nullable
        @RestrictTo(SUBCLASSES)
        final Uri mDefaultRedirectUri;

        @Nullable
        final EventsManager mEventsManager;

        @Nullable
        final Configuration mMigrationSource;

        private Configuration(@Nullable final Uri server, final long id, @Nullable final String accountType,
                              @Nullable final Uri redirectUri, @Nullable final EventsManager eventsManager,
                              @Nullable final Configuration migrationSource) {
            mServer = server != null ? server : CAS_SERVER;
            mClientId = id;
            mAccountType = accountType;
            mDefaultRedirectUri = redirectUri;
            mEventsManager = eventsManager;
            mMigrationSource = migrationSource;
        }

        @NonNull
        public static Configuration base() {
            return new Configuration(null, INVALID_CLIENT_ID, null, null, null, null);
        }

        @NonNull
        public Configuration server(@Nullable final String uri) {
            return server(uri != null ? Uri.parse(uri + (uri.endsWith("/") ? "" : "/")) : null);
        }

        @NonNull
        public Configuration server(@Nullable final Uri uri) {
            return new Configuration(uri, mClientId, mAccountType, mDefaultRedirectUri, mEventsManager,
                                     mMigrationSource);
        }

        @NonNull
        public Configuration accountType(@Nullable final String type) {
            return new Configuration(mServer, mClientId, type, mDefaultRedirectUri, mEventsManager, mMigrationSource);
        }

        @NonNull
        public Configuration clientId(final long id) {
            return new Configuration(mServer, id, mAccountType, mDefaultRedirectUri, mEventsManager, mMigrationSource);
        }

        @NonNull
        public Configuration redirectUri(@Nullable final String uri) {
            return redirectUri(uri != null ? Uri.parse(uri) : null);
        }

        @NonNull
        public Configuration redirectUri(@Nullable final Uri uri) {
            return new Configuration(mServer, mClientId, mAccountType, uri, mEventsManager, mMigrationSource);
        }

        @NonNull
        public Configuration eventsManager(@Nullable final EventsManager manager) {
            return new Configuration(mServer, mClientId, mAccountType, mDefaultRedirectUri, manager, mMigrationSource);
        }

        @NonNull
        public Configuration migrationSource(@Nullable final Configuration source) {
            return new Configuration(mServer, mClientId, mAccountType, mDefaultRedirectUri, mEventsManager, source);
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
            return mServer.equals(that.mServer) &&
                    mClientId == that.mClientId &&
                    TextUtils.equals(mAccountType, that.mAccountType) &&
                    (mDefaultRedirectUri != null ? mDefaultRedirectUri.equals(that.mDefaultRedirectUri) :
                            that.mDefaultRedirectUri == null) &&
                    (mEventsManager != null ? mEventsManager.equals(that.mEventsManager) :
                            that.mEventsManager == null) &&
                    (mMigrationSource != null ? mMigrationSource.equals(that.mMigrationSource) :
                            that.mMigrationSource == null);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[] {mServer, mClientId, mAccountType});
        }
    }
}
