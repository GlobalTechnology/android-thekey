package me.thekey.android.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.AsyncTask;
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

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import kotlin.collections.ArraysKt;
import me.thekey.android.Attributes;
import me.thekey.android.LoginUriBuilder;
import me.thekey.android.TheKey;
import me.thekey.android.TheKeyService;
import me.thekey.android.core.events.CompoundEventsManager;
import me.thekey.android.events.EventsManager;
import me.thekey.android.exception.RateLimitExceededApiError;
import me.thekey.android.exception.TheKeyApiError;
import me.thekey.android.exception.TheKeyInvalidSessionException;
import me.thekey.android.exception.TheKeySocketException;
import timber.log.Timber;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.SUBCLASSES;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static me.thekey.android.core.Constants.CAS_SERVER;
import static me.thekey.android.core.Constants.DEFAULT_TRAFFIC_STATS_TAG;
import static me.thekey.android.core.Constants.OAUTH_PARAM_ACCESS_TOKEN;
import static me.thekey.android.core.Constants.OAUTH_PARAM_CLIENT_ID;
import static me.thekey.android.core.Constants.OAUTH_PARAM_ERROR;
import static me.thekey.android.core.Constants.OAUTH_PARAM_REDIRECT_URI;
import static me.thekey.android.core.Constants.OAUTH_PARAM_RESPONSE_TYPE;
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

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int LIMIT_LOAD_ATTRIBUTES = 3;

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
    @RestrictTo(LIBRARY_GROUP)
    final CompoundEventsManager mEventsManager = new CompoundEventsManager();

    @NonNull
    @RestrictTo(SUBCLASSES)
    final Configuration mConfig;
    @Nullable
    private TheKeyImpl mMigrationSource;
    @NonNull
    private final Uri mServer;
    final long mClientId;

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

        initDefaultServices();
        for (final TheKeyService service : config.mServices) {
            initService(service);
        }
        if (mConfig.mEventsManager != null) {
            initService(mConfig.mEventsManager);
        }

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

    // region TheKeyServices
    private final Map<String, TheKeyService> mRegisteredServices = new HashMap<>();

    private void initDefaultServices() {
        // create the LocalBroadcastManagerEventsManager if it's on the classpath
        try {
            initService((TheKeyService) Class
                    .forName("me.thekey.android.localbroadcast.LocalBroadcastManagerEventsManager")
                    .getDeclaredConstructor(Context.class)
                    .newInstance(mContext));
        } catch (final Exception e) {
            Timber.tag("TheKey").d(e, "Unable to initialize LocalBroadcastManagerEventsManager");
        }

        // attach the LiveDataRegistry if it's on the classpath
        try {
            initService((TheKeyService) Class.forName("me.thekey.android.livedata.LiveDataRegistry")
                    .getDeclaredField("INSTANCE")
                    .get(null));
        } catch (final Exception e) {
            Timber.tag("TheKey").d(e, "Unable to initialize LiveDataRegistry");
        }
    }

    private void initService(@NonNull final TheKeyService service) {
        service.init(this);
        if (service instanceof EventsManager) {
            mEventsManager.addEventsManager((EventsManager) service);
        }
    }

    @Override
    @RestrictTo(LIBRARY_GROUP)
    public void registerService(@NonNull final TheKeyService service, @NonNull final String key) {
        mRegisteredServices.put(key, service);
    }

    @Nullable
    @Override
    @RestrictTo(LIBRARY_GROUP)
    @SuppressWarnings("unchecked")
    public TheKeyService getService(@NonNull final String key) {
        return mRegisteredServices.get(key);
    }
    // endregion TheKeyServices

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

    @NonNull
    @Override
    public LoginUriBuilder loginUriBuilder() {
        return new LoginUriBuilderImpl();
    }

    @Override
    @WorkerThread
    public final boolean loadAttributes(@Nullable final String guid) throws TheKeySocketException {
        if (guid == null) {
            return false;
        }

        int attempts = 0;
        String accessToken;
        while ((accessToken = getValidAccessToken(guid, 0)) != null) {
            // limit number of retries for loading attributes
            if (++attempts > LIMIT_LOAD_ATTRIBUTES) {
                return false;
            }

            final int currTrafficTag = TrafficStats.getThreadStatsTag();
            TrafficStats.setThreadStatsTag(mConfig.mTrafficTag);

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
                } else if (conn.getResponseCode() == HTTP_TOO_MANY_REQUESTS) {
                    if (attempts < LIMIT_LOAD_ATTRIBUTES) {
                        // Rate Limiting Header
                        final String retryAfter = conn.getHeaderField("Retry-After");
                        if (retryAfter != null) {
                            try {
                                //noinspection BusyWait
                                Thread.sleep(Long.parseLong(retryAfter) * 1000);
                                continue;
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }

                    return false;
                }
            } catch (final MalformedURLException e) {
                throw new RuntimeException("malformed CAS URL", e);
            } catch (final IOException e) {
                throw new TheKeySocketException("connect error", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                TrafficStats.setThreadStatsTag(currTrafficTag);
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
        final int currTrafficTag = TrafficStats.getThreadStatsTag();
        TrafficStats.setThreadStatsTag(mConfig.mTrafficTag);

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
            throw new IllegalStateException("malformed CAS URL", e);
        } catch (final IOException e) {
            throw new TheKeySocketException("connect error", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }

            TrafficStats.setThreadStatsTag(currTrafficTag);
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
                } catch (final RateLimitExceededApiError e) {
                    try {
                        Thread.sleep(e.getRetryAfter() * 1000);
                        return getValidAccessToken(guid, depth + 1);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return null;
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
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> clearAuthState(guid, true));
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
        params.put(PARAM_CODE, code);
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

        final int currTrafficTag = TrafficStats.getThreadStatsTag();
        TrafficStats.setThreadStatsTag(mConfig.mTrafficTag);

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
            switch (conn.getResponseCode()) {
                case HTTP_OK:
                    return parseJsonResponse(conn.getInputStream());
                case HTTP_BAD_REQUEST:
                case HTTP_TOO_MANY_REQUESTS:
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

            TrafficStats.setThreadStatsTag(currTrafficTag);
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
        account.attributes = getCachedAttributes(guid);
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
    String generateAndStoreCodeVerifier(@NonNull final String state) {
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
        @RestrictTo(LIBRARY_GROUP)
        final String mAccountType;

        @Nullable
        @RestrictTo(LIBRARY_GROUP)
        final Uri mDefaultRedirectUri;

        final int mTrafficTag;

        @NonNull
        final TheKeyService[] mServices;

        /**
         * @deprecated Since v4.0.0
         */
        @Nullable
        @Deprecated
        final EventsManager mEventsManager;

        @Nullable
        final Configuration mMigrationSource;

        @SuppressWarnings("checkstyle:ParameterNumber")
        private Configuration(@Nullable final Uri server, final long id, @Nullable final String accountType,
                              @Nullable final Uri redirectUri, final int trafficTag,
                              @Nullable final EventsManager eventsManager,
                              @Nullable final TheKeyService[] services,
                              @Nullable final Configuration migrationSource) {
            mServer = server != null ? server : CAS_SERVER;
            mClientId = id;
            mAccountType = accountType;
            mDefaultRedirectUri = redirectUri;
            mTrafficTag = trafficTag;
            mServices = services != null ? services : new TheKeyService[0];
            mEventsManager = eventsManager;
            mMigrationSource = migrationSource;
        }

        @NonNull
        public static Configuration base() {
            return new Configuration(null, INVALID_CLIENT_ID, null, null, DEFAULT_TRAFFIC_STATS_TAG, null, null, null);
        }

        @NonNull
        public Configuration server(@Nullable final String uri) {
            return server(uri != null ? Uri.parse(uri + (uri.endsWith("/") ? "" : "/")) : null);
        }

        @NonNull
        public Configuration server(@Nullable final Uri uri) {
            return new Configuration(uri, mClientId, mAccountType, mDefaultRedirectUri, mTrafficTag, mEventsManager,
                                     mServices, mMigrationSource);
        }

        @NonNull
        public Configuration accountType(@Nullable final String type) {
            return new Configuration(mServer, mClientId, type, mDefaultRedirectUri, mTrafficTag, mEventsManager,
                                     mServices, mMigrationSource);
        }

        @NonNull
        public Configuration clientId(final long id) {
            return new Configuration(mServer, id, mAccountType, mDefaultRedirectUri, mTrafficTag, mEventsManager,
                                     mServices, mMigrationSource);
        }

        @NonNull
        public Configuration redirectUri(@Nullable final String uri) {
            return redirectUri(uri != null ? Uri.parse(uri) : null);
        }

        @NonNull
        public Configuration redirectUri(@Nullable final Uri uri) {
            return new Configuration(mServer, mClientId, mAccountType, uri, mTrafficTag, mEventsManager,
                                     mServices, mMigrationSource);
        }

        /**
         * @deprecated Since v4.0.0, use {@link Configuration#service(TheKeyService)} to register the events manager.
         */
        @NonNull
        @Deprecated
        public Configuration eventsManager(@Nullable final EventsManager manager) {
            return new Configuration(mServer, mClientId, mAccountType, mDefaultRedirectUri, mTrafficTag, manager,
                                     mServices, mMigrationSource);
        }

        @NonNull
        public Configuration service(@NonNull final TheKeyService service) {
            return new Configuration(mServer, mClientId, mAccountType, mDefaultRedirectUri, mTrafficTag, mEventsManager,
                                     ArraysKt.plus(mServices, service), mMigrationSource);
        }

        @NonNull
        public Configuration trafficStatsTag(final int tag) {
            return new Configuration(mServer, mClientId, mAccountType, mDefaultRedirectUri, tag, mEventsManager,
                                     mServices, mMigrationSource);
        }

        @NonNull
        public Configuration migrationSource(@Nullable final Configuration source) {
            return new Configuration(mServer, mClientId, mAccountType, mDefaultRedirectUri, mTrafficTag, mEventsManager,
                                     mServices, source);
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
                    mTrafficTag == that.mTrafficTag &&
                    Arrays.equals(mServices, that.mServices) &&
                    (mEventsManager != null ? mEventsManager.equals(that.mEventsManager) :
                            that.mEventsManager == null) &&
                    (mMigrationSource != null ? mMigrationSource.equals(that.mMigrationSource) :
                            that.mMigrationSource == null);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(
                    new Object[] {mServer, mClientId, mAccountType, mDefaultRedirectUri, mTrafficTag, mEventsManager,
                            mMigrationSource});
        }
    }

    final class LoginUriBuilderImpl extends LoginUriBuilder {
        @NonNull
        @Override
        public Uri build() {
            final Uri redirectUri = mRedirectUri != null ? mRedirectUri : getDefaultRedirectUri();
            final String state = mState != null ? mState : generateUrlSafeBase64String(16);
            final String challenge = encodeS256Challenge(generateAndStoreCodeVerifier(state));

            // build oauth login url
            final Builder uri = getCasUri("login").buildUpon()
                    .appendQueryParameter(OAUTH_PARAM_RESPONSE_TYPE, OAUTH_RESPONSE_TYPE_CODE)
                    .appendQueryParameter(OAUTH_PARAM_CLIENT_ID, Long.toString(mClientId))
                    .appendQueryParameter(OAUTH_PARAM_REDIRECT_URI, redirectUri.toString())
                    .appendQueryParameter(PARAM_STATE, state)
                    .appendQueryParameter(PARAM_CODE_CHALLENGE_METHOD, CODE_CHALLENGE_METHOD_S256)
                    .appendQueryParameter(PARAM_CODE_CHALLENGE, challenge);

            // attach the scope if it is specified
            if (!mScope.isEmpty()) {
                uri.appendQueryParameter(PARAM_SCOPE, TextUtils.join(" ", mScope));
            }

            // deeplink to the signup form if requested
            if (mSignup) {
                uri.appendQueryParameter(PARAM_ACTION, ACTION_SIGNUP);
            }

            return uri.build();
        }
    }
}
